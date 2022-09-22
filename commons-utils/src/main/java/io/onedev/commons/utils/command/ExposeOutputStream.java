package io.onedev.commons.utils.command;

import java.io.OutputStream;

import com.google.common.base.Preconditions;

public class ExposeOutputStream implements OutputStreamHandler {

	private OutputStream output;
	
	@Override
	public void handle(OutputStream output) {
		this.output = output;
	}

	public OutputStream getOutput() {
		return Preconditions.checkNotNull(output);
	}
	
	@Override
	public void waitFor() {
	}

}