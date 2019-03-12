package io.onedev.commons.codeassist;

import java.io.Serializable;

import javax.annotation.Nullable;

import io.onedev.commons.utils.Range;

public class InputSuggestion implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final String content;
	
	private final int caret;
	
	private final String description;
	
	private final Range match;
	
	/**
	 * Construct the input suggestion.
	 * 
	 * @param content
	 * 			content of the suggestion
	 * @param caret
	 * 			caret of the suggestion
	 * @param description
	 * 			description of the suggestion
	 * @param match
	 * 			optionally specifies range of the string being matched against user input
	 */
	public InputSuggestion(String content, int caret, @Nullable String description, @Nullable Range match) {
		this.content = content;
		this.caret = caret;
		this.description = description;
		this.match = match;
	}
	
	public InputSuggestion(String content, @Nullable String description, @Nullable Range match) {
		this(content, -1, description, match);
	}
	
	public InputSuggestion(String content, @Nullable Range match) {
		this(content, null, match);
	}
	
	public InputSuggestion(String content) {
		this(content, null);
	}
	
	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public Range getMatch() {
		return match;
	}

	public String getContent() {
		return content;
	}

	public int getCaret() {
		return caret;
	}

	public InputSuggestion escape(String chars) {
		StringBuilder builder = new StringBuilder();
		int start, end;
		if (match != null) {
			start = match.getFrom();
			end = match.getTo();
		} else {
			start = end = -1;
		}
		int newStart = start, newEnd = end;
		for (int i=0; i<getContent().length(); i++) {
			char ch = getContent().charAt(i);
			if (chars.indexOf(ch) != -1) {
				builder.append("\\");
				if (start != -1 && i < start)
					newStart++;
				if (end != -1 && i < end) 
					newEnd++;
			} 
			builder.append(ch);
		}
		
		Range newMatch;
		if (newStart != -1 && newEnd != -1)
			newMatch = new Range(newStart, newEnd);
		else
			newMatch = null;
		return new InputSuggestion(builder.toString(), getCaret(), getDescription(), newMatch);
	}
	
}
