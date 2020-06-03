package io.onedev.commons.jsyntax.haskell;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class HaskellTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new HaskellTokenizer(), new String[] {"haskell/haskell.js"}, "test.hs");
		verify(new HaskellTokenizer(), new String[] {"haskell/haskell.js"}, "test2.hs");
	}
}
