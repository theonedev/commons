package io.onedev.commons.jsyntax.stylus;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class StylusTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new  StylusTokenizer(), new String[] {"stylus/stylus.js"}, "test.styl");
		verify(new  StylusTokenizer(), new String[] {"stylus/stylus.js"}, "test2.styl");
	}

}
