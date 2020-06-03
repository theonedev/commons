package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest9Lexer;

/**
 * This test case uses MySQL select grammar.
 * 
 * @author robin
 *
 */
public class TestCodeAssist9 {

	private CodeAssist newCodeAssist(boolean findAllPaths) {
		return new CodeAssist(CodeAssistTest9Lexer.class, findAllPaths) {

			private static final long serialVersionUID = 1L;
	
			@Override
			protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
				if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
					LexerRuleRefElementSpec elementSpec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
					if (elementSpec.getRuleName().equals("Quoted")) {
						return new FenceAware(getGrammar(), '"', '"') {
							
							@Override
							protected List<InputSuggestion> match(String unfencedMatchWith) {
								List<InputSuggestion> suggestions = new ArrayList<>();
								suggestions.add(new InputSuggestion("name"));
								return suggestions;
							}
							
						}.suggest(terminalExpect);
					}
				}
				return null;
			}
		};
	}

	@Test
	public void test()	{
		List<? extends InputStatus> suggestions;
		
		suggestions = newCodeAssist(false).suggest(new InputStatus(" "), "query");
		assertEquals(1, suggestions.size());
		assertEquals(" \"name\" is \":12", suggestions.get(0).toString());
	}
	
}
