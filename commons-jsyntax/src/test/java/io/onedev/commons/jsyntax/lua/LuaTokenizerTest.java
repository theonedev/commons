package io.onedev.commons.jsyntax.lua;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class LuaTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new LuaTokenizer(), new String[] {"lua/lua.js"}, "test.lua");
	}
}
