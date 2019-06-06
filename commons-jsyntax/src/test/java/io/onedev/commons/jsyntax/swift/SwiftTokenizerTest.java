package io.onedev.commons.jsyntax.swift;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.swift.SwiftTokenizer;

public class SwiftTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SwiftTokenizer(), new String[] {"swift/swift.js"}, "test.swift");
	}
}
