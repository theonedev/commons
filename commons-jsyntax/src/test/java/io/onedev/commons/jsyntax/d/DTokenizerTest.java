package io.onedev.commons.jsyntax.d;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class DTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new DTokenizer(), new String[] {"d/d.js"}, "test.d");
	}

}
