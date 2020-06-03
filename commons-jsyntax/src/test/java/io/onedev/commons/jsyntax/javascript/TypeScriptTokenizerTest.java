package io.onedev.commons.jsyntax.javascript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class TypeScriptTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new TypeScriptTokenizer(), new String[]{"javascript/javascript.js"}, "test.ts");
	}

}
