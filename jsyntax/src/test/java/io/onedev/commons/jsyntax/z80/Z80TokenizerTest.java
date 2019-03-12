package io.onedev.commons.jsyntax.z80;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.z80.Z80Tokenizer;

public class Z80TokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new Z80Tokenizer(), new String[] {"z80/z80.js"}, "test.z80", "z80");
		verify(new Z80Tokenizer(true), new String[] {"z80/z80.js"}, "test.z80", "text/x-ez80");
	}
}
