package io.onedev.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class FileUtilsTest {

	@Test
	public void optionallySkipsSymlinksWhenListingFiles() throws Exception {
		var root = Files.createTempDirectory("list-links");
		var external = Files.createTempDirectory("list-link-target");
		try {
			assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
			var regular = Files.writeString(root.resolve("regular.txt"), "regular");
			Files.writeString(root.resolve("excluded.txt"), "excluded");
			Files.writeString(external.resolve("outside.txt"), "outside");
			Files.createSymbolicLink(root.resolve("file-link.txt"), regular);
			Files.createSymbolicLink(root.resolve("directory-link"), external);
			var includes = List.of("**/*.txt");
			var excludes = List.of("excluded.txt");
			assertEquals(Set.of("regular.txt", "file-link.txt", "directory-link/outside.txt"),
					new HashSet<>(FileUtils.listPaths(root.toFile(), includes, excludes)));
			assertEquals(FileUtils.listFiles(root.toFile(), includes, excludes),
					FileUtils.listFiles(root.toFile(), includes, excludes, true));
			assertEquals(List.of(regular.toFile()), FileUtils.listFiles(root.toFile(), includes, excludes, false));
			Files.createSymbolicLink(root.resolve("dangling.txt"), root.resolve("missing"));
			Files.createSymbolicLink(root.resolve("cycle"), root);
			assertEquals(List.of("regular.txt"), FileUtils.listPaths(root.toFile(), includes, excludes, false));
			assertEquals(List.of(), FileUtils.listFiles(root.toFile(),
					List.of("directory-link/outside.txt"), List.of(), false));
		} finally {
			FileUtils.deletePath(root.toFile());
			FileUtils.deletePath(external.toFile());
		}
	}

	@Test
	public void deletesFilesAndMissingPaths() throws Exception {
		var file = Files.createTempFile("delete-file", ".tmp");
		try {
			FileUtils.deletePath(file.toFile());
			assertFalse(Files.exists(file));
			FileUtils.deletePath(file.toFile(), 1);
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	public void unlinksRootSymlinksWithoutDeletingTargets() throws Exception {
		var root = Files.createTempDirectory("delete-links");
		try {
			assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
			var directory = Files.createDirectory(root.resolve("directory"));
			var file = Files.writeString(directory.resolve("keep.txt"), "keep");
			for (var target : new java.nio.file.Path[] {directory, file, root.resolve("missing")}) {
				var link = Files.createSymbolicLink(root.resolve("link"), target);
				FileUtils.deletePath(link.toFile());
				assertFalse(Files.exists(link, LinkOption.NOFOLLOW_LINKS));
				assertEquals("keep", Files.readString(file));
			}
		} finally {
			FileUtils.deletePath(root.toFile());
		}
	}

	@Test
	public void deletesReadOnlyDirectoriesWithoutFollowingLinks() throws Exception {
		var root = Files.createTempDirectory("delete-readonly");
		var external = Files.createTempDirectory("delete-link-target");
		try {
			assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
			var module = Files.createDirectories(root.resolve("cache/module@v1.0.0"));
			Files.writeString(module.resolve("module.go"), "package module");
			Files.writeString(external.resolve("keep.txt"), "keep");
			Files.createSymbolicLink(root.resolve("outside"), external);
			var readOnly = PosixFilePermissions.fromString("r-xr-xr-x");
			Files.setPosixFilePermissions(module, readOnly);
			Files.setPosixFilePermissions(external, readOnly);
			FileUtils.deletePath(root.toFile());
			assertFalse(Files.exists(root));
			assertEquals("keep", Files.readString(external.resolve("keep.txt")));
			assertEquals(readOnly, Files.getPosixFilePermissions(external));
		} finally {
			if (Files.exists(root.resolve("cache/module@v1.0.0")))
				root.resolve("cache/module@v1.0.0").toFile().setWritable(true);
			external.toFile().setWritable(true);
			FileUtils.deletePath(root.toFile());
			FileUtils.deletePath(external.toFile());
		}
	}
}
