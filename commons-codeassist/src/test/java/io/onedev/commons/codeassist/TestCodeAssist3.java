package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.ParseExpect;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest3Lexer;

public class TestCodeAssist3 {

	private static final String[] AUTHORS = new String[]{"robin shen", "steve luo", "justin"};

	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest3Lexer.class, false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
				LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
				if (spec.getRuleName().equals("QuotedValue")) {
					return new FenceAware(codeAssist.getGrammar(), '"', '"') {

						@Override
						protected List<InputSuggestion> match(String matchWith) {
							ParseExpect criteriaNode = terminalExpect.findExpectByRule("criteria");
							if (criteriaNode.getState().getFirstMatchedToken().getText().equals("author")) {
								List<InputSuggestion> suggestions = new ArrayList<>();
								for (String value: AUTHORS) {
									if (value.toLowerCase().startsWith(matchWith.toLowerCase()) && value.contains(" "))
										suggestions.add(new InputSuggestion(value));
								}
								return suggestions;
							} else {
								return null;
							}
						}
						
					}.suggest(terminalExpect);
				} else if (spec.getRuleName().equals("NQuotedValue")) {
					ParseExpect criteriaNode = terminalExpect.findExpectByRule("criteria");
					if (criteriaNode.getState().getFirstMatchedToken().getText().equals("author")) {
						List<InputSuggestion> suggestions = new ArrayList<>();
						for (String value: AUTHORS) {
							if (value.toLowerCase().startsWith(terminalExpect.getUnmatchedText()) && !value.contains(" "))
								suggestions.add(new InputSuggestion(value));
						}
						return suggestions;
					} else {
						return null;
					}
				}
			}
			return null;
		}

	};
	
	@Test
	public void test() {
		List<? extends InputStatus> suggestions;
		
		suggestions = codeAssist.suggest(new InputStatus("title: hello "), "query");
		assertEquals(3, suggestions.size());
		assertEquals("title: hello title::19", suggestions.get(0).toString());
		assertEquals("title: hello author::20", suggestions.get(1).toString());
		assertEquals("title: \"hello\":14", suggestions.get(2).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("title: hello author:"), "query");
		assertEquals(4, suggestions.size());
		assertEquals("title: hello author::20", suggestions.get(0).toString());
		assertEquals("title: hello author:\"robin shen\":32", suggestions.get(1).toString());
		assertEquals("title: hello author:\"steve luo\":31", suggestions.get(2).toString());
		assertEquals("title: hello author:justin:26", suggestions.get(3).toString());

		suggestions = codeAssist.suggest(new InputStatus("title: hello world"), "query");
		assertEquals(1, suggestions.size());
		assertEquals("title: \"hello world\":20", suggestions.get(0).toString());
		
		suggestions = codeAssist.suggest(new InputStatus(""), "query");
		assertEquals(2, suggestions.size());
		assertEquals("title::6", suggestions.get(0).toString());
		assertEquals("author::7", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("title:"), "query");
		assertEquals(2, suggestions.size());
		assertEquals("title::6", suggestions.get(0).toString());
		assertEquals("title:\":7", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("author: dustin"), "query");
		assertEquals(1, suggestions.size());
		assertEquals("author: dustin :15", suggestions.get(0).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("author: dustin "), "query");
		assertEquals(2, suggestions.size());
		assertEquals("author: dustin title::21", suggestions.get(0).toString());
		assertEquals("author: dustin author::22", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("author: \"robin shen\""), "query");
		assertEquals(2, suggestions.size());
		assertEquals("author: \"robin shen\"title::26", suggestions.get(0).toString());
		assertEquals("author: \"robin shen\"author::27", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("title: hello"), "query");
		assertEquals(2, suggestions.size());
		assertEquals("title: hello :13", suggestions.get(0).toString());
		assertEquals("title: \"hello\":14", suggestions.get(1).toString());
	}
	
}
