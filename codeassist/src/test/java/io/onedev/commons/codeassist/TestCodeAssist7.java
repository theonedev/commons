package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.onedev.commons.codeassist.CodeAssist;
import io.onedev.commons.codeassist.InputStatus;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest7Lexer;

/**
 * This test case uses MySQL select grammar.
 * 
 * @author robin
 *
 */
public class TestCodeAssist7 {

	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest7Lexer.class, false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			return null;
		}

	};

	@Test
	public void test()	{
		List<? extends InputStatus> suggestions;

		suggestions = codeAssist.suggest(new InputStatus("created-by-me"), "query");
		assertEquals(3, suggestions.size());
		assertEquals("created-by-meorder-by:21", suggestions.get(0).toString());
		assertEquals("created-by-meand:16", suggestions.get(1).toString());
		assertEquals("created-by-meor:15", suggestions.get(2).toString());
		
		suggestions = codeAssist.suggest(new InputStatus(""), "query2");
		assertEquals(3, suggestions.size());
		assertEquals("created:7", suggestions.get(0).toString());
		assertEquals("created by:10", suggestions.get(1).toString());
		assertEquals("created by me:13", suggestions.get(2).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("created "), "query2");
		assertEquals(2, suggestions.size());
		assertEquals("created by:10", suggestions.get(0).toString());
		assertEquals("created by me:13", suggestions.get(1).toString());

		suggestions = codeAssist.suggest(new InputStatus("issues"), "query3");
		assertEquals(1, suggestions.size());
		assertEquals("issues created by me:20", suggestions.get(0).toString());
	}
	
}
