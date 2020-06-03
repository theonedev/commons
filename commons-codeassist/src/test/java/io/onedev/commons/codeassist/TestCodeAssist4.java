package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest4Lexer;

public class TestCodeAssist4 {

	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest4Lexer.class, false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			return null;
		}

	};
	
	@Test
	public void test() {
		List<? extends InputStatus> suggestions;

		suggestions = codeAssist.suggest(new InputStatus("a"), "stat");
		assertEquals(6, suggestions.size());
		assertEquals("a;:2", suggestions.get(0).toString());
		assertEquals("a=:2", suggestions.get(1).toString());
		assertEquals("a+:2", suggestions.get(2).toString());
		assertEquals("a-:2", suggestions.get(3).toString());
		assertEquals("a*:2", suggestions.get(4).toString());
		assertEquals("a/:2", suggestions.get(5).toString());

		suggestions = codeAssist.suggest(new InputStatus(""), "stat");
		assertEquals(2, suggestions.size());
		assertEquals(";:1", suggestions.get(0).toString());
		assertEquals("(:1", suggestions.get(1).toString());

		suggestions = codeAssist.suggest(new InputStatus("5*"), "stat");
		assertEquals(2, suggestions.size());
		assertEquals("5*:2", suggestions.get(0).toString());
		assertEquals("5*(:3", suggestions.get(1).toString());

		suggestions = codeAssist.suggest(new InputStatus("5"), "stat");
		assertEquals(5, suggestions.size());
		assertEquals("5;:2", suggestions.get(0).toString());
		assertEquals("5+:2", suggestions.get(1).toString());
		assertEquals("5-:2", suggestions.get(2).toString());
		assertEquals("5*:2", suggestions.get(3).toString());
		assertEquals("5/:2", suggestions.get(4).toString());
	}
	
}
