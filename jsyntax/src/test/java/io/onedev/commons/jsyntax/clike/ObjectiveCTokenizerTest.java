package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clike.ObjectiveCTokenizer;

public class ObjectiveCTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new ObjectiveCTokenizer(), new String[]{"clike/clike.js"}, "test.mm");
	}

}
