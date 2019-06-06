package io.onedev.commons.jsyntax.brainfuck;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.brainfuck.BrainfuckTokenizer;

public class BrainfuckTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new BrainfuckTokenizer(), new String[]{"brainfuck/brainfuck.js"}, "test.bf");
	}

}
