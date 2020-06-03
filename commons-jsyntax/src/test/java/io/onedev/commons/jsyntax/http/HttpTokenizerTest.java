package io.onedev.commons.jsyntax.http;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class HttpTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new HttpTokenizer(), new String[] {"http/http.js"}, "test.http","http");
	}
}
