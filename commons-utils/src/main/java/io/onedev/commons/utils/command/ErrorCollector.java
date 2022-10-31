package io.onedev.commons.utils.command;

import javax.annotation.Nullable;

import io.onedev.commons.bootstrap.SensitiveMasker;

public class ErrorCollector extends LineConsumer {

    private static final int MAX_MESSAGE_LEN = 1024;

	private final StringBuilder message = new StringBuilder();
	
	public ErrorCollector(String encoding) {
		super(encoding);
	}
	
	@Override
	public void consume(String line) {
		SensitiveMasker masker = SensitiveMasker.get();
		if (masker != null)
			line = masker.mask(line);
		
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
