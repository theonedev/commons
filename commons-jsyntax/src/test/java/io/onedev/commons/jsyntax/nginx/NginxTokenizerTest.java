package io.onedev.commons.jsyntax.nginx;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.nginx.NginxTokenizer;


public class NginxTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new NginxTokenizer(), new String[] {"nginx/nginx.js"}, "nginx","nginx");
		verify(new NginxTokenizer(), new String[] {"nginx/nginx.js"}, "nginx2","nginx");
		verify(new NginxTokenizer(), new String[] {"nginx/nginx.js"}, "nginx3","nginx");
	}
}
