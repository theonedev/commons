package io.onedev.commons.jsyntax.haxe;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.haxe.HaxeTokenizer;

public class HaxeTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new HaxeTokenizer(), new String[] {"haxe/haxe.js"}, "test.hx");
	}
}
