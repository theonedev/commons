package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.CTokenizer;

public class CTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new CTokenizer(), new String[]{"clike/clike.js"}, "test.c");
	}

}
