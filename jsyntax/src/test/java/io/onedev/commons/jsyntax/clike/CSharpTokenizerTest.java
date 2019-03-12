package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.CSharpTokenizer;

public class CSharpTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new CSharpTokenizer(), new String[]{"clike/clike.js"}, "test.cs");
	}

}
