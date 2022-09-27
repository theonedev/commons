package io.onedev.commons.utils.command;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ExposeOutputStream implements OutputStreamHandler {

	private volatile OutputStream output;
	
	@Override
	public void handle(OutputStream output) {
		this.output = output;
	}

	public void write(String message) throws IOException {
		OutputStream outputCopy = output;
		if (outputCopy != null) 
			outputCopy.write(message.getBytes(StandardCharsets.UTF_8));
	}
	
	@Override
	public void waitFor() {
	}

}