package io.onedev.commons.jsyntax.rust;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.rust.RustTokenizer;

public class RustTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new RustTokenizer(), new String[] {"rust/rust.js"}, "test.rs");
	}
}
