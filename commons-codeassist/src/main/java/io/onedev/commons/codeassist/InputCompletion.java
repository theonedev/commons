package io.onedev.commons.codeassist;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.utils.LinearRange;

public class InputCompletion extends InputStatus {

	private static final long serialVersionUID = 1L;

	private final String label;
	
	private final String description;
	
	private final LinearRange match;
	
	public InputCompletion(String label, String content, int caret,
			@Nullable String description, @Nullable LinearRange match) {
		super(content, caret);
		this.label = label;
		this.description = description;
		this.match = match;
	}
	
	public String getLabel() {
		return label;
	}

	@Nullable
	public String getDescription() {
		return description;
	}
	
	@Nullable
	public LinearRange getMatch() {
		return match;
	}

}
