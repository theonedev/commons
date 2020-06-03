package io.onedev.commons.jsyntax.css;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class CssTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new CssTokenizer(), new String[]{"css/css.js"}, "test.css");
		verify(new CssTokenizer(), new String[]{"css/css.js"}, "test2.css");
		verify(new CssTokenizer(), new String[]{"css/css.js"}, "test3.css");
	}

}
