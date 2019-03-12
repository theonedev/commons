package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.ScalaTokenizer;

public class ScalaTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new ScalaTokenizer(), new String[]{"clike/clike.js"}, "test.scala");
	}

}
