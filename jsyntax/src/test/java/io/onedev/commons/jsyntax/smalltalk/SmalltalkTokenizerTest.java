package io.onedev.commons.jsyntax.smalltalk;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.smalltalk.SmalltalkTokenizer;

public class SmalltalkTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SmalltalkTokenizer(), new String[] {"smalltalk/smalltalk.js"}, "test.st");
	}
}
