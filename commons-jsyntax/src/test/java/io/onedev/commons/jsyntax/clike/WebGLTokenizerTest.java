package io.onedev.commons.jsyntax.clike;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class WebGLTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new WebGLTokenizer(), new String[]{"clike/clike.js"}, "test.webgl", "x-shader/x-vertex");
	}

}
