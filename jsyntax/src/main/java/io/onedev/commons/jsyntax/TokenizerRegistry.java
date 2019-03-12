package io.onedev.commons.jsyntax;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.reflections.Reflections;

import javassist.Modifier;

public class TokenizerRegistry {

	private static final List<Tokenizer> tokenizers;
	
	static {
		tokenizers = new ArrayList<>();
		Reflections reflections = new Reflections(TokenizerRegistry.class.getPackage().getName());
		for (Class<? extends Tokenizer> tokenizerClass: reflections.getSubTypesOf(Tokenizer.class)) {
			if (!Modifier.isAbstract(tokenizerClass.getModifiers())) {
				try {
					tokenizers.add(tokenizerClass.newInstance());
				} catch (InstantiationException | IllegalAccessException e) {
					// ignore as we may have anonymous inline tokenizer classes 
				}
			}
		}
	}
	
	/**
	 * Get tokenizer of specified file
	 * 
	 * @param fileName
	 * 			name of file to be tokenized
	 * @return
	 * 			tokenizer of specified file, or <tt>null</tt> if not found 
	 */
	@Nullable 
	public static Tokenizer getTokenizer(String fileName) {
		if (fileName != null) {
			for (Tokenizer tokenizer: tokenizers) {
				if (tokenizer.accept(fileName)) 
					return tokenizer;
			}
		}
		return null;
	}
	
	public static Tokenizer getTokenizerByMime(String mime) {
		for (Tokenizer tokenizer : tokenizers) {
			if (tokenizer.acceptMime(mime))
				return tokenizer;
		}
		return null;
	}
	
	public static Tokenizer getTokenizerByMode(String mode) {
		for (Tokenizer tokenizer : tokenizers) {
			if (tokenizer.acceptMode(mode))
				return tokenizer;
		}
		return null;
	}
	
	public static List<Tokenizer> getTokenizers() {
		return tokenizers;
	}
	
}
