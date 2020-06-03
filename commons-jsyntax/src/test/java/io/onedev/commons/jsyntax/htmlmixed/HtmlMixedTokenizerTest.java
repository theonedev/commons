package io.onedev.commons.jsyntax.htmlmixed;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class HtmlMixedTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new HtmlMixedTokenizer(), new String[] 
				{"xml/xml.js", "javascript/javascript.js", "css/css.js", "htmlmixed/htmlmixed.js"}, "test.htm");
	}
}
