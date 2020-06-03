package io.onedev.commons.jsyntax.perl;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PerlTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new PerlTokenizer(), new String[] {"perl/perl.js"}, "test.pl");
	}
}
