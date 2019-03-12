package io.onedev.commons.jsyntax.gfm;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.gfm.GfmTokenizer;

public class GfmTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new GfmTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "markdown/markdown.js" , "gfm/gfm.js" }, "test.md", "text/x-gfm");
		verify(new GfmTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "markdown/markdown.js" , "gfm/gfm.js" }, "test2.md", "text/x-gfm");
	}
}
