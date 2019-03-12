package io.onedev.commons.jsyntax.htmlembedded;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.htmlembedded.JspTokenizer;

public class JspTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new JspTokenizer(), 
				new String[] {"xml/xml.js", "javascript/javascript.js", "css/css.js",
						"htmlmixed/htmlmixed.js", "clike/clike.js", "htmlembedded/htmlembedded.js"}, 
				"test.jsp", "application/x-jsp");
	}
}
