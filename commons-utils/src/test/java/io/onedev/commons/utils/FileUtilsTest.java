package io.onedev.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;

public class FileUtilsTest {

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
			FileUtils.deleteDir(root.toFile());
			assertFalse(Files.exists(root));
			assertEquals("keep", Files.readString(external.resolve("keep.txt")));
			assertEquals(readOnly, Files.getPosixFilePermissions(external));
		} finally {
			if (Files.exists(root.resolve("cache/module@v1.0.0")))
				root.resolve("cache/module@v1.0.0").toFile().setWritable(true);
			external.toFile().setWritable(true);
			FileUtils.deleteDir(root.toFile());
			FileUtils.deleteDir(external.toFile());
		}
	}
}
