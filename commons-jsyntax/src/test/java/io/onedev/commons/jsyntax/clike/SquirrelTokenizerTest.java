package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.SquirrelTokenizer;

public class SquirrelTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SquirrelTokenizer(), new String[]{"clike/clike.js"}, "test.nut");
	}

}
