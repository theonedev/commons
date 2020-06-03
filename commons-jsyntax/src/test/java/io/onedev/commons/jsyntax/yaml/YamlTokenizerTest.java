package io.onedev.commons.jsyntax.yaml;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class YamlTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new YamlTokenizer(), new String[] {"yaml/yaml.js"}, "test.yaml");
	}
	@Test
	public void test2() {
		verify(new YamlTokenizer(), new String[] {"yaml/yaml.js"}, "test2.yaml");
	}
}
