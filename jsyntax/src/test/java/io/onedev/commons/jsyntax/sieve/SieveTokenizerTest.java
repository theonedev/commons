package io.onedev.commons.jsyntax.sieve;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.sieve.SieveTokenizer;


public class SieveTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		//verify(new SieveTokenizer(), new String[] {"sieve/sieve.js"}, "test.sieve");
		verify(new SieveTokenizer(), new String[] {"sieve/sieve.js"}, "test2.sieve");
	}
}
