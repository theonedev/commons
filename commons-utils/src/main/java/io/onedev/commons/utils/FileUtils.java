package io.onedev.commons.utils;

import static java.nio.file.Files.isSymbolicLink;
import static java.util.Arrays.asList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.DirectoryScanner;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.bootstrap.Bootstrap;

@SuppressWarnings("deprecation")
public class FileUtils extends org.apache.commons.io.FileUtils {

	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	/** Unix file type mask and socket type (S_IFMT, S_IFSOCK) */
	private static final int S_IFMT = 0170000;
	
	private static final int S_IFSOCK = 0140000;

	/**
	 * Load properties from specified path inside specified directory or jar file. 
	 * @param file
	 * 			A jar file or a directory to load property file from.
	 * @param path 
	 * 			relative path of the property file inside the jar or to the directory.
	 * @return
	 * 			Content of the property file. Null if not found.
	 */
	@Nullable
	public static Properties loadProperties(File file, String path) {
		if (file.isFile() && file.getName().endsWith(".jar")) {
			ZipFile zip = null;
			try {
				zip = new ZipFile(file);
				ZipEntry entry = zip.getEntry(path);
				if (entry != null) {
					InputStream is = null;
					try {
						is = zip.getInputStream(entry);
						Properties props = new Properties();
						props.load(is);
						return props;
					} catch (IOException e) {
						throw new RuntimeException(e);
					} finally {
						if (is != null) {
							is.close();
						}
					}
				} else {
					return null;
				}
			} catch (Exception e) {
				throw ExceptionUtils.unchecked(e);
			} finally {
				if (zip != null) {
					try {
						zip.close();
					} catch (IOException e) {
					}
				}
			}
		} else if (file.isDirectory() && new File(file, path).exists()) {
			Properties props = new Properties();
			InputStream is = null;
			try {
				is = new FileInputStream(new File(file, path));
				props.load(is);
				return props;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Load properties from specified file. Surrounding white spaces of property values will 
	 * be trimmed off. And a property is considered not defined if the value is trimmed to 
	 * empty.
	 * @param file 
	 * 			file to load properties from
	 * @return
	 */
	public static Properties loadProperties(File file) {
		Properties props = new Properties();
		try (InputStream is = new FileInputStream(file)) {
			props.load(is);
			
			for (Iterator<Entry<Object, Object>> it = props.entrySet().iterator(); it.hasNext();) {
				Entry<Object, Object> entry = it.next();
				String value = (String) entry.getValue();
				value = value.trim();
				if (value.length() == 0)
					it.remove();
				else
					entry.setValue(value);
			}
			return props;
		} catch (Exception e) {
			throw ExceptionUtils.unchecked(e);
		}
	}
	
	public static boolean isWritable(File dir) {
		boolean dirExists = dir.exists();
		File tempFile = null;
		try {
			FileUtils.createDir(dir);
			tempFile = createTempFile("test", "test", dir);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (tempFile != null)
				tempFile.delete();
			if (!dirExists)
				deleteDir(dir);
		}
	}
	
    /**
     * Get default file encoding of underlying OS
     * @return
     */
    public static String getDefaultEncoding() {
    	return new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding();
    }
    
    /**
     * List all files matching specified patterns under specified base directory.
     * 
     * @param baseDir 
     * 			Base directory to scan files in
     * @return
     * 			Collection of files matching specified path pattern. Directories will not be included even 
     * 			if its path matches the pattern
     */
    public static Collection<File> listFiles(File baseDir, Collection<String> includes, Collection<String> excludes) {
		return listPaths(baseDir, includes, excludes).stream()
				.map(it->new File(baseDir, it))
				.collect(Collectors.toList());
    }

	public static Collection<String> listPaths(File baseDir, Collection<String> includes, Collection<String> excludes) {
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(baseDir);
		scanner.setIncludes(includes.toArray(new String[0]));
		scanner.setExcludes(excludes.toArray(new String[0]));
		scanner.scan();
		return asList(scanner.getIncludedFiles());
	}

    public static void deleteDir(File dir) {
    	if (isSymbolicLink(dir.toPath())) {
    		deleteFile(dir);
    	} else if (dir.exists()) {
    		cleanDir(dir);
    		deleteFile(dir);
    	}
    }

	public static void deleteDir(File dir, int retries) {
		int retried = 0;
		while (dir.exists()) {
			try {
				deleteDir(dir);
				break;
			} catch (Exception e) {
				if (retried++ < retries) {
					logger.error("Error deleting directory '" + dir.getAbsolutePath() + "', will retry later...", e);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e2) {
					}
				} else {
					throw e;
				}
			}
		}
	}
	
	public static void deleteFile(File file) {
		int maxTries = 10;
    	int numTries = 1;

    	while (true) {
    		if (file.delete())
    			break;
    		
    		if (file.exists()) {
            	if (numTries == maxTries) {
            		throw new RuntimeException("Failed to delete file " + file);
            	} else {
                    System.gc();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e2) {
                    }
                    numTries++;
            	}
    		} else {
    			break;
    		}
        }
    }    
    
	public static void writeFile(File file, String content, Charset encoding) {
		try {
			org.apache.commons.io.FileUtils.writeStringToFile(file, content, encoding);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
    
	public static void writeFile(File file, String content) {
		try {
			org.apache.commons.io.FileUtils.writeStringToFile(file, content, Charset.defaultCharset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void touchFile(File file) {
		try {
			org.apache.commons.io.FileUtils.touch(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void createDir(File dir) {
		Bootstrap.createDir(dir);
	}

	public static void cleanDir(File dir) {
		if (dir.exists()) {
	        for (File file : dir.listFiles()) {
	        	if (file.isDirectory())
	        		deleteDir(file);
	        	else
	        		deleteFile(file);
	        }
		} else {
			if (isSymbolicLink(dir.toPath()))
				deleteFile(dir);
			createDir(dir);
		}
	}

	public static File getTempDir() {
		return new File(System.getProperty("java.io.tmpdir"));
	}

	public static File createTempFile(String prefix, String suffix, File directory) {
		return createTempFile(prefix, suffix, directory, "rw-rw----");
	}

	public static File createTempFile(String prefix, String suffix) {
		return createTempFile(prefix, suffix, getTempDir());
	}

	public static File createTempFile() {
		return createTempFile("file", "tmp");
	}

	public static boolean isPosixFileAttributesSupported() {
		return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
	}

	public static boolean isUnixSocket(Path path) {
		if (!isPosixFileAttributesSupported())
			return false;
		try {
			Integer mode = (Integer) Files.getAttribute(path, "unix:mode", LinkOption.NOFOLLOW_LINKS);
			return mode != null && (mode & S_IFMT) == S_IFSOCK;
		} catch (IOException | UnsupportedOperationException e) {
			return false;
		}
	}

	public static File createTempFile(String prefix, String suffix, File directory, String perms) {
		try {
			if (isPosixFileAttributesSupported()) {
				Set<PosixFilePermission> filePerms = PosixFilePermissions.fromString(perms);
				FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(filePerms);

				return Files.createTempFile(directory.toPath(), prefix, suffix, attrs).toFile();
			} else {
				return Files.createTempFile(directory.toPath(), prefix, suffix).toFile();
			}
		} catch (IllegalArgumentException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static File createTempDir(String prefix, String perms) {
		try {
			if (isPosixFileAttributesSupported()) {
				Set<PosixFilePermission> filePerms = PosixFilePermissions.fromString(perms);
				FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(filePerms);

				return Files.createTempDirectory(getTempDir().toPath(), prefix, attrs).toFile();
			} else {
				return Files.createTempDirectory(getTempDir().toPath(), prefix).toFile();
			}
		} catch (IllegalArgumentException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static File createTempDir(String prefix) {
		return createTempDir(prefix, "rwxrwx---");
	}

	public static File createTempDir() {
		return createTempDir("dir");
	}

}
