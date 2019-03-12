package io.onedev.commons.jsyntax.cobol;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.cobol.CobolTokenizer;

public class CobolTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new CobolTokenizer(), new String[] {"cobol/cobol.js"}, "test.cob");
	}
	
}
