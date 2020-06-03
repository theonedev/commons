package io.onedev.commons.jsyntax.dart;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class DartTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new DartTokenizer(), new String[] {"clike/clike.js", "dart/dart.js"}, "test.dart", "application/dart");
	}
}
