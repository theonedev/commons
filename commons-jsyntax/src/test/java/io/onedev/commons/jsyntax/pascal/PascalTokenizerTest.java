package io.onedev.commons.jsyntax.pascal;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PascalTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new PascalTokenizer(), new String[] {"pascal/pascal.js"}, "test.pas");
	}
}
