package io.onedev.commons.jsyntax.javascript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.javascript.JSONLDTokenizer;

public class JSONLDTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new JSONLDTokenizer(), new String[]{"javascript/javascript.js"}, "test.jsonld");
	}

}
