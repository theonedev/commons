package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.CodeAssist;
import io.onedev.commons.codeassist.InputStatus;
import io.onedev.commons.codeassist.InputSuggestion;
import io.onedev.commons.codeassist.parser.Element;
import io.onedev.commons.codeassist.parser.ParseExpect;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.codeassist.test.CodeAssistTest5Lexer;

/**
 * This test case uses MySQL select grammar.
 * 
 * @author robin
 *
 */
public class TestCodeAssist5 {

	private static final Map<String, List<String>> SCHEMA_TABLES = new LinkedHashMap<>();
	
	private static final Map<String, List<String>> TABLE_COLUMNS = new LinkedHashMap<>();

	private static final String DEFAULT_SCHEMA = "schemaA"; 
	
	static {
		SCHEMA_TABLES.put("schemaA", Lists.newArrayList("tableA", "tableB"));
		SCHEMA_TABLES.put("schemaB", Lists.newArrayList("tableC", "tableD"));
		TABLE_COLUMNS.put("tableA", Lists.newArrayList("columnA", "columnB"));
		TABLE_COLUMNS.put("tableB", Lists.newArrayList("columnC", "columnD"));
		TABLE_COLUMNS.put("tableC", Lists.newArrayList("columnE", "columnF"));
		TABLE_COLUMNS.put("tableD", Lists.newArrayList("columnG", "columnH"));
	}
	
	private CodeAssist codeAssist = new CodeAssist(CodeAssistTest5Lexer.class, 
			new String[]{
					"io/onedev/commons/codeassist/test/CodeAssistTest5Lexer.g4", 
					"io/onedev/commons/codeassist/test/CodeAssistTest5Parser.g4"}, 
			"CodeAssistTest5Parser.tokens", false) {

		private static final long serialVersionUID = 1L;

		@Override
		protected List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
			ParseExpect parseExpect = terminalExpect.findExpectByRule("schema_name");
			if (parseExpect != null) {
				List<InputSuggestion> suggestions = new ArrayList<>();
				for (String schemaName: SCHEMA_TABLES.keySet()) {
					if (schemaName.startsWith(terminalExpect.getUnmatchedText()))
						suggestions.add(new InputSuggestion(schemaName));
				}
				return suggestions;
			} 
			parseExpect = terminalExpect.findExpectByLabel("tableName");
			if (parseExpect != null) {
				List<InputSuggestion> suggestions = new ArrayList<>();
				String schemaName;
				List<Element> schemaNameElements = parseExpect.getState().findMatchedElementsByRule("schema_name", true);
				if (!schemaNameElements.isEmpty())
					schemaName = schemaNameElements.get(0).getMatchedText();
				else
					schemaName = DEFAULT_SCHEMA;
				if (SCHEMA_TABLES.containsKey(schemaName)) {
					for (String tableName: SCHEMA_TABLES.get(schemaName)) {
						if (tableName.startsWith(terminalExpect.getUnmatchedText()))
							suggestions.add(new InputSuggestion(tableName));
					}
				}
				return suggestions;
			}
			parseExpect = terminalExpect.findExpectByLabel("columnName");
			if (parseExpect != null) {
				List<InputSuggestion> suggestions = new ArrayList<>();
				List<Element> tableNameElements = parseExpect.getState().findMatchedElementsByLabel("tableName", true);
				if (!tableNameElements.isEmpty()) {
					String tableName = tableNameElements.get(0).getMatchedText();
					if (TABLE_COLUMNS.containsKey(tableName)) {
						for (String columnName: TABLE_COLUMNS.get(tableName)) {
							if (columnName.startsWith(terminalExpect.getUnmatchedText()))
								suggestions.add(new InputSuggestion(columnName));
						}
					}
				}
				return suggestions;
			}
			return null;
		}

		@Override
		protected Optional<String> describe(TerminalExpect terminalExpect, String literal) {
			if (literal.length() > 1)
				return super.describe(terminalExpect, literal);
			else
				return null;
		}

	};
	
	@Test
	public void test() {
		List<? extends InputStatus> suggestions;
		
		suggestions = codeAssist.suggest(new InputStatus("select schemaB."), "stat");
		assertEquals(2, suggestions.size());
		assertEquals("select schemaB.tableC.:22", suggestions.get(0).toString());
		assertEquals("select schemaB.tableD.:22", suggestions.get(1).toString());

		suggestions = codeAssist.suggest(new InputStatus("select tableC."), "stat");
		assertEquals(2, suggestions.size());
		assertEquals("select tableC.columnE:21", suggestions.get(0).toString());
		assertEquals("select tableC.columnF:21", suggestions.get(1).toString());
		
		suggestions = codeAssist.suggest(new InputStatus("select schema"), "stat");
		assertEquals(5, suggestions.size());
		assertEquals("select schema select:20", suggestions.get(0).toString());
		assertEquals("select schema from:18", suggestions.get(1).toString());
		assertEquals("select schema where:19", suggestions.get(2).toString());
		assertEquals("select schemaA.:15", suggestions.get(3).toString());
		assertEquals("select schemaB.:15", suggestions.get(4).toString());

		suggestions = codeAssist.suggest(new InputStatus("select table"), "stat");
		assertEquals(5, suggestions.size());
		assertEquals("select table select:19", suggestions.get(0).toString());
		assertEquals("select table from:17", suggestions.get(1).toString());
		assertEquals("select table where:18", suggestions.get(2).toString());
		assertEquals("select tableA.:14", suggestions.get(3).toString());
		assertEquals("select tableB.:14", suggestions.get(4).toString());
	}
	
}
