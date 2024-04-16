package io.onedev.commons.utils.command;

import io.onedev.commons.bootstrap.Bootstrap;
import io.onedev.commons.bootstrap.SensitiveMasker;
import io.onedev.commons.utils.ImmediateFuture;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class StreamPumper {

    private static final int BUFFER_SIZE = 64*1024;

	public static Future<?> pump(InputStream input, @Nullable OutputStream output) {
    	SensitiveMasker masker = SensitiveMasker.get();
    	
    	return Bootstrap.executorService.submit(() -> {
			if (masker != null)
				SensitiveMasker.push(masker);
			try {
				byte[] buf = new byte[BUFFER_SIZE];

				try {
					int length;
					while ((length = input.read(buf)) != -1) {
						if (output != null) {
							output.write(buf, 0, length);
							output.flush();
						}
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					try {
						while (input.read(buf) != -1);
					} catch (IOException ignored) {
					}
					closeQuietly(input);
					closeQuietly(output);
				}
			} finally {
				if (masker != null)
					SensitiveMasker.pop();
			}
		});
    }

	public static Function<InputStream, Future<?>> pump(@Nullable OutputStream os) {
		return is -> pump(is, os);
	}

	public static Function<OutputStream, Future<?>> pump(@Nullable InputStream is) {
		return os -> {
			if (is != null) {
				return pump(is, os);
			} else {
				closeQuietly(os);
				return new ImmediateFuture<Void>(null);
			}
		};
	}

}
