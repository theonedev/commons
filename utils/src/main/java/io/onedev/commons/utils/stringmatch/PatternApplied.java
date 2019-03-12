package io.onedev.commons.utils.stringmatch;

import java.io.Serializable;

import io.onedev.commons.utils.Range;

public class PatternApplied implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String text;
	
	private final Range match;
	
	public PatternApplied(String text, Range match) {
		this.text = text;
		this.match = match;
	}

	public String getText() {
		return text;
	}

	public Range getMatch() {
		return match;
	}

	@Override
	public String toString() {
		return text + ":" + match;
	}
}
