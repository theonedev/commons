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

	@Override
	public String toString() {
		return from + "-" + to;
	}
	
	private static int getIndex(String text, int indexWithoutSpaces) {
		int index=0;
		int pos = 0;
		for (char ch: text.toCharArray()) {
			if (ch != ' ') {
				if (index == indexWithoutSpaces)
					return pos;
				index++;
			}
			pos++;
		}
		return pos;
	}
	
	@Nullable
	public static LinearRange match(String text, String matchWith, boolean fromStart, boolean caseSensitive, boolean ignoreSpaces) {
		String normalizedText = text;
		String normalizedMatchWith = matchWith;
		if (!caseSensitive) {
			normalizedText = normalizedText.toLowerCase();
			normalizedMatchWith = normalizedMatchWith.toLowerCase();
		}
		if (ignoreSpaces) {
			normalizedText = StringUtils.deleteWhitespace(normalizedText);
			normalizedMatchWith = StringUtils.deleteWhitespace(normalizedMatchWith);
		}
		int start = normalizedText.indexOf(normalizedMatchWith);
		int end = start + normalizedMatchWith.length();
		if (fromStart && start == 0 || !fromStart && start != -1) {
			if (ignoreSpaces) 
				return new LinearRange(getIndex(text, start), getIndex(text, end));
			else
				return new LinearRange(start, end);
		} else {
			return null;
		}
	}
}
