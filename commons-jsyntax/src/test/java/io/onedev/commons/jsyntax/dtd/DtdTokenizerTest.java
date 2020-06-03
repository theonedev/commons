package io.onedev.commons.jsyntax.dtd;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class DtdTokenizerTest extends AbstractTokenizerTest {	
	@Test
	public void test() {
		verify(new DtdTokenizer(),new String[] {"dtd/dtd.js"},"test.dtd");
		verify(new DtdTokenizer(),new String[] {"dtd/dtd.js"},"test2.dtd");
	}

}
