package io.onedev.commons.jsyntax.cmake;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.cmake.CmakeTokenizer;


public class CmakeTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new CmakeTokenizer(), new String[] {"cmake/cmake.js"}, "test.cmake");
	}
}
