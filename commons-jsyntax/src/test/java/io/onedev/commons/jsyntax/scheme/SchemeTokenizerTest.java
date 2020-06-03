package io.onedev.commons.jsyntax.scheme;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class SchemeTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new  SchemeTokenizer(), new String[] {"scheme/scheme.js"}, "test.scm");
		verify(new  SchemeTokenizer(), new String[] {"scheme/scheme.js"}, "test2.scm");
	}
}
