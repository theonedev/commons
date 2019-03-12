package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.CppTokenizer;

public class CppTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new CppTokenizer(), new String[]{"clike/clike.js"}, "test.cpp");
	}

}
