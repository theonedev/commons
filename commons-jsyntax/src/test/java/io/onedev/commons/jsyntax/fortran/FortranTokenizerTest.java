package io.onedev.commons.jsyntax.fortran;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class FortranTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new FortranTokenizer(), new String[] {"fortran/fortran.js"}, "test.f");
	}
}
