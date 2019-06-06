package io.onedev.commons.jsyntax.tornado;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.tornado.TornadoTokenizer;

public class TornadoTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new TornadoTokenizer(), 
				new String[] {"xml/xml.js", "javascript/javascript.js", "css/css.js", "htmlmixed/htmlmixed.js", "tornado/tornado.js"}, 
				"test.html", "text/x-tornado");
	}
}
