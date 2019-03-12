package io.onedev.commons.jsyntax.jsx;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.jsx.JsxTokenizer;

public class JsxTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new JsxTokenizer(), new String[] {"xml/xml.js", "javascript/javascript.js", "jsx/jsx.js"}, "test.jsx", "text/jsx");
		verify(new JsxTokenizer(), new String[] {"xml/xml.js", "javascript/javascript.js", "jsx/jsx.js"}, "test2.jsx", "text/jsx");
	}
}
