package io.onedev.commons.codeassist;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.onedev.commons.codeassist.grammar.Grammar;
import io.onedev.commons.codeassist.parser.EarleyParser;
import io.onedev.commons.codeassist.test.CodeAssistTest1Lexer;
import io.onedev.commons.codeassist.test.CodeAssistTest4Lexer;

public class EarleyParserTest {

	private Grammar grammar;
	
	private boolean matches(String ruleName, String text) {
		return new EarleyParser(grammar, ruleName, text).matches();
	}
	
	@Test
	public void test() {
		grammar = new Grammar(CodeAssistTest1Lexer.class);
		assertFalse(matches("notRealAmbiguity", "1"));
		assertTrue(matches("notRealAmbiguity", "1 2"));
		assertTrue(matches("notRealAmbiguity", "1 2 3"));

		grammar = new Grammar(CodeAssistTest4Lexer.class);
		assertTrue(matches("expr", "(1+2)+3"));
		assertTrue(matches("expr", "1+(2*3)"));
		assertFalse(matches("expr", "(1+2)+"));
		assertFalse(matches("expr", "1(2*3)"));
		assertFalse(matches("expr", "1/2+3)"));
	}

}
