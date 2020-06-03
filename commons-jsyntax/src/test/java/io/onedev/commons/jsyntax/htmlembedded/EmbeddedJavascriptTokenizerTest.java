package io.onedev.commons.jsyntax.htmlembedded;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class EmbeddedJavascriptTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new EmbeddedJavascriptTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "htmlembedded/htmlembedded.js" }, "test.ejs", "application/x-ejs");
	}
}
