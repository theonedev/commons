package io.onedev.commons.jsyntax.soy;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.soy.SoyTokenizer;

public class SoyTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SoyTokenizer(), 
				new String[] {"xml/xml.js", "javascript/javascript.js", "css/css.js", "htmlmixed/htmlmixed.js", "soy/soy.js"},
				"test.soy", "text/x-soy");
	}
}
