package io.onedev.commons.jsyntax.r;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class RTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new RTokenizer(), new String[] {"r/r.js"}, "test.r");
	}
}
