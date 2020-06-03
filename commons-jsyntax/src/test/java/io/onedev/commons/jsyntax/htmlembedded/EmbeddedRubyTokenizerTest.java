package io.onedev.commons.jsyntax.htmlembedded;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class EmbeddedRubyTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new EmbeddedRubyTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "ruby/ruby.js", "htmlembedded/htmlembedded.js" }, "test.erb");
	}
}
