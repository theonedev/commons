package io.onedev.commons.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
	
	private static final int BUFFER_SIZE = 64*1024;
	
    /**
     * Zip specified directory recursively as specified file.
     * @param dir
     * @param file
     */
    public static void zip(File dir, File file) {
    	try (OutputStream os = new FileOutputStream(file)) {
    		zip(dir, os);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
		};
    }
    
    /**
     * Zip specified directory recursively as specified file.
     * @param dir
     * @param file
     */
    public static void zip(File dir, OutputStream os) {
    	try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os, BUFFER_SIZE))) {
			zos.setLevel(Deflater.DEFAULT_COMPRESSION);
			zip(zos, dir, "");
    	} catch (IOException e) {
    		throw new RuntimeException(e);
		}
    }
    
    private static void zip(ZipOutputStream zos, File dir, String basePath) {
		byte buffer[] = new byte[BUFFER_SIZE];
		
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
	 */
	public static void unzip(File srcFile, File destDir) {
	    try (InputStream is = new FileInputStream(srcFile);) {
	    	unzip(is, destDir);
	    } catch (Exception e) {
			throw ExceptionUtils.unchecked(e);
	    }
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
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is, BUFFER_SIZE));		
	    try {
		    ZipEntry entry;
		    while((entry = zis.getNextEntry()) != null) {
				if (entry.getName().endsWith("/")) {
					FileUtils.createDir(new File(destDir, entry.getName()));
				} else {		    		
				    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(destDir, entry.getName())), BUFFER_SIZE);) {
				        int count;
				        byte data[] = new byte[BUFFER_SIZE];
				        while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) 
				        	bos.write(data, 0, count);
				        bos.flush();
				    }
				}
		    }
	    } catch (Exception e) {
			throw ExceptionUtils.unchecked(e);
	    }
	} 	

}
