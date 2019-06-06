package io.onedev.commons.jsyntax.pug;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.pug.PugTokenizer;

public class PugTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new PugTokenizer(), 
				new String[] {"xml/xml.js", "javascript/javascript.js", "css/css.js", "htmlmixed/htmlmixed.js", "pug/pug.js"},
				"test.pug", "pug");
		verify(new PugTokenizer(), 
				new String[] {"xml/xml.js", "javascript/javascript.js", "css/css.js", "htmlmixed/htmlmixed.js", "pug/pug.js"},
				"test2.pug", "pug");
	}
}
