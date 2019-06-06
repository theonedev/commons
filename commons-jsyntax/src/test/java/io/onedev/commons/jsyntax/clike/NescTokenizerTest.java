package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.NescTokenizer;

public class NescTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new NescTokenizer(), new String[]{"clike/clike.js"}, "test.nesc", "text/x-nesc");
	}

}
