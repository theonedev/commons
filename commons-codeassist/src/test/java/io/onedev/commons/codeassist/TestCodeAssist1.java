package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest1Lexer;

public class TestCodeAssist1 {

	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest1Lexer.class, false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			return null;
		}

	};
	
	@Test
	public void test()	{
		List<? extends InputStatus> suggestions;

		suggestions = codeAssist.suggest(new InputStatus(""), "selfReference");
		assertEquals(1, suggestions.size());
		assertEquals("ab:2", suggestions.get(0).toString());

		suggestions = codeAssist.suggest(new InputStatus("ab "), "selfReference");
		assertEquals(1, suggestions.size());
		
		suggestions = codeAssist.suggest(new InputStatus("ab"), "selfReference");
		assertEquals(2, suggestions.size());
		
		suggestions = codeAssist.suggest(new InputStatus(""), "mandatories");
		assertEquals(2, suggestions.size());
		assertEquals("ab c:4", suggestions.get(0).toString());
		assertEquals("cd ef g h:9", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("cd "), "mandatories");
		assertEquals(1, suggestions.size());
		assertEquals("cd ef g h:9", suggestions.get(0).toString());
	}

}
