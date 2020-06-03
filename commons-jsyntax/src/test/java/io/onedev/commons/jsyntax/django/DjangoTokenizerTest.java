package io.onedev.commons.jsyntax.django;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class DjangoTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new DjangoTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "django/django.js" }, "test.html", "text/x-django");
		verify(new DjangoTokenizer(), new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js",
				"htmlmixed/htmlmixed.js", "django/django.js" }, "test2.html", "text/x-django");
	}
}
