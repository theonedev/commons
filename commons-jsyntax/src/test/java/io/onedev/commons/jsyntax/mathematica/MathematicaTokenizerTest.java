package io.onedev.commons.jsyntax.mathematica;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class MathematicaTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new MathematicaTokenizer(), new String[] {"mathematica/mathematica.js"}, "test.m");
	}
}
