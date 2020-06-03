package io.onedev.commons.codeassist;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.onedev.commons.utils.LinearRange;

public class InputSuggestionTest {

	@Test
	public void test() {
		InputSuggestion suggestion = new InputSuggestion("hello world", new LinearRange(0, 5));
		suggestion = suggestion.escape("");
		assertEquals("hello world", suggestion.getContent());
		assertEquals("0-5", suggestion.getMatch().toString());
		
		suggestion = new InputSuggestion("'hello'world", new LinearRange(0, 12));
		suggestion = suggestion.escape("'");
		assertEquals("\\'hello\\'world", suggestion.getContent());
		assertEquals("0-14", suggestion.getMatch().toString());
		
		suggestion = new InputSuggestion("'hello'wor'ld", new LinearRange(3, 12));
		suggestion = suggestion.escape("'");
		assertEquals("\\'hello\\'wor\\'ld", suggestion.getContent());
		assertEquals("4-15", suggestion.getMatch().toString());
	}

}
