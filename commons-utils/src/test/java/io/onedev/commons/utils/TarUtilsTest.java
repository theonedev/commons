package io.onedev.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static org.apache.commons.compress.archivers.tar.TarConstants.LF_SYMLINK;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

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
			assertTrue(extractedFileWithBackslash.exists(), "File with backslash in name should exist");
			assertTrue(extractedFileWithBackslash.isFile(), "Should be a file, not a directory");
			assertEquals("test content", Files.readString(extractedFileWithBackslash.toPath(), StandardCharsets.UTF_8));

			// Verify the normal file
			File extractedNormalFile = new File(destDir, "normal.txt");
			assertTrue(extractedNormalFile.exists(), "Normal file should exist");
			assertEquals("normal content", Files.readString(extractedNormalFile.toPath(), StandardCharsets.UTF_8));

			// Verify the subdirectory file
			File extractedSubFile = new File(destDir, "subdir/subfile.txt");
			assertTrue(extractedSubFile.exists(), "Subdirectory file should exist");
			assertEquals("sub content", Files.readString(extractedSubFile.toPath(), StandardCharsets.UTF_8));

			// Verify that no spurious "str" directory was created
			File spuriousDir = new File(destDir, "str");
			assertTrue(!spuriousDir.exists(), "No spurious 'str' directory should be created");

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
			assertTrue(extractedFileWithBackslash.exists(), "File with backslash should exist");
			assertEquals("backslash content", Files.readString(extractedFileWithBackslash.toPath(), StandardCharsets.UTF_8));

		} finally {
			FileUtils.deleteDir(sourceDir);
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testTarUntarWithExcludedPathPatterns() throws IOException {
		File sourceDir = Files.createTempDirectory("tar-test-source").toFile();
		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			File keepFile = new File(sourceDir, "keep.txt");
			Files.writeString(keepFile.toPath(), "keep content", StandardCharsets.UTF_8);

			File ignoredFile = new File(sourceDir, "ignored.log");
			Files.writeString(ignoredFile.toPath(), "ignored content", StandardCharsets.UTF_8);

			File ignoredDir = new File(sourceDir, "ignored-dir");
			ignoredDir.mkdir();
			File ignoredChild = new File(ignoredDir, "child.txt");
			Files.writeString(ignoredChild.toPath(), "child content", StandardCharsets.UTF_8);

			File nestedDir = new File(sourceDir, "nested");
			nestedDir.mkdir();
			File nestedKeep = new File(nestedDir, "keep.txt");
			Files.writeString(nestedKeep.toPath(), "nested keep content", StandardCharsets.UTF_8);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TarUtils.tar(sourceDir, List.of("*.log", "ignored-dir"), baos, true);

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TarUtils.untar(bais, destDir, true);

			assertTrue(new File(destDir, "keep.txt").exists(), "Unmatched file should exist");
			assertTrue(new File(destDir, "nested/keep.txt").exists(), "Unmatched nested file should exist");
			assertFalse(new File(destDir, "ignored.log").exists(), "Matched file should be excluded");
			assertFalse(new File(destDir, "ignored-dir").exists(), "Matched directory children should be excluded");
			assertFalse(new File(destDir, "ignored-dir/child.txt").exists(), "Matched directory children should be excluded");
		} finally {
			FileUtils.deleteDir(sourceDir);
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testTarUntarWithSymbolicLink() throws IOException {
		// Skip this test on Windows where creating symbolic links typically requires
		// elevated privileges and the path semantics differ
		if (File.separatorChar == '\\') {
			return;
		}

		File sourceDir = Files.createTempDirectory("tar-test-source").toFile();
		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			File targetFile = new File(sourceDir, "target.txt");
			Files.writeString(targetFile.toPath(), "target content", StandardCharsets.UTF_8);

			Path symlinkPath = new File(sourceDir, "link.txt").toPath();
			Files.createSymbolicLink(symlinkPath, Paths.get("target.txt"));

			File subDir = new File(sourceDir, "subdir");
			subDir.mkdir();
			Path relativeSymlinkPath = new File(subDir, "link-to-target.txt").toPath();
			Files.createSymbolicLink(relativeSymlinkPath, Paths.get("../target.txt"));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			TarUtils.tar(sourceDir, baos, true);

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TarUtils.untar(bais, destDir, true);

			File extractedTarget = new File(destDir, "target.txt");
			assertTrue(extractedTarget.exists(), "Target file should exist");
			assertEquals("target content", Files.readString(extractedTarget.toPath(), StandardCharsets.UTF_8));

			Path extractedSymlink = new File(destDir, "link.txt").toPath();
			assertTrue(Files.exists(extractedSymlink, LinkOption.NOFOLLOW_LINKS), "Symbolic link should exist");
			assertTrue(Files.isSymbolicLink(extractedSymlink), "Should be a symbolic link");
			assertEquals(Paths.get("target.txt"), Files.readSymbolicLink(extractedSymlink));
			assertEquals("target content", Files.readString(extractedSymlink, StandardCharsets.UTF_8));

			Path extractedRelativeSymlink = new File(destDir, "subdir/link-to-target.txt").toPath();
			assertTrue(Files.exists(extractedRelativeSymlink, LinkOption.NOFOLLOW_LINKS), "Relative symbolic link should exist");
			assertTrue(Files.isSymbolicLink(extractedRelativeSymlink), "Should be a symbolic link");
			assertEquals(Paths.get("../target.txt"), Files.readSymbolicLink(extractedRelativeSymlink));
			assertEquals("target content", Files.readString(extractedRelativeSymlink, StandardCharsets.UTF_8));

		} finally {
			FileUtils.deleteDir(sourceDir);
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testUntarRejectsEntryNameEscape() throws IOException {
		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			byte[] content = "malicious".getBytes(StandardCharsets.UTF_8);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
				TarArchiveEntry entry = new TarArchiveEntry("../escape.txt");
				entry.setSize(content.length);
				tos.putArchiveEntry(entry);
				tos.write(content);
				tos.closeArchiveEntry();
				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				TarUtils.untar(bais, destDir, true);
				fail("Expected an exception due to tar entry name escape");
			} catch (ExplicitException e) {
				assertTrue(e.getMessage().contains("Tar entry escape"), "Exception message should mention tar entry escape");
			}

			File escapedFile = new File(destDir.getParentFile(), "escape.txt");
			assertFalse(escapedFile.exists(), "Escaping file should not be created");

		} finally {
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testUntarRejectsSymbolicLinkEscape() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
				TarArchiveEntry entry = new TarArchiveEntry("escape.txt", LF_SYMLINK);
				entry.setLinkName("../../../etc/passwd");
				tos.putArchiveEntry(entry);
				tos.closeArchiveEntry();
				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				TarUtils.untar(bais, destDir, true);
				fail("Expected an exception due to symbolic link escape");
			} catch (ExplicitException e) {
				assertTrue(e.getMessage().contains("symbol link escape"), "Exception message should mention symbol link escape");
				assertTrue(e.getMessage().contains("escape.txt"), "Exception message should mention tar entry name");
			}

			File extractedSymlink = new File(destDir, "escape.txt");
			assertFalse(Files.exists(extractedSymlink.toPath(), LinkOption.NOFOLLOW_LINKS), "Escaping symbolic link should not be created");

		} finally {
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testUntarChainedSymbolicLinks() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			byte[] content = "target content".getBytes(StandardCharsets.UTF_8);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

				TarArchiveEntry fileEntry = new TarArchiveEntry("target.txt");
				fileEntry.setSize(content.length);
				tos.putArchiveEntry(fileEntry);
				tos.write(content);
				tos.closeArchiveEntry();

				TarArchiveEntry aliasEntry = new TarArchiveEntry("alias", LF_SYMLINK);
				aliasEntry.setLinkName("target.txt");
				tos.putArchiveEntry(aliasEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry chainEntry = new TarArchiveEntry("chain", LF_SYMLINK);
				chainEntry.setLinkName("alias");
				tos.putArchiveEntry(chainEntry);
				tos.closeArchiveEntry();

				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TarUtils.untar(bais, destDir, true);

			Path extractedAlias = new File(destDir, "alias").toPath();
			assertTrue(Files.isSymbolicLink(extractedAlias), "Alias symlink should exist");
			assertEquals(Paths.get("target.txt"), Files.readSymbolicLink(extractedAlias));

			Path extractedChain = new File(destDir, "chain").toPath();
			assertTrue(Files.isSymbolicLink(extractedChain), "Chain symlink should exist");
			assertEquals(Paths.get("alias"), Files.readSymbolicLink(extractedChain));

			assertEquals("target content",
					Files.readString(extractedChain, StandardCharsets.UTF_8));
		} finally {
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testUntarSymbolicLinkThroughDirectorySymlink() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			byte[] content = "inside content".getBytes(StandardCharsets.UTF_8);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

				TarArchiveEntry dirEntry = new TarArchiveEntry("versions/A/");
				tos.putArchiveEntry(dirEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry fileEntry = new TarArchiveEntry("versions/A/payload.txt");
				fileEntry.setSize(content.length);
				tos.putArchiveEntry(fileEntry);
				tos.write(content);
				tos.closeArchiveEntry();

				TarArchiveEntry currentEntry = new TarArchiveEntry("versions/Current", LF_SYMLINK);
				currentEntry.setLinkName("A");
				tos.putArchiveEntry(currentEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry topEntry = new TarArchiveEntry("payload", LF_SYMLINK);
				topEntry.setLinkName("versions/Current/payload.txt");
				tos.putArchiveEntry(topEntry);
				tos.closeArchiveEntry();

				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			TarUtils.untar(bais, destDir, true);

			Path extractedTop = new File(destDir, "payload").toPath();
			assertTrue(Files.isSymbolicLink(extractedTop), "Top symlink should exist");
			assertEquals("inside content",
					Files.readString(extractedTop, StandardCharsets.UTF_8));
		} finally {
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testUntarSymbolicLinkRejectedWhenPathEscapesThroughExistingSymlink() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File destDir = Files.createTempDirectory("tar-test-dest").toFile();
		File outsideDir = Files.createTempDirectory("tar-test-outside").toFile();

		try {
			Files.createSymbolicLink(new File(destDir, "shortcut").toPath(),
					outsideDir.toPath().toAbsolutePath());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

				TarArchiveEntry entry = new TarArchiveEntry("important", LF_SYMLINK);
				entry.setLinkName("shortcut/secret.txt");
				tos.putArchiveEntry(entry);
				tos.closeArchiveEntry();

				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				TarUtils.untar(bais, destDir, true);
				fail("Expected an exception due to symbolic link escape via existing symlink");
			} catch (ExplicitException e) {
				assertTrue(e.getMessage().contains("resolves outside"), "Exception message should mention resolves outside");
				assertTrue(e.getMessage().contains("important"), "Exception message should mention tar entry name");
			}

			assertFalse(Files.exists(new File(destDir, "important").toPath(), LinkOption.NOFOLLOW_LINKS), "Escaping symlink should not be created");
		} finally {
			FileUtils.deleteDir(destDir);
			FileUtils.deleteDir(outsideDir);
		}
	}

	@Test
	public void testUntarRejectsParentSegmentAfterSymbolicLink() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

				tos.putArchiveEntry(new TarArchiveEntry("d/e/"));
				tos.closeArchiveEntry();

				TarArchiveEntry upEntry = new TarArchiveEntry("d/e/up", LF_SYMLINK);
				upEntry.setLinkName("../..");
				tos.putArchiveEntry(upEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry escapeEntry = new TarArchiveEntry("escape", LF_SYMLINK);
				escapeEntry.setLinkName("d/e/up/../..");
				tos.putArchiveEntry(escapeEntry);
				tos.closeArchiveEntry();

				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				TarUtils.untar(bais, destDir, true);
				fail("Expected an exception due to symbolic link escape via parent segment after symlink");
			} catch (ExplicitException e) {
				assertTrue(e.getMessage().contains("resolves outside"), "Exception message should mention resolves outside");
				assertTrue(e.getMessage().contains("escape"), "Exception message should mention tar entry name");
			}

			assertFalse(Files.exists(new File(destDir, "escape").toPath(), LinkOption.NOFOLLOW_LINKS), "Escaping symlink should not be kept");
			assertFalse(Files.exists(new File(destDir, "d/e/up").toPath(), LinkOption.NOFOLLOW_LINKS), "Symlinks should be removed on failure");
		} finally {
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testUntarRejectsSymbolicLinkEscapeCausedByLaterEntry() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

				TarArchiveEntry escapeEntry = new TarArchiveEntry("escape", LF_SYMLINK);
				escapeEntry.setLinkName("a/..");
				tos.putArchiveEntry(escapeEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry selfEntry = new TarArchiveEntry("a", LF_SYMLINK);
				selfEntry.setLinkName(".");
				tos.putArchiveEntry(selfEntry);
				tos.closeArchiveEntry();

				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				TarUtils.untar(bais, destDir, true);
				fail("Expected an exception due to symbolic link escape caused by later entry");
			} catch (ExplicitException e) {
				assertTrue(e.getMessage().contains("resolves outside"), "Exception message should mention resolves outside");
			}

			assertFalse(Files.exists(new File(destDir, "escape").toPath(), LinkOption.NOFOLLOW_LINKS), "Escaping symlink should not be kept");
		} finally {
			FileUtils.deleteDir(destDir);
		}
	}

	@Test
	public void testUntarRejectsSymbolicLinkCreatedThroughCaseAliasedLink() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File parentDir = Files.createTempDirectory("tar-test-parent").toFile();
		File destDir = new File(parentDir, "dest");

		try {
			FileUtils.createDir(destDir);
			FileUtils.touchFile(new File(destDir, "case"));
			boolean caseInsensitive = new File(destDir, "CASE").exists();
			FileUtils.deleteFile(new File(destDir, "case"));
			if (!caseInsensitive) {
				return;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

				tos.putArchiveEntry(new TarArchiveEntry("d/e/"));
				tos.closeArchiveEntry();

				TarArchiveEntry upEntry = new TarArchiveEntry("d/e/up", LF_SYMLINK);
				upEntry.setLinkName("../..");
				tos.putArchiveEntry(upEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry aliasEntry = new TarArchiveEntry("AB", LF_SYMLINK);
				aliasEntry.setLinkName("d/e/up/..");
				tos.putArchiveEntry(aliasEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry innerEntry = new TarArchiveEntry("ab/l", LF_SYMLINK);
				innerEntry.setLinkName("x");
				tos.putArchiveEntry(innerEntry);
				tos.closeArchiveEntry();

				TarArchiveEntry replaceEntry = new TarArchiveEntry("Ab", LF_SYMLINK);
				replaceEntry.setLinkName("y");
				tos.putArchiveEntry(replaceEntry);
				tos.closeArchiveEntry();

				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				TarUtils.untar(bais, destDir, true);
				fail("Expected an exception due to symbolic link created through case aliased link");
			} catch (ExplicitException e) {
				assertTrue(e.getMessage().contains("ab/l"), "Exception message should mention tar entry name");
			}

			assertFalse(Files.exists(new File(parentDir, "l").toPath(), LinkOption.NOFOLLOW_LINKS), "Symlink should not be created outside destination");
			assertFalse(Files.exists(new File(destDir, "AB").toPath(), LinkOption.NOFOLLOW_LINKS), "Symlinks should be removed on failure");
		} finally {
			FileUtils.deleteDir(parentDir);
		}
	}

	@Test
	public void testUntarRejectsSymbolicLinkCycle() throws IOException {
		if (File.separatorChar == '\\') {
			return;
		}

		File destDir = Files.createTempDirectory("tar-test-dest").toFile();

		try {
			Files.createSymbolicLink(new File(destDir, "loop").toPath(), Paths.get("loop"));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (var gos = new GZIPOutputStream(baos);
				 var tos = new TarArchiveOutputStream(gos)) {
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

				TarArchiveEntry entry = new TarArchiveEntry("follower", LF_SYMLINK);
				entry.setLinkName("loop");
				tos.putArchiveEntry(entry);
				tos.closeArchiveEntry();

				tos.finish();
			}

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			try {
				TarUtils.untar(bais, destDir, true);
				fail("Expected an exception due to symbolic link cycle");
			} catch (ExplicitException e) {
				assertTrue(e.getMessage().contains("Too many"), "Exception message should mention too many symbolic links");
			}
		} finally {
			FileUtils.deleteDir(destDir);
		}
	}
}
