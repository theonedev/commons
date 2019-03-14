package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.onedev.commons.codeassist.CodeAssist;
import io.onedev.commons.codeassist.InputStatus;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest6Lexer;

/**
 * This test case uses MySQL select grammar.
 * 
 * @author robin
 *
 */
public class TestCodeAssist6 {

	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest6Lexer.class, false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			return null;
		}

	};

	@Test
	public void test()	{
		List<? extends InputStatus> suggestions;
		
		suggestions = codeAssist.suggest(new InputStatus("ab"), "rule2");
		assertEquals(2, suggestions.size());
		assertEquals("ab cd:5", suggestions.get(0).toString());
		assertEquals("ab :3", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus(""), "rule1");
		assertEquals(1, suggestions.size());
		assertEquals("ab cd:5", suggestions.get(0).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("is"), "rule3");
		assertEquals(2, suggestions.size());
		assertEquals("is :3", suggestions.get(0).toString());
		assertEquals("is not :7", suggestions.get(1).toString());
	}
	
}
