package io.onedev.commons.utils.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;

public class PumpOutputFromInput implements OutputStreamHandler {

	private final InputStream input;
	
	private StreamPumper pumper;

	public PumpOutputFromInput(@Nullable InputStream input) {
		this.input = input;
	}
	
	@Override
	public void handle(OutputStream output) {
		if (input != null) {
			pumper = new StreamPumper(input, output);
		} else {
			try {
				output.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void waitFor() {
		if (pumper != null)
			pumper.waitFor();
	}

}
