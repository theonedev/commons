package io.onedev.commons.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class TarUtilsTest {

	@Test
	public void testTarUntarWithBackslashInFilename() throws IOException {
		// Skip this test on Windows where backslash is not a valid filename character
		if (File.separatorChar == '\\') {
			return;
		}

		File sourceDir = Files.createTempDirectory("tar-test-source").toFile();
		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			// Create a file with a backslash in its name (valid on Linux/macOS)
			File fileWithBackslash = new File(sourceDir, "str\\escape.txt");
			Files.writeString(fileWithBackslash.toPath(), "test content", StandardCharsets.UTF_8);

			// Create a normal file for comparison
			File normalFile = new File(sourceDir, "normal.txt");
			Files.writeString(normalFile.toPath(), "normal content", StandardCharsets.UTF_8);

			// Create a subdirectory with a file
			File subDir = new File(sourceDir, "subdir");
			subDir.mkdir();
			File subFile = new File(subDir, "subfile.txt");
			Files.writeString(subFile.toPath(), "sub content", StandardCharsets.UTF_8);

			// Tar the source directory
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TarUtils.tar(sourceDir, baos, true);

			// Untar to destination directory
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TarUtils.untar(bais, destDir, true);

			// Verify the file with backslash in name exists and has correct content
			File extractedFileWithBackslash = new File(destDir, "str\\escape.txt");
			assertTrue("File with backslash in name should exist", extractedFileWithBackslash.exists());
			assertTrue("Should be a file, not a directory", extractedFileWithBackslash.isFile());
			assertEquals("test content", Files.readString(extractedFileWithBackslash.toPath(), StandardCharsets.UTF_8));

			// Verify the normal file
			File extractedNormalFile = new File(destDir, "normal.txt");
			assertTrue("Normal file should exist", extractedNormalFile.exists());
			assertEquals("normal content", Files.readString(extractedNormalFile.toPath(), StandardCharsets.UTF_8));

			// Verify the subdirectory file
			File extractedSubFile = new File(destDir, "subdir/subfile.txt");
			assertTrue("Subdirectory file should exist", extractedSubFile.exists());
			assertEquals("sub content", Files.readString(extractedSubFile.toPath(), StandardCharsets.UTF_8));

			// Verify that no spurious "str" directory was created
			File spuriousDir = new File(destDir, "str");
			assertTrue("No spurious 'str' directory should be created", !spuriousDir.exists());

		} finally {
			// Clean up
			FileUtils.deleteDir(sourceDir);
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testTarUntarWithIncludes() throws IOException {
		// Skip this test on Windows where backslash is not a valid filename character
		if (File.separatorChar == '\\') {
			return;
		}

		File sourceDir = Files.createTempDirectory("tar-test-source").toFile();
		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			// Create a file with a backslash in its name
			File fileWithBackslash = new File(sourceDir, "test\\file.txt");
			Files.writeString(fileWithBackslash.toPath(), "backslash content", StandardCharsets.UTF_8);

			// Create another file
			File otherFile = new File(sourceDir, "other.txt");
			Files.writeString(otherFile.toPath(), "other content", StandardCharsets.UTF_8);

			// Tar with includes (using the overloaded method)
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TarUtils.tar(sourceDir, null, null, baos, true);

			// Untar to destination directory
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TarUtils.untar(bais, destDir, true);

			// Verify the file with backslash in name exists
			File extractedFileWithBackslash = new File(destDir, "test\\file.txt");
			assertTrue("File with backslash should exist", extractedFileWithBackslash.exists());
			assertEquals("backslash content", Files.readString(extractedFileWithBackslash.toPath(), StandardCharsets.UTF_8));

		} finally {
			FileUtils.deleteDir(sourceDir);
			FileUtils.deleteDir(destDir);
		}
	}
}
