package io.onedev.commons.jsyntax.css;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class GSSTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new GSSTokenizer(), new String[]{"css/css.js"}, "test.gss");
	}

}
