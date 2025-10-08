package io.onedev.commons.utils;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
	public boolean equals(Object other) {
		if (!(other instanceof LinearRange))
			return false;
		if (this == other)
			return true;
		LinearRange otherRange = (LinearRange) other;
		return new EqualsBuilder()
				.append(from, otherRange.from)
				.append(to, otherRange.to)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(from)
				.append(to)
				.toHashCode();
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
