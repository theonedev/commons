package io.onedev.commons.codeassist;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest10Lexer;

public class TestCodeAssist10 {

	private static final String[] USERS = new String[]{"robin", "tim"};

	private static final String[] GROUPS = new String[]{"developers", "testers"};
	
	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest10Lexer.class, false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
				LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
				if (spec.getRuleName().equals("Value")) {
					return new FenceAware(codeAssist.getGrammar(), "(", ")") {

						@Override
						protected List<InputSuggestion> match(String matchWith) {
							if (terminalExpect.getState().getLastMatchedToken().getType() == CodeAssistTest10Lexer.USER) {
								List<InputSuggestion> suggestions = new ArrayList<>();
								for (String value: USERS) {
									if (value.toLowerCase().contains(matchWith.toLowerCase()))
										suggestions.add(new InputSuggestion(value));
								}
								return suggestions;
							} else if (terminalExpect.getState().getLastMatchedToken().getType() == CodeAssistTest10Lexer.GROUP) {
								List<InputSuggestion> suggestions = new ArrayList<>();
								for (String value: GROUPS) {
									if (value.toLowerCase().contains(matchWith.toLowerCase()))
										suggestions.add(new InputSuggestion(value));
								}
								return suggestions;
							} else {
								return null;
							}
						}
						
					}.suggest(terminalExpect);
				}
			}
			return null;
		}

	};
	
	@Test
	public void test() {
		List<? extends InputStatus> suggestions;

		suggestions = codeAssist.suggest(new InputStatus("code writ", 6), "userMatcher");
		assertEquals(1, suggestions.size());
		assertEquals("code writersrit:12", suggestions.get(0).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("user(robin) or code writers", 5), "userMatcher");
		assertEquals(2, suggestions.size());
		assertEquals("user(robin) or code writers:11", suggestions.get(0).toString());
		assertEquals("user(tim) or code writers:9", suggestions.get(1).toString());
				
		suggestions = codeAssist.suggest(new InputStatus("code writers", 4), "userMatcher");
		assertEquals(2, suggestions.size());
		assertEquals("code writers:12", suggestions.get(0).toString());
		assertEquals("code readers:12", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus(" code writers", 0), "userMatcher");
		assertTrue(!suggestions.isEmpty());
		assertEquals("anyone code writers:6", suggestions.get(0).toString());
	}
	
}
