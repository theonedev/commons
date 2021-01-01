package io.onedev.commons.utils;

public class ExplicitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExplicitException(String message) {
		super(message);
	}
	
	public ExplicitException(String message, Throwable cause) {
		super(message, cause);
	}

}
