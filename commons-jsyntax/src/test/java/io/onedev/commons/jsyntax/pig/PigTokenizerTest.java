package io.onedev.commons.jsyntax.pig;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PigTokenizerTest extends AbstractTokenizerTest{
	@Test
	public void test() {
		verify(new PigTokenizer(), new String[] {"pig/pig.js"}, "test.pig");
	}
}

