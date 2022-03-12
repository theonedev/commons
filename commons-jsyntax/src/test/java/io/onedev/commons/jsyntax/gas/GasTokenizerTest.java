package io.onedev.commons.jsyntax.gas;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class GasTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new GasTokenizer(), new String[] {"gas/gas.js"}, "test.s");
	}
	
}
