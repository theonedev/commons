package io.onedev.commons.jsyntax.vue;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.vue.VueTokenizer;

public class VueTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new VueTokenizer(),
				new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js", "htmlmixed/htmlmixed.js",
						"coffeescript/coffeescript.js", "sass/sass.js", "stylus/stylus.js", "pug/pug.js",
						"handlebars/handlebars.js", "vue/vue.js" },
				"test.vue");
		verify(new VueTokenizer(),
				new String[] { "xml/xml.js", "javascript/javascript.js", "css/css.js", "htmlmixed/htmlmixed.js",
						"coffeescript/coffeescript.js", "sass/sass.js", "stylus/stylus.js", "pug/pug.js",
						"handlebars/handlebars.js", "vue/vue.js" },
				"test2.vue");
	}
}
