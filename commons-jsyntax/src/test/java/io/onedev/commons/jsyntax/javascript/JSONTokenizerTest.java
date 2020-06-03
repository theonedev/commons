package io.onedev.commons.jsyntax.javascript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class JSONTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new JSONTokenizer(), new String[]{"javascript/javascript.js"}, "test.json");
	}

}
