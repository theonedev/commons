package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class CSharpTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new CSharpTokenizer(), new String[]{"clike/clike.js"}, "test.cs");
	}

}
