package io.onedev.commons.jsyntax.slim;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.slim.SlimTokenizer;

public class SlimTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SlimTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "ruby/ruby.js", "slim/slim.js" }, "test.slim");
	}
}
