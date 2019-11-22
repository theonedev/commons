package io.onedev.commons.utils;

import java.io.Serializable;

import javax.annotation.Nullable;

public class LinearRange implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int from;
	
	private final int to;
	
	public LinearRange(int from, int to) {
		this.from = from;
		this.to = to;
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}
	
	public int getLength() {
		return to - from;
	}

	@Override
	public String toString() {
		return from + "-" + to;
	}
	
	@Nullable
	public static LinearRange match(String text, String matchWith) {
		String normalizedText = text;
		String normalizedMatchWith = matchWith;
		normalizedText = text.toLowerCase();
		normalizedMatchWith = matchWith.toLowerCase();
		
		int start = normalizedText.indexOf(normalizedMatchWith);
		int end = start + normalizedMatchWith.length();
		if (start != -1) 
			return new LinearRange(start, end);
		else 
			return null;
	}
}
