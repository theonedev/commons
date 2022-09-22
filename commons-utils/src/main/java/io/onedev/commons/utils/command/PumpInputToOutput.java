package io.onedev.commons.utils.command;

import java.io.InputStream;
import java.io.OutputStream;

public class PumpInputToOutput implements OutputStreamHandler {

	private final InputStream input;
	
	private StreamPumper pumper;

	public PumpInputToOutput(InputStream input) {
		this.input = input;
	}
	
	@Override
	public void handle(OutputStream output) {
		pumper = new StreamPumper(input, output);
	}

	@Override
	public void waitFor() {
		pumper.waitFor();
	}

}
