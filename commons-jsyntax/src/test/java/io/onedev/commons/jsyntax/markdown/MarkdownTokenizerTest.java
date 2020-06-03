package io.onedev.commons.jsyntax.markdown;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class MarkdownTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new MarkdownTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "markdown/markdown.js" }, "test.mkd", "text/x-markdown");
		verify(new MarkdownTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "markdown/markdown.js" }, "test2.mkd", "text/x-markdown");
		verify(new MarkdownTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "markdown/markdown.js" }, "test3.mkd", "text/x-markdown");
	}
}
