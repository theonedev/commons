package io.onedev.commons.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.onedev.commons.bootstrap.Bootstrap;

public class FileUtils extends org.apache.commons.io.FileUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
	
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
			tempFile = File.createTempFile("test", "test", dir);
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
     * @param pathPattern 
     * 			Pattern of file path to be used for search
     * @return
     * 			Collection of files matching specified path pattern. Directories will not be included even 
     * 			if its path matches the pattern
     */
    public static Collection<File> listFiles(File baseDir, Collection<String> includes, Collection<String> excludes) {
    	Collection<File> files = new ArrayList<File>();
    	
    	DirectoryScanner scanner = new DirectoryScanner();
    	scanner.setBasedir(baseDir);
    	scanner.setIncludes(includes.toArray(new String[0]));
    	scanner.setExcludes(excludes.toArray(new String[0]));
    	scanner.scan();
    	
    	for (String path: scanner.getIncludedFiles()) 
    		files.add(new File(baseDir, path));
    	return files;
    }

    public static void deleteDir(File dir) {
    	if (Files.isSymbolicLink(dir.toPath())) {
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
    
	public static void writeFile(File file, String content, String encoding) {
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
	
    public static File createTempDir(String prefix) {
    	return Bootstrap.createTempDir(prefix);
    }
    
    public static File createTempDir() {
    	return createTempDir("temp");
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
			if (Files.isSymbolicLink(dir.toPath())) 
				deleteFile(dir);
			createDir(dir);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T readFile(File file, Callable<T> callable) {
		T result = null;
		String lockName;
		try {
			lockName = file.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Lock lock = LockUtils.getReadWriteLock(lockName).readLock();
		try {
			lock.lockInterruptibly();
			if (file.exists()) {
				byte[] bytes = FileUtils.readFileToByteArray(file);
				result = (T) SerializationUtils.deserialize(bytes);
			}
		} catch (Exception e) {
			logger.error("Error reading callable result from file '" + file.getAbsolutePath() 
					+ "', fall back to execute callable.", e);
		} finally {
			lock.unlock();
		}
		
		if (result == null) {
			FileUtils.createDir(file.getParentFile());
			
			lock = LockUtils.getReadWriteLock(lockName).writeLock();
			try {
				lock.lockInterruptibly();
				Preconditions.checkNotNull(result = callable.call());
				
				FileUtils.writeByteArrayToFile(file, SerializationUtils.serialize((Serializable) result));
			} catch (Exception e) {
				throw ExceptionUtils.unchecked(e);
			} finally {
				lock.unlock();
			}
		}
		
		return result;
	}
	
	public static void tar(File baseDir, Collection<String> includes, Collection<String> excludes, 
			OutputStream os, boolean compress) {
		byte data[] = new byte[Bootstrap.BUFFER_SIZE];

		TarArchiveOutputStream tos = null;
		try {
			if (compress)
				tos = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(os, Bootstrap.BUFFER_SIZE), Bootstrap.BUFFER_SIZE));
			else
				tos = new TarArchiveOutputStream(new BufferedOutputStream(os, Bootstrap.BUFFER_SIZE));
			tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			tos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
			for (File file: FileUtils.listFiles(baseDir, includes, excludes)) {
	    		String basePath = PathUtils.parseRelative(file.getAbsolutePath(), baseDir.getAbsolutePath());
	    		Preconditions.checkNotNull(basePath);
	    		if (basePath.length() == 0)
	    			continue;
	    		
	    		basePath = basePath.substring(1);
	    		if (!file.isFile() && !basePath.endsWith("/"))
	    			basePath += "/";
				
				TarArchiveEntry entry = new TarArchiveEntry(basePath);
				if (file.isFile()) {
					entry.setSize(file.length());
					if (file.canExecute())
						entry.setMode(entry.getMode() | 0000100);
					entry.setModTime(file.lastModified());
				}
				
				tos.putArchiveEntry(entry);
				
				if (file.isFile()) {
					try (InputStream is = new FileInputStream(file)) {
						int count;
						while((count = is.read(data)) != -1) 
							tos.write(data, 0, count);
					}
				}
				tos.closeArchiveEntry();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (tos != null) {
				try {
					tos.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	public static void untar(InputStream is, File destDir, boolean compressed) {
	    byte data[] = new byte[Bootstrap.BUFFER_SIZE];
	    TarArchiveInputStream tis = null;
		try {
		    if (compressed)
		    	tis = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(is, Bootstrap.BUFFER_SIZE), Bootstrap.BUFFER_SIZE));
		    else
		    	tis = new TarArchiveInputStream(new BufferedInputStream(is, Bootstrap.BUFFER_SIZE));
			TarArchiveEntry entry;
			while((entry = tis.getNextTarEntry()) != null) {
				if (entry.getName().contains("..")) 
					throw new RuntimeException("Upper directory is not allowed to avoid zipslip security volnerability");
				if (!entry.getName().endsWith("/")) { 
					File destFile = new File(destDir, entry.getName());
					File parentFile = destFile.getParentFile();
					FileUtils.createDir(parentFile);
					
					int count;
		
					if (destFile.exists()) 
						FileUtils.deleteFile(destFile);
					
				    try (OutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile), Bootstrap.BUFFER_SIZE)) {
				        while((count = tis.read(data)) != -1) 
				        	bos.write(data, 0, count);
				        if ((entry.getMode() & 0000100) != 0)
				        	destFile.setExecutable(true);
				    } finally {
				        destFile.setLastModified(entry.getModTime().getTime());
				    }
				} else {
					FileUtils.createDir(new File(destDir, entry.getName()));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (tis != null) {
				try {
					tis.close();
				} catch (IOException e) {
				}
			}
		}
	}
		
    /**
     * Zip specified directory recursively as specified file, with 
     * @param dir
     * @param file
     */
    public static void zip(File dir, File file, @Nullable String executables) {
		Zip zip = new Zip();
		
		Project antProject = new Project();
		antProject.init();
		antProject.addBuildListener(new BuildListener(){

			@Override
			public void messageLogged(BuildEvent event) {
				if (event.getPriority() == Project.MSG_ERR)
					logger.error(event.getMessage());
				else if (event.getPriority() == Project.MSG_WARN)
					logger.warn(event.getMessage());
				else if (event.getPriority() == Project.MSG_INFO)
					logger.info(event.getMessage());
				else
					logger.debug(event.getMessage());
			}

			@Override
			public void buildFinished(BuildEvent event) {
			}

			@Override
			public void buildStarted(BuildEvent event) {
			}

			@Override
			public void targetFinished(BuildEvent event) {
			}

			@Override
			public void targetStarted(BuildEvent event) {
			}

			@Override
			public void taskFinished(BuildEvent event) {
			}

			@Override
			public void taskStarted(BuildEvent event) {
			}
			
		});
		
		zip.setProject(antProject);
		zip.setDestFile(file);
		
		if (executables != null) {
			ZipFileSet zipFileSet = new ZipFileSet();
			zipFileSet.setDir(dir);
			zipFileSet.setExcludes(executables);
			zip.addZipfileset(zipFileSet);
			
			zipFileSet = new ZipFileSet();
			zipFileSet.setDir(dir);
			zipFileSet.setIncludes(executables);
			zipFileSet.setFileMode("755");
			zip.addZipfileset(zipFileSet);
		} else {
			ZipFileSet zipFileSet = new ZipFileSet();
			zipFileSet.setDir(dir);
			zip.addZipfileset(zipFileSet);
		}
		
		zip.execute();
    }
    
    /**
     * Zip specified directory recursively as specified file.
     * @param dir
     * @param file
     */
    public static void zip(File dir, OutputStream os) {
    	try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os, Bootstrap.BUFFER_SIZE))) {
			zos.setLevel(Deflater.DEFAULT_COMPRESSION);
			zip(zos, dir, "");
    	} catch (IOException e) {
    		throw new RuntimeException(e);
		}
    }
    
    private static void zip(ZipOutputStream zos, File dir, String basePath) {
		byte buffer[] = new byte[Bootstrap.BUFFER_SIZE];
		
		try {
			if (basePath.length() != 0)
				zos.putNextEntry(new ZipEntry(basePath + "/"));

			for (File file: dir.listFiles()) {
				if (file.isDirectory()) {
					if (basePath.length() != 0)
						zip(zos, file, basePath + "/" + file.getName());
					else
						zip(zos, file, file.getName());
				} else {
					try (FileInputStream is = new FileInputStream(file)) {
						if (basePath.length() != 0)
							zos.putNextEntry(new ZipEntry(basePath + "/" + file.getName()));
						else
							zos.putNextEntry(new ZipEntry(file.getName()));
						int len;
					    while ((len = is.read(buffer)) > 0)
					    	zos.write(buffer, 0, len);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
	 * Unzip files matching specified matcher from specified file to specified directory.
	 * 
	 * @param srcFile 
	 * 		zip file to extract from
	 * @param 
	 * 		destDir destination directory to extract to
	 * @param matcher 
	 * 		if not null, only entries with name matching this param in the zip will be extracted.
	 * 		Use null if you want to extract all entries from the zip file.  
	 */
	public static void unzip(File srcFile, File destDir) {
		Bootstrap.unzip(srcFile, destDir);
	} 	
	
	/**
	 * Unzip files matching specified matcher from input stream to specified directory.
	 * 
	 * @param is
	 * 			input stream to unzip files from. This method will not close the stream 
	 * 			after using it. Caller should be responsible for closing
	 * @param destDir
	 * 			destination directory to extract files to
	 */
	public static void unzip(InputStream is, File destDir) {
		Bootstrap.unzip(is, destDir);
	} 		
	
}
