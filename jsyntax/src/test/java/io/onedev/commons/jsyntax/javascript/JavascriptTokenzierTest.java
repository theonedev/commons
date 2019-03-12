package io.onedev.commons.jsyntax.javascript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.javascript.JavaScriptTokenizer;

public class JavascriptTokenzierTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new JavaScriptTokenizer(), new String[]{"javascript/javascript.js"}, "test.js");
		verify(new JavaScriptTokenizer(), new String[]{"javascript/javascript.js"}, "test2.js");
	}

}
