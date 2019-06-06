package io.onedev.commons.jsyntax.vb;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.vb.VbTokenizer;

public class VbTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new VbTokenizer(), new String[] {"vb/vb.js"}, "test.vb");
	}
}
