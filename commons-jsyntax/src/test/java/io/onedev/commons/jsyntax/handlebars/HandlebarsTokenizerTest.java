package io.onedev.commons.jsyntax.handlebars;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.handlebars.HandlebarsTokenizer;

public class HandlebarsTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new HandlebarsTokenizer(), new String[] { "handlebars/handlebars.js" }, "test.txt",
				"text/x-handlebars-template");
		verify(new HandlebarsTokenizer(), new String[] { "handlebars/handlebars.js" }, "test2.txt",
				"text/x-handlebars-template");
		verify(new HandlebarsTokenizer(), new String[] { "handlebars/handlebars.js" }, "test3.txt",
				"text/x-handlebars-template");
	}
}
