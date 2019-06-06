package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.KotlinTokenizer;

public class KotlinTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new KotlinTokenizer(), new String[]{"clike/clike.js"}, "test.kt.txt");
	}

}
