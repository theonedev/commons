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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jspecify.annotations.Nullable;

import com.google.common.collect.Sets;

import io.onedev.commons.utils.match.WildcardUtils;

public class TarUtils {

    private static final int BUFFER_SIZE = 64*1024;

    private static final String SINGLE_FILE_PAX_HEADER = "OneDev.singleFile";

    public static void tar(File dirOrFile, OutputStream os, boolean compress) {
        tar(dirOrFile, Collections.emptyList(), os, compress);
    }

    public static void tar(File dirOrFile, List<String> excludedPathPatterns, OutputStream os, boolean compress) {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            var tos = newTarArchiveOutputStream(os, compress);
            if (dirOrFile.isFile()) {
                if (Files.isSymbolicLink(dirOrFile.toPath()))
                    throw new ExplicitException("Symbolic link is not allowed: " + dirOrFile.getAbsolutePath());
                if (FileUtils.isUnixSocket(dirOrFile.toPath()))
                    throw new ExplicitException("Unix socket is not allowed: " + dirOrFile.getAbsolutePath());
        
                TarArchiveEntry entry = new TarArchiveEntry(dirOrFile.getName());
                entry.setSize(dirOrFile.length());
                if (dirOrFile.canExecute())
                    entry.setMode(entry.getMode() | 0000100);
                entry.setModTime(dirOrFile.lastModified());
                entry.addPaxHeader(SINGLE_FILE_PAX_HEADER, "true");
                tos.putArchiveEntry(entry);
                try (InputStream is = new FileInputStream(dirOrFile)) {
                    int count;
                    while ((count = is.read(buffer)) != -1)
                        tos.write(buffer, 0, count);
                }
                tos.closeArchiveEntry();    
            } else if (dirOrFile.isDirectory()) {
                var basePath = dirOrFile.toPath();
                Files.walkFileTree(dirOrFile.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        if (FileUtils.isUnixSocket(filePath))
                            return FileVisitResult.CONTINUE;
                        var entryName = basePath.relativize(filePath).toString().replace(File.separatorChar, '/');
                        if (isExcluded(entryName, excludedPathPatterns))
                            return FileVisitResult.CONTINUE;
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
                        if (entryName.length() != 0) {
                            if (isExcluded(entryName, excludedPathPatterns))
                                return FileVisitResult.SKIP_SUBTREE;
                            addTarEntry(dirPath.toFile(), entryName, null, tos, buffer);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            tos.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isExcluded(String entryName, List<String> excludedPathPatterns) {
        return excludedPathPatterns.stream().anyMatch(it -> WildcardUtils.matchPath(it, entryName));
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
        } else if (!FileUtils.isUnixSocket(file.toPath())) {
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
    public static void untar(InputStream is, File destDirOrFile, boolean compressed) {
        byte[] buffer = new byte[BUFFER_SIZE];
        TarArchiveInputStream tis;
        try {
            if (compressed)
                tis = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(is, BUFFER_SIZE), BUFFER_SIZE));
            else
                tis = new TarArchiveInputStream(new BufferedInputStream(is, BUFFER_SIZE));

            TarArchiveEntry firstEntry = tis.getNextTarEntry();
            if (firstEntry == null)
                return;

            if ("true".equals(firstEntry.getExtraPaxHeader(SINGLE_FILE_PAX_HEADER)))
                untarSingleFile(tis, firstEntry, destDirOrFile, buffer);
            else
                untarToDir(tis, firstEntry, destDirOrFile, buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    private static void untarSingleFile(TarArchiveInputStream tis, TarArchiveEntry entry,
                                        File destFile, byte[] buffer) throws IOException {
        if (destFile.exists() && destFile.isDirectory())
            throw new ExplicitException("Destination should not be a directory: " + destFile.getAbsolutePath());
        if (entry.isSymbolicLink())
            throw new ExplicitException("Symbolic link entry is not allowed: " + entry.getName());
        if (!entry.isFile())
            throw new ExplicitException("Expecting a file entry: " + entry.getName());

        File parentDir = destFile.getParentFile();
        if (parentDir != null)
            FileUtils.createDir(parentDir);

        try (var bos = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE)) {
            int count;
            while((count = tis.read(buffer)) != -1)
                bos.write(buffer, 0, count);
            if ((entry.getMode() & 0000100) != 0)
                destFile.setExecutable(true);
        } finally {
            destFile.setLastModified(entry.getModTime().getTime());
        }

        if (tis.getNextTarEntry() != null)
            throw new ExplicitException("Expecting a single file entry but got more");
    }

    private static Path resolveEntryPath(Path destPath, String entryName) {
        var entryPath = destPath.resolve(entryName).normalize();
        if (!entryPath.startsWith(destPath))
            throw new ExplicitException("Tar entry escape detected: " + entryName);
        return entryPath;
    }

    private static void ensureLinkTargetInside(Path destPath, Path linkTargetPath, String entryName) throws IOException {
        var relativePath = destPath.relativize(linkTargetPath);
        var currentPath = destPath;
        var linkFollowCount = 0;
        for (var pathSegment: relativePath) {
            currentPath = currentPath.resolve(pathSegment);
            while (Files.isSymbolicLink(currentPath)) {
                if (++linkFollowCount > 40)
                    throw new ExplicitException("Too many levels of symbolic links for tar entry "
                            + entryName + ": " + currentPath);
                var target = Files.readSymbolicLink(currentPath);
                var parent = currentPath.getParent();
                if (parent == null)
                    throw new ExplicitException("Tar entry symbol link resolves outside destination: "
                            + entryName + " -> " + currentPath);
                currentPath = parent.resolve(target).normalize();
                if (!currentPath.startsWith(destPath))
                    throw new ExplicitException("Tar entry symbol link resolves outside destination: "
                            + entryName + " -> " + currentPath);
            }
            if (!Files.exists(currentPath, LinkOption.NOFOLLOW_LINKS))
                return;
        }
    }

    private static void createDirInside(Path destPath, Path dirPath) throws IOException {
        if (!dirPath.startsWith(destPath))
            throw new ExplicitException("Tar entry parent escape detected: " + dirPath);

        var relativePath = destPath.relativize(dirPath);
        var currentPath = destPath;
        for (var pathSegment: relativePath) {
            currentPath = currentPath.resolve(pathSegment);
            if (Files.isSymbolicLink(currentPath))
                FileUtils.deleteFile(currentPath.toFile());
            if (Files.exists(currentPath, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(currentPath, LinkOption.NOFOLLOW_LINKS)) {
                    throw new ExplicitException("Tar entry can not create directory over file: "
                            + currentPath);
                }
            } else {
                Files.createDirectory(currentPath);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void untarToDir(TarArchiveInputStream tis, TarArchiveEntry firstEntry,
                                   File destDir, byte[] buffer) throws IOException {
        if (destDir.exists() && !destDir.isDirectory())
            throw new ExplicitException("Destination should be a directory: " + destDir.getAbsolutePath());
        
        FileUtils.createDir(destDir);
        var destPath = destDir.toPath().toRealPath().normalize();
        TarArchiveEntry entry = firstEntry;
        do {
            var entryName = entry.getName();
            var entryPath = resolveEntryPath(destPath, entryName);
            File entryFile = entryPath.toFile();
            if (entry.isSymbolicLink()) {
                var entryParentPath = entryPath.getParent();
                if (entryParentPath == null || !entryParentPath.startsWith(destPath))
                    throw new ExplicitException("Tar entry parent escape detected: " + entryName);
                var linkName = entry.getLinkName();
                Path linkTarget = Paths.get(linkName);
                var linkTargetPath = entryParentPath.resolve(linkTarget).normalize();
                if (!linkTargetPath.startsWith(destPath))
                    throw new ExplicitException("Tar entry symbol link escape detected: "
                            + entryName + " -> " + linkName);
                ensureLinkTargetInside(destPath, linkTargetPath, entryName);

                createDirInside(destPath, entryParentPath);
                if (Files.exists(entryPath, LinkOption.NOFOLLOW_LINKS)) {
                    if (Files.isDirectory(entryPath, LinkOption.NOFOLLOW_LINKS))
                        FileUtils.deleteDir(entryFile);
                    else
                        FileUtils.deleteFile(entryFile);
                }
                createSymbolicLink(entryPath, linkTarget);
            } else if (entry.isFile()) {
                var entryParentPath = entryPath.getParent();
                if (entryParentPath == null || !entryParentPath.startsWith(destPath))
                    throw new ExplicitException("Tar entry parent escape detected: " + entryName);
                createDirInside(destPath, entryParentPath);

                if (Files.exists(entryPath, LinkOption.NOFOLLOW_LINKS)) {
                    if (Files.isDirectory(entryPath, LinkOption.NOFOLLOW_LINKS))
                        FileUtils.deleteDir(entryFile);
                    else
                        FileUtils.deleteFile(entryFile);
                }

                try (var os = Files.newOutputStream(entryPath, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
                     var bos = new BufferedOutputStream(os, BUFFER_SIZE)) {
                    int count;
                    while((count = tis.read(buffer)) != -1)
                        bos.write(buffer, 0, count);
                    if ((entry.getMode() & 0000100) != 0)
                        entryFile.setExecutable(true);
                } finally {
                    entryFile.setLastModified(entry.getModTime().getTime());
                }
            } else {
                createDirInside(destPath, entryPath);
            }
        } while ((entry = tis.getNextTarEntry()) != null);
    }
}
