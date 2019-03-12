package io.onedev.commons.jsyntax.turtle;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.turtle.TurtleTokenizer;

public class TurtleTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new TurtleTokenizer(), new String[] {"turtle/turtle.js"}, "test.turtle", "turtle");
		verify(new TurtleTokenizer(), new String[] {"turtle/turtle.js"}, "test2.turtle", "turtle");
	}
}
