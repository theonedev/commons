package io.onedev.commons.utils;

import com.google.common.collect.Sets;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
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

        try {
            var tos = newTarArchiveOutputStream(os, compress);
            if (baseDir.exists() && baseDir.isDirectory()) {
                var basePath = baseDir.toPath();
                Files.walkFileTree(baseDir.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        var relativePathName = basePath.relativize(filePath).toString().replace('\\', '/');
                        if (Files.isSymbolicLink(filePath)) {
                            TarArchiveEntry entry = new TarArchiveEntry(relativePathName, LF_SYMLINK);
                            entry.setLinkName(Files.readSymbolicLink(filePath).toString());
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
            tos.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        if (compress) {
            var baos = new BufferedOutputStream(os, BUFFER_SIZE);
            var zos = new GZIPOutputStream(baos, BUFFER_SIZE);
            tos = new TarArchiveOutputStream(zos) {
                @Override
                public void finish() throws IOException {
                    super.finish();
                    zos.finish();
                    baos.flush();
                }
            };
        } else {
            var baos = new BufferedOutputStream(os, BUFFER_SIZE);
            tos = new TarArchiveOutputStream(baos) {
                public void finish() throws IOException {
                    super.finish();
                    baos.flush();
                }
            };
        }
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

        try {
            var tos = newTarArchiveOutputStream(os, compress);
            if (baseDir.exists() && baseDir.isDirectory()) {
                Collection<File> executableFiles = null;
                if (executables != null)
                    executableFiles = FileUtils.listFiles(baseDir, executables, new HashSet<>());
                for (String path: FileUtils.listPaths(baseDir, includes, excludes)) {
                    path = path.replace('\\', '/');
                    addTarEntry(baseDir, path, executableFiles, tos, buffer);
                }
            }
            tos.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void untar(InputStream is, File destDir, boolean compressed) {
        var destPath = destDir.toPath();
        byte[] buffer = new byte[BUFFER_SIZE];
        List<Pair<String, String>> symlinks = new ArrayList<>();
        TarArchiveInputStream tis;
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
                    createSymbolicLink(destPath.resolve(entryName), Paths.get(entry.getLinkName()));
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
        }
    }
}
