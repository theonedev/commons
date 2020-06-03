package io.onedev.commons.jsyntax.factor;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class FactorTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new FactorTokenizer(), new String[] {"factor/factor.js"}, "test.factor");
	}
}
