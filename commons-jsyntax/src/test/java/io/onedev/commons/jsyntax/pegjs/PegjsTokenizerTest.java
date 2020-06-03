package io.onedev.commons.jsyntax.pegjs;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PegjsTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new PegjsTokenizer(), new String[] {"javascript/javascript.js", "pegjs/pegjs.js"}, "test.jsonld", "pegjs");
	}
}
