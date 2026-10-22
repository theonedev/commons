package io.onedev.commons.utils;

import static org.apache.commons.compress.archivers.tar.TarConstants.LF_SYMLINK;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs({OS.LINUX, OS.MAC})
class TarUtilsExternalSymlinkTest {

    @TempDir
    Path temp;

    @Test
    void preservesExternalDanglingAndCyclicLinksOnlyWhenEnabled() throws IOException {
        var source = Files.createDirectory(temp.resolve("source"));
        var outside = Files.writeString(temp.resolve("outside"), "do not copy");
        Files.writeString(source.resolve("file"), "cached");
        var links = Map.of("absolute", outside.toString(), "relative", "../outside",
                "dangling", "../missing", "cycle", "cycle", "internal", "file");
        for (var link : links.entrySet())
            Files.createSymbolicLink(source.resolve(link.getKey()), Path.of(link.getValue()));
        var archive = new ByteArrayOutputStream();
        TarUtils.tar(source.toFile(), archive, false);

        assertThrows(ExplicitException.class, () -> TarUtils.untar(
                new ByteArrayInputStream(archive.toByteArray()), temp.resolve("strict").toFile(), false));
        var restored = temp.resolve("restored");
        for (int attempt = 0; attempt < 2; attempt++) {
            TarUtils.untar(new ByteArrayInputStream(archive.toByteArray()), restored.toFile(), false, true);
            for (var link : links.entrySet())
                assertEquals(Path.of(link.getValue()), Files.readSymbolicLink(restored.resolve(link.getKey())));
            assertEquals("cached", Files.readString(restored.resolve("file")));
        }
        assertEquals("do not copy", Files.readString(outside));
    }

    @Test
    void neverWritesThroughArchivedLinksRegardlessOfEntryOrder() throws IOException {
        var outside = Files.createDirectory(temp.resolve("outside"));
        var sentinel = Files.writeString(outside.resolve("file"), "keep");
        for (boolean linkFirst : List.of(true, false)) {
            var archive = new ByteArrayOutputStream();
            try (var tar = new TarArchiveOutputStream(archive)) {
                if (linkFirst)
                    addLink(tar, "redirect", "../outside");
                addFile(tar, "redirect/file", "cached");
                if (!linkFirst)
                    addLink(tar, "redirect", "../outside");
            }
            var restored = temp.resolve("restored-" + linkFirst);
            TarUtils.untar(new ByteArrayInputStream(archive.toByteArray()), restored.toFile(), false, true);
            assertEquals("keep", Files.readString(sentinel));
            if (linkFirst) {
                assertFalse(Files.isSymbolicLink(restored.resolve("redirect")));
                assertEquals("cached", Files.readString(restored.resolve("redirect/file")));
            } else {
                assertEquals(Path.of("../outside"), Files.readSymbolicLink(restored.resolve("redirect")));
            }
        }
    }

    @Test
    void replacesExistingParentAndLeafLinksWithoutModifyingTheirTargets() throws IOException {
        var outside = Files.createDirectory(temp.resolve("outside"));
        var sentinel = Files.writeString(outside.resolve("file"), "keep");
        var restored = Files.createDirectory(temp.resolve("restored"));
        Files.createSymbolicLink(restored.resolve("parent"), outside);
        Files.createSymbolicLink(restored.resolve("leaf"), sentinel);
        var archive = new ByteArrayOutputStream();
        try (var tar = new TarArchiveOutputStream(archive)) {
            addFile(tar, "parent/file", "cached parent");
            addFile(tar, "leaf", "cached leaf");
        }
        TarUtils.untar(new ByteArrayInputStream(archive.toByteArray()), restored.toFile(), false, true);
        assertEquals("keep", Files.readString(sentinel));
        assertFalse(Files.isSymbolicLink(restored.resolve("parent")));
        assertFalse(Files.isSymbolicLink(restored.resolve("leaf")));
        assertEquals("cached parent", Files.readString(restored.resolve("parent/file")));
        assertEquals("cached leaf", Files.readString(restored.resolve("leaf")));
    }

    @Test
    void stillRejectsEscapingEntryPaths() throws IOException {
        var sentinel = Files.writeString(temp.resolve("outside"), "keep");
        for (var name : List.of("../outside", "nested/../../outside", sentinel.toString())) {
            for (boolean link : List.of(true, false)) {
                var archive = new ByteArrayOutputStream();
                try (var tar = new TarArchiveOutputStream(archive)) {
                    if (link)
                        addLink(tar, name, "/usr/bin/python3");
                    else
                        addFile(tar, name, "changed");
                }
                assertThrows(ExplicitException.class, () -> TarUtils.untar(
                        new ByteArrayInputStream(archive.toByteArray()), temp.resolve("restored").toFile(), false, true));
                assertEquals("keep", Files.readString(sentinel));
            }
        }
    }

    private static void addLink(TarArchiveOutputStream tar, String name, String target) throws IOException {
        var entry = new TarArchiveEntry(name, LF_SYMLINK, true);
        entry.setLinkName(target);
        tar.putArchiveEntry(entry);
        tar.closeArchiveEntry();
    }

    private static void addFile(TarArchiveOutputStream tar, String name, String content) throws IOException {
        var bytes = content.getBytes(StandardCharsets.UTF_8);
        var entry = new TarArchiveEntry(name, true);
        entry.setSize(bytes.length);
        tar.putArchiveEntry(entry);
        tar.write(bytes);
        tar.closeArchiveEntry();
    }
}
