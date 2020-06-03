package io.onedev.commons.jsyntax.puppet;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PuppetTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new PuppetTokenizer(), new String[] {"puppet/puppet.js"}, "test.pp");
	}
	@Test
	public void test2() {
		verify(new PuppetTokenizer(), new String[] {"puppet/puppet.js"}, "test2.pp");
	}
}
