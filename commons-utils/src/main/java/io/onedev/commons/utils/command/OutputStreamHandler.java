package io.onedev.commons.utils.command;

import java.io.OutputStream;

public interface OutputStreamHandler {

	void handle(OutputStream output);
	
	void waitFor();
	
}
