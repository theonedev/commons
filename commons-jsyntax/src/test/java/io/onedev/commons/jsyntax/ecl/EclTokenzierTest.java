package io.onedev.commons.jsyntax.ecl;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class EclTokenzierTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new EclTokenzier(), new String[] {"ecl/ecl.js"}, "test.ecl");
		
	}
	@Test
	public void test2() {
		verify(new EclTokenzier(), new String[] {"ecl/ecl.js"}, "test2.ecl");
	}
}
