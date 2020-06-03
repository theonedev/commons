package io.onedev.commons.jsyntax.go;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class GoTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new GoTokenizer(), new String[] {"go/go.js"}, "test.go");
	}
}
