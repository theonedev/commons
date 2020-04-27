package io.onedev.commons.utils.command;

import javax.annotation.Nullable;

public class ErrorCollector extends LineConsumer {

    private static final int MAX_MESSAGE_LEN = 1024;

	private final StringBuilder message = new StringBuilder();
	
	public ErrorCollector(String encoding) {
		super(encoding);
	}
	
	@Override
	public void consume(String line) {
		if (message.length() < MAX_MESSAGE_LEN) {
			if (message.length() != 0) 
				message.append("\n");
			message.append(line);
			if (message.length() >= MAX_MESSAGE_LEN)
				message.append("\n...");
		} 
	}

	@Nullable
	public String getMessage() {
		if (message.length() != 0)
			return message.toString();
		else
			return null;
	}
	
}
