package io.onedev.commons.jsyntax.css;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class SCSSTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SCSSTokenizer(), new String[]{"css/css.js"}, "test.scss");
	}

}
