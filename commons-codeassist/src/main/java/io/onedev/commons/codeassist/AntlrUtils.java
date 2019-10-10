package io.onedev.commons.codeassist;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;

import io.onedev.commons.utils.WordUtils;

public class AntlrUtils {

	public static String getDefaultGrammarFile(Class<?> lexerClass) {
		String lexerName = lexerClass.getName().replace(".", "/");
		return lexerName.substring(0, lexerName.length() - "Lexer".length()) + ".g4";
	}
	
	public static String getDefaultTokenFile(Class<?> lexerClass) {
		return lexerClass.getSimpleName() + ".tokens";
	}
		
	@SuppressWarnings("unchecked")
	public static Class<? extends Lexer> getLexerClass(Class<? extends Parser> parserClass) {
		String parserClassName = parserClass.getName();
		String lexerClassName = parserClassName.substring(0, parserClassName.length()-"Parser".length()) + "Lexer";
		try {
			return (Class<? extends Lexer>) Class.forName(lexerClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getLexerRuleName(String[] lexerRuleNames, int rule) {
		return WordUtils.uncamel(lexerRuleNames[rule-1]).toLowerCase();
	}
	
	public static int getLexerRule(String[] lexerRuleNames, String lexerRuleName) {
		for (int i=0; i<lexerRuleNames.length; i++) {
			String each = lexerRuleNames[i];
			if (WordUtils.uncamel(each).toLowerCase().equals(lexerRuleName))
				return i+1;
		}
		throw new RuntimeException("Unable to find lexer rule: " + lexerRuleName);
	}

}
