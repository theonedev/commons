package io.onedev.commons.utils.command;

import java.io.InputStream;

public interface InputStreamHandler {

	void handle(InputStream input);
	
	void waitFor();
	
}
