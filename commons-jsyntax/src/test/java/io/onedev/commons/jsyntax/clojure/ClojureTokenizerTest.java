package io.onedev.commons.jsyntax.clojure;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.clojure.ClojureTokenizer;

public class ClojureTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new ClojureTokenizer(), new String[] {"clojure/clojure.js"}, "test.clj");
		verify(new ClojureTokenizer(), new String[] {"clojure/clojure.js"}, "test2.clj");
	}
}
