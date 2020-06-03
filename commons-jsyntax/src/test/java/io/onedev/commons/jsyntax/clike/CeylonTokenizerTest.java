package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class CeylonTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		// ceylon mode not added to meta yet
		verify(new CeylonTokenizer(), new String[]{"clike/clike.js"}, "test.ceylon", "text/x-ceylon");
		verify(new CeylonTokenizer(), new String[]{"clike/clike.js"}, "test2.ceylon", "text/x-ceylon");
	}

}
