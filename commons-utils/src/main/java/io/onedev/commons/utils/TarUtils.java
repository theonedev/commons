package io.onedev.commons.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.google.common.base.Preconditions;

public class TarUtils {
	
	private static final int BUFFER_SIZE = 64*1024;
	
	public static void tar(File baseDir, Collection<String> includes, Collection<String> excludes, 
			OutputStream os) {
		byte data[] = new byte[BUFFER_SIZE];
		try (TarArchiveOutputStream tos = new TarArchiveOutputStream(new BufferedOutputStream(os, BUFFER_SIZE))) {
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
		}
	}
	
	public static void untar(InputStream is, File destDir) {
	    byte data[] = new byte[BUFFER_SIZE];
		try (TarArchiveInputStream tis = new TarArchiveInputStream(new BufferedInputStream(is, BUFFER_SIZE))) {
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
					
				    try (OutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE)) {
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
		}
	}
	
}
