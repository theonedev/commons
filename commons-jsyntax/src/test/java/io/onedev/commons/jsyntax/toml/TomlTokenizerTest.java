package io.onedev.commons.jsyntax.toml;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class TomlTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new TomlTokenzier(), new String[] {"toml/toml.js"}, "test.toml");
	}
}
