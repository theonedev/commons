package io.onedev.commons.jsyntax.javascript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.javascript.JSONTokenizer;

public class JSONTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new JSONTokenizer(), new String[]{"javascript/javascript.js"}, "test.json");
	}

}
