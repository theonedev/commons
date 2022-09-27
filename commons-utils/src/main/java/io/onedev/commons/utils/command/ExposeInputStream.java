package io.onedev.commons.utils.command;

import java.io.IOException;
import java.io.InputStream;

public class ExposeInputStream implements InputStreamHandler {

	private volatile InputStream input;
	
	@Override
	public void handle(InputStream input) {
		this.input = input;
	}

	public void close() {
		InputStream inputCopy = input;
		if (inputCopy != null) {
			try {
				inputCopy.close();
			} catch (IOException e) {
			}
		}
	}
	
	@Override
	public void waitFor() {
	}

}