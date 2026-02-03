package io.onedev.commons.utils;

import static java.nio.file.Files.createSymbolicLink;
import static org.apache.commons.compress.archivers.tar.TarConstants.LF_DIR;
import static org.apache.commons.compress.archivers.tar.TarConstants.LF_SYMLINK;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.jspecify.annotations.Nullable;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

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
                        var entryName = basePath.relativize(filePath).toString().replace(File.separatorChar, '/');
                        if (Files.isSymbolicLink(filePath)) {
                            TarArchiveEntry entry = new TarArchiveEntry(entryName, LF_SYMLINK);
                            entry.setLinkName(Files.readSymbolicLink(filePath).toString());
                            tos.putArchiveEntry(entry);
                            tos.closeArchiveEntry();
                        } else {
                            addTarEntry(filePath.toFile(), entryName, null, tos, buffer);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dirPath, BasicFileAttributes attrs) throws IOException {
                        var entryName = basePath.relativize(dirPath).toString().replace(File.separatorChar, '/');
                        if (entryName.length() != 0)
                            addTarEntry(dirPath.toFile(), entryName, null, tos, buffer);
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

    private static void addTarEntry(File file, String entryName, @Nullable Collection<File> executableFiles,
                                    TarArchiveOutputStream tos, byte[] buffer) throws IOException {
        if (file.isDirectory()) {
            TarArchiveEntry entry = new TarArchiveEntry(entryName + "/", LF_DIR);
            tos.putArchiveEntry(entry);
            tos.closeArchiveEntry();
        } else {
            TarArchiveEntry entry = new TarArchiveEntry(entryName);
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
                    var entryName = path.replace(File.separatorChar, '/');
                    addTarEntry(new File(baseDir, path), entryName, executableFiles, tos, buffer);
                }
            }
            tos.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public static void untar(InputStream is, File destDir, boolean compressed) {
        var destPath = destDir.toPath();
        byte[] buffer = new byte[BUFFER_SIZE];
        TarArchiveInputStream tis;
        try {
            if (compressed)
                tis = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(is, BUFFER_SIZE), BUFFER_SIZE));
            else
                tis = new TarArchiveInputStream(new BufferedInputStream(is, BUFFER_SIZE));
            TarArchiveEntry entry;
            while((entry = tis.getNextTarEntry()) != null) {
                var entryName = entry.getName();
                if (Splitter.on('/').trimResults().splitToStream(entryName).anyMatch(it->it.equals("..")))
                    throw new ExplicitException("Tar entry should not contain path segment '..': " + entryName);
                if (entry.isSymbolicLink()) {
                    var filePath = destPath.resolve(entryName);
                    if (Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
                        var file = filePath.toFile();
                        if (file.isDirectory())
                            FileUtils.deleteDir(file);
                        else
                            FileUtils.deleteFile(file);
                    }
                    createSymbolicLink(filePath, Paths.get(entry.getLinkName()));
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
