package io.onedev.commons.jsyntax.livescript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.livescript.LivescriptTokenizer;


public class LivescriptTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new LivescriptTokenizer(), new String[] {"livescript/livescript.js"}, "test.ls","livescript");
		
	}
	@Test
	public void test2() {
		verify(new LivescriptTokenizer(), new String[] {"livescript/livescript.js"}, "test2.ls","livescript");
	}
}
