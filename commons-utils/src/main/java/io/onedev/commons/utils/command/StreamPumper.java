package io.onedev.commons.utils.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import io.onedev.commons.bootstrap.Bootstrap;
import io.onedev.commons.bootstrap.SensitiveMasker;

public class StreamPumper {

    private static final int BUFFER_SIZE = 64*1024;
    
	private final Future<?>	future;
	
	public StreamPumper(InputStream input, @Nullable OutputStream output) {
    	SensitiveMasker masker = SensitiveMasker.get();
    	
    	future = Bootstrap.executorService.submit(new Runnable() {

			public void run() {
				if (masker != null)
					SensitiveMasker.push(masker);
				try {
			        byte[] buf = new byte[BUFFER_SIZE];
			
			        try {
				        int length;
			            while ((length = input.read(buf)) > 0) {
			            	if (output != null) {
			            		output.write(buf, 0, length);
			            		output.flush();
			            	}
			            }
			        } catch (IOException e) {
			        	throw new RuntimeException(e);
					} finally {
			        	try {
							while (input.read(buf) > 0);
						} catch (IOException e) {
						}
		            	try {
							input.close();
						} catch (IOException e) {
						}
			        	if (output != null) {
							try {
								output.close();
							} catch (IOException e) {
							}
			        	}
			        }
				} finally {
					if (masker != null)
						SensitiveMasker.pop();
				}
			}
    		
    	});
    }
	
    public void waitFor() {
    	try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
    }

}
