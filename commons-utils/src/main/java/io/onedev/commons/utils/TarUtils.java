package io.onedev.commons.utils;

import com.google.common.collect.Sets;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.file.Files.createSymbolicLink;
import static org.apache.commons.compress.archivers.tar.TarConstants.LF_DIR;
import static org.apache.commons.compress.archivers.tar.TarConstants.LF_SYMLINK;

public class TarUtils {

    private static final int BUFFER_SIZE = 64*1024;

    public static void tar(File baseDir, OutputStream os, boolean compress) {
        byte[] buffer = new byte[BUFFER_SIZE];

        try (var tos = newTarArchiveOutputStream(os, compress)) {
            if (baseDir.exists() && baseDir.isDirectory()) {
                var basePath = baseDir.toPath();
                Files.walkFileTree(baseDir.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        var relativePathName = basePath.relativize(filePath).toString().replace('\\', '/');
                        if (Files.isSymbolicLink(filePath)) {
                            TarArchiveEntry entry = new TarArchiveEntry(relativePathName, LF_SYMLINK);
                            var linkTargetPath = filePath.toRealPath();
                            var linkTargetRelativePathName = filePath.getParent().relativize(linkTargetPath).toString().replace('\\', '/');
                            if (basePath.relativize(linkTargetPath).toString().contains("..")) {
                                var errorMessage = String.format("Symlink target is outside of base directory (link: %s, target: %s)",
                                        relativePathName, linkTargetRelativePathName);
                                throw new ExplicitException(errorMessage);
                            }
                            entry.setLinkName(linkTargetRelativePathName);
                            tos.putArchiveEntry(entry);
                            tos.closeArchiveEntry();
                        } else {
                            addTarEntry(baseDir, relativePathName, null, tos, buffer);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dirPath, BasicFileAttributes attrs) throws IOException {
                        var relativePathName = basePath.relativize(dirPath).toString().replace('\\', '/');
                        if (relativePathName.length() != 0)
                            addTarEntry(baseDir, relativePathName, null, tos, buffer);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void tar(File baseDir, @Nullable Collection<String> includes,
OutputStream os, boolean compress) {
        tar(baseDir, includes, null, os, compress);
    }

    public static void tar(File baseDir, @Nullable Collection<String> includes,
@Nullable Collection<String> excludes, OutputStream os,
boolean compress) {
        tar(baseDir, includes, excludes, null, os, compress);
    }

    private static void addTarEntry(File baseDir, String pathName, @Nullable Collection<File> executableFiles,
                                    TarArchiveOutputStream tos, byte[] buffer) throws IOException {
        var file = new File(baseDir, pathName);
        if (file.isDirectory()) {
            TarArchiveEntry entry = new TarArchiveEntry(pathName + "/", LF_DIR);
            tos.putArchiveEntry(entry);
            tos.closeArchiveEntry();
        } else {
            TarArchiveEntry entry = new TarArchiveEntry(pathName);
            entry.setSize(file.length());
            if (executableFiles != null && executableFiles.stream().anyMatch(it->it.toPath().normalize().equals(file.toPath().normalize()))
                    || executableFiles == null && file.canExecute()) {
                entry.setMode(entry.getMode() | 0000100);
            }
            entry.setModTime(file.lastModified());
            tos.putArchiveEntry(entry);
            try (InputStream is = new FileInputStream(file)) {
                int count;
                while((count = is.read(buffer)) != -1)
                    tos.write(buffer, 0, count);
            }
            tos.closeArchiveEntry();
        }
    }

    private static TarArchiveOutputStream newTarArchiveOutputStream(OutputStream os, boolean compress) throws IOException {
        TarArchiveOutputStream tos;
        if (compress)
            tos = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(os, BUFFER_SIZE), BUFFER_SIZE));
        else
            tos = new TarArchiveOutputStream(new BufferedOutputStream(os, BUFFER_SIZE));
        tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        return tos;
    }

    public static void tar(File baseDir, @Nullable Collection<String> includes,
@Nullable Collection<String> excludes,
@Nullable Collection<String> executables,
OutputStream os, boolean compress) {
        if (includes == null)
            includes = Sets.newHashSet("**");
        if (excludes == null)
            excludes = new HashSet<>();

        byte[] buffer = new byte[BUFFER_SIZE];

        try (var tos = newTarArchiveOutputStream(os, compress)) {
            if (baseDir.exists() && baseDir.isDirectory()) {
                Collection<File> executableFiles = null;
                if (executables != null)
                    executableFiles = FileUtils.listFiles(baseDir, executables, new HashSet<>());
                for (String path: FileUtils.listPaths(baseDir, includes, excludes)) {
                    path = path.replace('\\', '/');
                    addTarEntry(baseDir, path, executableFiles, tos, buffer);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void untar(InputStream is, File destDir, boolean compressed) {
        var destPath = destDir.toPath();
        byte[] buffer = new byte[BUFFER_SIZE];
        List<Pair<String, String>> symlinks = new ArrayList<>();
        TarArchiveInputStream tis = null;
        try {
            if (compressed)
                tis = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(is, BUFFER_SIZE), BUFFER_SIZE));
            else
                tis = new TarArchiveInputStream(new BufferedInputStream(is, BUFFER_SIZE));
            TarArchiveEntry entry;
            while((entry = tis.getNextTarEntry()) != null) {
                var entryName = entry.getName();
                if (entryName.contains(".."))
                    throw new ExplicitException("Tar entry name contains '..': " + entryName);
                if (entry.isSymbolicLink()) {
                    symlinks.add(new ImmutablePair<>(entryName, entry.getLinkName()));
                } else if (entry.isFile()) {
                    File entryFile = new File(destDir, entryName);
                    File entryParentFile = entryFile.getParentFile();
                    FileUtils.createDir(entryParentFile);

                    if (entryFile.exists())
                        FileUtils.deleteFile(entryFile);

                    try (var bos = new BufferedOutputStream(new FileOutputStream(entryFile), BUFFER_SIZE)) {
                        int count;
                        while((count = tis.read(buffer)) != -1)
                            bos.write(buffer, 0, count);
                        if ((entry.getMode() & 0000100) != 0)
                            entryFile.setExecutable(true);
                    } finally {
                        entryFile.setLastModified(entry.getModTime().getTime());
                    }
                } else {
                    FileUtils.createDir(new File(destDir, entryName));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(tis);
        }
        for (var symlink: symlinks) {
            var linkPath = destPath.resolve(symlink.getLeft());
            if (destPath.relativize(linkPath.getParent().resolve(symlink.getRight())).toString().contains("..")) {
                var errorMessage = String.format("Symlink target is outside of destination directory (link: %s, target: %s)",
                        symlink.getLeft(), symlink.getRight());
                throw new ExplicitException(errorMessage);
            }
            try {
                createSymbolicLink(linkPath, Paths.get(symlink.getRight()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
