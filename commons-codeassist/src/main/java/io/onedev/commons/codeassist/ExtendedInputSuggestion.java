package io.onedev.commons.codeassist;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.utils.LinearRange;

class ExtendedInputSuggestion extends InputSuggestion {

	private static final long serialVersionUID = 1L;
	
	private final LinearRange replaceRange;
	
	private final String mandatories;

	public ExtendedInputSuggestion(LinearRange replaceRange, String replaceContent, int caret, 
			@Nullable String description, @Nullable LinearRange match, String mandatories) {
		super(replaceContent, caret, description, match);
		this.replaceRange = replaceRange;
		this.mandatories = mandatories;
	}

	public LinearRange getReplaceRange() {
		return replaceRange;
	}

	public String getMandatories() {
		return mandatories;
	}
	
}