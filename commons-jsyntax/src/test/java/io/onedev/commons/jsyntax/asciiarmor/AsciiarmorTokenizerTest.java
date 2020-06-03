package io.onedev.commons.jsyntax.asciiarmor;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class AsciiarmorTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new AsciiarmorTokenizer(), new String[] {"asciiarmor/asciiarmor.js"}, "test.pgp");
	}
}