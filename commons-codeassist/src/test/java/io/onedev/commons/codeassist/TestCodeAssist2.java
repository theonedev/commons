package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.codeassist.CodeAssist;
import io.onedev.commons.codeassist.FenceAware;
import io.onedev.commons.codeassist.InputStatus;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest2Lexer;

public class TestCodeAssist2 {

	private static final String[] BRANCHS = new String[]{"master", "dev", "feature1", "feature2"};

	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest2Lexer.class, false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
				LexerRuleRefElementSpec spec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
				if (spec.getRuleName().equals("Value")) {
					return new FenceAware(codeAssist.getGrammar(), '(', ')') {

						@Override
						protected List<InputSuggestion> match(String matchWith) {
							if (terminalExpect.getState().getLastMatchedToken().getType() == CodeAssistTest2Lexer.BRANCH) {
								List<InputSuggestion> suggestions = new ArrayList<>();
								for (String value: BRANCHS) {
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

		suggestions = codeAssist.suggest(new InputStatus("branch(feature)"), "query");
		assertEquals(12, suggestions.size());
		assertEquals("branch(feature)branch(:22", suggestions.get(0).toString());
		assertEquals("branch(feature)tag(:19", suggestions.get(1).toString());
		assertEquals("branch(feature)id(:18", suggestions.get(2).toString());
		assertEquals("branch(feature)before(:22", suggestions.get(3).toString());
		assertEquals("branch(feature)after(:21", suggestions.get(4).toString());
		assertEquals("branch(feature)message(:23", suggestions.get(5).toString());
		assertEquals("branch(feature)committer(:25", suggestions.get(6).toString());
		assertEquals("branch(feature)author(:22", suggestions.get(7).toString());
		assertEquals("branch(feature)path(:20", suggestions.get(8).toString());
		assertEquals("branch(feature)^:16", suggestions.get(9).toString());
		assertEquals("branch(feature)..:17", suggestions.get(10).toString());
		assertEquals("branch(feature)...:18", suggestions.get(11).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("branch"), "query");
		assertEquals(5, suggestions.size());
		assertEquals("branch(:7", suggestions.get(0).toString());
		assertEquals("branch(master):14", suggestions.get(1).toString());
		assertEquals("branch(dev):11", suggestions.get(2).toString());
		assertEquals("branch(feature1):16", suggestions.get(3).toString());
		assertEquals("branch(feature2):16", suggestions.get(4).toString());

		suggestions = codeAssist.suggest(new InputStatus("message"), "query");
		assertEquals(1, suggestions.size());
		assertEquals("message(:8", suggestions.get(0).toString());
		
		suggestions = codeAssist.suggest(new InputStatus(""), "revisionCriteria");
		assertEquals(4, suggestions.size());
		assertEquals("branch(:7", suggestions.get(0).toString());
		assertEquals("tag(:4", suggestions.get(1).toString());
		assertEquals("id(:3", suggestions.get(2).toString());
		assertEquals("^:1", suggestions.get(3).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("branch(master)"), "revisionCriteria");
		assertEquals(2, suggestions.size());
		assertEquals("branch(master)..:16", suggestions.get(0).toString());
		assertEquals("branch(master)...:17", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("br"), "query");
		assertEquals(1, suggestions.size());
		assertEquals("branch(:7", suggestions.get(0).toString());

		suggestions = codeAssist.suggest(new InputStatus("branch("), "query");
		assertEquals(4, suggestions.size());
		assertEquals("branch(master):14", suggestions.get(0).toString());
		assertEquals("branch(dev):11", suggestions.get(1).toString());
		assertEquals("branch(feature1):16", suggestions.get(2).toString());
		assertEquals("branch(feature2):16", suggestions.get(3).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("branch( fea"), "query");
		assertEquals(2, suggestions.size());
		assertEquals("branch(feature1):16", suggestions.get(0).toString());
		assertEquals("branch(feature2):16", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("tag"), "query");
		assertEquals(1, suggestions.size());
		assertEquals("tag(:4", suggestions.get(0).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("branch(master)t"), "query");
		assertEquals(1, suggestions.size());
		assertEquals("branch(master)tag(:18", suggestions.get(0).toString());
	}
	
}
