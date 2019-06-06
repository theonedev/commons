package io.onedev.commons.jsyntax.python;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.python.PythonTokenizer;

public class PythonTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new PythonTokenizer(true), new String[] {"python/python.js"}, "test.py");
	}
}
