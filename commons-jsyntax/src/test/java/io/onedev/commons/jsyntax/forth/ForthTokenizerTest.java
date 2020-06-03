package io.onedev.commons.jsyntax.forth;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class ForthTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new ForthTokenizer(), new String[] {"forth/forth.js"},"test.fth");
	}
}
