package io.onedev.commons.jsyntax.commonlisp;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.commonlisp.CommonlispTokenizer;


public class CommonlispTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new CommonlispTokenizer(), new String[]{"commonlisp/commonlisp.js"}, "test.lisp");
	}
}
