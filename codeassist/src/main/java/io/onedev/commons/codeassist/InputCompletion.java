package io.onedev.commons.codeassist;

import javax.annotation.Nullable;

import io.onedev.commons.utils.Range;

public class InputCompletion extends InputStatus {

	private static final long serialVersionUID = 1L;

	private final String label;
	
	private final String description;
	
	private final Range match;
	
	public InputCompletion(String label, String content, int caret,
			@Nullable String description, @Nullable Range match) {
		super(content, caret);
		this.label = label;
		this.description = description;
		this.match = match;
	}
	
	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}
	
	public Range getMatch() {
		return match;
	}

}
