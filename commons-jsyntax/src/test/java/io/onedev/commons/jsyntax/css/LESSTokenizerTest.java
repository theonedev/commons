package io.onedev.commons.jsyntax.css;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.css.LESSTokenizer;

public class LESSTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new LESSTokenizer(), new String[]{"css/css.js"}, "test.less");
	}

}
