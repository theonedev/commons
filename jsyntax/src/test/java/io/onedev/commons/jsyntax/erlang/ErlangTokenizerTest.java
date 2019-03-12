package io.onedev.commons.jsyntax.erlang;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.erlang.ErlangTokenizer;

public class ErlangTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new ErlangTokenizer(), new String[] { "erlang/erlang.js" }, "test.erl");
		verify(new ErlangTokenizer(), new String[] { "erlang/erlang.js" }, "test2.erl");
	}
}
