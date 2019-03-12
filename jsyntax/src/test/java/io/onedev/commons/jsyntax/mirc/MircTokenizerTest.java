package io.onedev.commons.jsyntax.mirc;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.mirc.MircTokenizer;

public class MircTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new MircTokenizer(), new String[] {"mirc/mirc.js"}, "test.mirc","mirc");
		verify(new MircTokenizer(), new String[] {"mirc/mirc.js"}, "test2.mirc","mirc");
		verify(new MircTokenizer(), new String[] {"mirc/mirc.js"}, "test3.mirc","mirc");
	}
}
