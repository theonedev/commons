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
import io.onedev.commons.codeassist.test.CodeAssistTest8Lexer;

/**
 * This test case uses MySQL select grammar.
 * 
 * @author robin
 *
 */
public class TestCodeAssist8 {

	private CodeAssist newCodeAssist(boolean findAllPaths) {
		return new CodeAssist(CodeAssistTest8Lexer.class, findAllPaths) {

			private static final long serialVersionUID = 1L;
	
			@Override
			protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
				if (terminalExpect.getElementSpec() instanceof LexerRuleRefElementSpec) {
					LexerRuleRefElementSpec elementSpec = (LexerRuleRefElementSpec) terminalExpect.getElementSpec();
					if (elementSpec.getRuleName().equals("Value")) {
						return new FenceAware(getGrammar(), "{{", "}}") {
							
							@Override
							protected List<InputSuggestion> match(String unfencedMatchWith) {
								List<InputSuggestion> suggestions = new ArrayList<>();
								if ("file1\\}".startsWith(unfencedMatchWith)) 
									suggestions.add(new InputSuggestion("file1\\}"));
								if ("dir1/".startsWith(unfencedMatchWith)) 
									suggestions.add(new InputSuggestion("dir1/", 5, null, null));
								if (!suggestions.isEmpty())
									return suggestions;
								else
									return null;
							}
							
						}.suggest(terminalExpect);
					} else if (elementSpec.getRuleName().equals("Identifier")) {
						if (terminalExpect.findExpectByLabel("tableName") != null) {
							List<InputSuggestion> suggestions = new ArrayList<>();
							suggestions.add(new InputSuggestion("table1"));
							suggestions.add(new InputSuggestion("table2"));
							return suggestions;
						} else if (terminalExpect.findExpectByLabel("columnName") != null) {
							List<InputSuggestion> suggestions = new ArrayList<>();
							suggestions.add(new InputSuggestion("column1"));
							suggestions.add(new InputSuggestion("column2"));
							return suggestions;
						}
					}
				}
				return null;
			}
		};
	}

	@Test
	public void test()	{
		List<? extends InputStatus> suggestions;
		
		suggestions = newCodeAssist(false).suggest(new InputStatus(""), "column");
		assertEquals(2, suggestions.size());
		assertEquals("table1.:7", suggestions.get(0).toString());
		assertEquals("table2.:7", suggestions.get(1).toString());
		
		suggestions = newCodeAssist(true).suggest(new InputStatus(""), "column");
		assertEquals(4, suggestions.size());
		assertEquals("table1.:7", suggestions.get(0).toString());
		assertEquals("table2.:7", suggestions.get(1).toString());
		assertEquals("column1:7", suggestions.get(2).toString());
		assertEquals("column2:7", suggestions.get(3).toString());
		
		suggestions = newCodeAssist(false).suggest(new InputStatus("name is {{}}", 10), "query");
		assertEquals(2, suggestions.size());
		assertEquals("name is {{file1\\}}}:19", suggestions.get(0).toString());
		assertEquals("name is {{dir1/}}:15", suggestions.get(1).toString());
		
		suggestions = newCodeAssist(false).suggest(new InputStatus("name is {{file1\\}}}", 10), "query");
		assertEquals(2, suggestions.size());
		assertEquals("name is {{file1\\}}}:19", suggestions.get(0).toString());
		assertEquals("name is {{dir1/}}:15", suggestions.get(1).toString());
		
		suggestions = newCodeAssist(false).suggest(new InputStatus("name is file2", 13), "query");
		assertEquals(1, suggestions.size());
		assertEquals("name is {{file2}}:17", suggestions.get(0).toString());
				
		suggestions = newCodeAssist(false).suggest(new InputStatus("name is file with space", 23), "query");
		assertEquals(1, suggestions.size());
		assertEquals("name is {{file with space}}:27", suggestions.get(0).toString());
		
		suggestions = newCodeAssist(false).suggest(new InputStatus("name is {{file name", 19), "query");
		assertEquals(1, suggestions.size());
		assertEquals("name is {{file name}}:21", suggestions.get(0).toString());
		
		suggestions = newCodeAssist(false).suggest(new InputStatus("name is {{file name\\}\\}", 23), "query");
		assertEquals(1, suggestions.size());
		assertEquals("name is {{file name\\}\\}}}:25", suggestions.get(0).toString());
		
		suggestions = newCodeAssist(false).suggest(new InputStatus("name is {{file name\\}\\}}}", 25), "query");
		assertEquals(0, suggestions.size());
	}
	
}
