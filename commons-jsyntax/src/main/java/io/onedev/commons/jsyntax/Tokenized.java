package io.onedev.commons.jsyntax;

import java.io.Serializable;

import io.onedev.commons.jsyntax.TokenUtils;

public class Tokenized implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String text;
	
	private final long[] tokens;

	/**
	 * @param text
	 * 			text used for tokenization
	 * @param tokens
	 * 			A token array is an array of &quot;long&quot;s representing tokens parsed from 
	 * 			corresponding line. A token is represented with &quot;long&quot; to reduce memory usage when parsing
	 * 			large texts, and it is composed of below information:<br>
	 * 			
	 * 			1st byte   2nd byte   3rd byte    4th byte     5th byte     6th byte    7th byte      8th byte <br> 
	 * 			|begin pos in line|   |end pos in line   |     | token types information                     | <br>
	 * 
	 * 			Class {@link TokenUtils} provides some convenience methods to operate on the token long number 
	 */
	public Tokenized(String text, long[] tokens) {
		this.text = text;
		this.tokens = tokens;
	}

	public String getText() {
		return text;
	}

	public long[] getTokens() {
		return tokens;
	}
	
}
