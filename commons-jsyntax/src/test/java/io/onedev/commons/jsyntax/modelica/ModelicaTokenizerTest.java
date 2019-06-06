package io.onedev.commons.jsyntax.modelica;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.modelica.ModelicaTokenizer;

public class ModelicaTokenizerTest extends AbstractTokenizerTest{
	@Test
	public void test() {
		verify(new ModelicaTokenizer(), new String[] {"modelica/modelica.js"}, "test.mo");
	}
}