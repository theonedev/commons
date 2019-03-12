package io.onedev.commons.codeassist;

import javax.annotation.Nullable;

import io.onedev.commons.utils.Range;

class ExtendedInputSuggestion extends InputSuggestion {

	private static final long serialVersionUID = 1L;
	
	private final Range replaceRange;
	
	private final String mandatories;

	public ExtendedInputSuggestion(Range replaceRange, String replaceContent, int caret, 
			@Nullable String description, @Nullable Range match, String mandatories) {
		super(replaceContent, caret, description, match);
		this.replaceRange = replaceRange;
		this.mandatories = mandatories;
	}

	public Range getReplaceRange() {
		return replaceRange;
	}

	public String getMandatories() {
		return mandatories;
	}
	
}