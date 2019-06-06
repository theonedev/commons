package io.onedev.commons.jsyntax.q;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.q.QTokenizer;

public class QTokenizerTest extends AbstractTokenizerTest{
	@Test
	public void test() {
		verify(new QTokenizer(), new String[] {"q/q.js"}, "test.q");
	}

}