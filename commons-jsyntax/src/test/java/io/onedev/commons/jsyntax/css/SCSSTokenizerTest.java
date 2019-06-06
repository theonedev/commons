package io.onedev.commons.jsyntax.css;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.css.SCSSTokenizer;

public class SCSSTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SCSSTokenizer(), new String[]{"css/css.js"}, "test.scss");
	}

}
