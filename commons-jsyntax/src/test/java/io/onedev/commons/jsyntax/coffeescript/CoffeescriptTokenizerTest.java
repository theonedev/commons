package io.onedev.commons.jsyntax.coffeescript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;


public class CoffeescriptTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new CoffeescriptTokenizer(), new String[]{"coffeescript/coffeescript.js"}, "test.coffee");
	}
	@Test
	public void test2() {
		verify(new CoffeescriptTokenizer(), new String[]{"coffeescript/coffeescript.js"}, "test2.coffee");
	}
}
