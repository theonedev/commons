package io.onedev.commons.jsyntax.vbscript;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.vbscript.VbScriptTokenizer;

public class VbScriptTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new VbScriptTokenizer(), new String[] {"vbscript/vbscript.js"}, "test.vbs");
	}
}
