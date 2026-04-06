package io.onedev.commons.utils.command;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.bootstrap.Bootstrap;
import io.onedev.commons.bootstrap.SecretMasker;

public class StreamPumper {

	private static final Logger logger = LoggerFactory.getLogger(StreamPumper.class);

    private static final int BUFFER_SIZE = 64*1024;

	public static Future<?> pump(InputStream input, @Nullable OutputStream output) {
    	SecretMasker masker = SecretMasker.get();
    	
    	return Bootstrap.executorService.submit(() -> {
			if (masker != null)
				SecretMasker.push(masker);
			try {
				byte[] buf = new byte[BUFFER_SIZE];

				int length;
				while ((length = input.read(buf)) != -1) {
					if (output != null) {
						try {
							output.write(buf, 0, length);
							output.flush();
						} catch (Throwable t) {
							logger.error("Error writing to output stream", t);
						}
					}
				}
			} catch (Throwable t) {
				logger.error("Error pumping stream", t);
			} finally {
				closeQuietly(input);
				closeQuietly(output);
				
				if (masker != null)
					SecretMasker.pop();
			}
		});
    }

	public static Function<InputStream, Future<?>> pumpTo(@Nullable OutputStream os) {
		return is -> pump(is, os);
	}

	public static Function<OutputStream, Future<?>> pumpFrom(@Nullable InputStream is) {
		return os -> {
			if (is != null) {
				return pump(is, os);
			} else {
				closeQuietly(os);
				return CompletableFuture.completedFuture(null);
			}
		};
	}

}
