package io.onedev.commons.utils.command;

import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;

public class PumpInputToOutput extends ExposeInputStream {

	private final OutputStream output;
	
	private StreamPumper pumper;

	public PumpInputToOutput(@Nullable OutputStream output) {
		this.output = output;
	}
	
	@Override
	public void handle(InputStream input) {
		super.handle(input);
		pumper = new StreamPumper(input, output);
	}

	@Override
	public void waitFor() {
		pumper.waitFor();
	}

}
