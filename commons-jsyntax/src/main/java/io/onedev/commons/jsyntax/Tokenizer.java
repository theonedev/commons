package io.onedev.commons.jsyntax;

import java.util.List;

public interface Tokenizer {
	
	/**
	 * Tokenize specified lines into syntax tokens
	 * 
	 * @param lines
	 * 			lines to be tokenized
	 */
	List<Tokenized> tokenize(List<String> lines);
	
	boolean accept(String fileName);
	
	boolean acceptMime(String mime);
	
	boolean acceptMode(String mode);
}
