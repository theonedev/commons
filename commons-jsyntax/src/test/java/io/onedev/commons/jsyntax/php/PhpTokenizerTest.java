package io.onedev.commons.jsyntax.php;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PhpTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new PhpTokenizer(), new String[] {"xml/xml.js", "javascript/javascript.js", "css/css.js", 
				"htmlmixed/htmlmixed.js", "clike/clike.js", "php/php.js"}, "test.php");
	}
}
