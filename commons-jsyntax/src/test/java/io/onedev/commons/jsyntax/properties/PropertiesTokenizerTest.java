package io.onedev.commons.jsyntax.properties;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PropertiesTokenizerTest extends AbstractTokenizerTest{
	@Test
	public void test() {
		verify(new PropertiesTokenizer(), new String[] {"properties/properties.js"}, "test.properties");
	}
}