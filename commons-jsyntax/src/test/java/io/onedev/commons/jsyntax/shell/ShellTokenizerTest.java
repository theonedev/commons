package io.onedev.commons.jsyntax.shell;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class ShellTokenizerTest extends AbstractTokenizerTest{
	@Test
	public void test() {
		verify(new ShellTokenizer(), new String[] {"shell/shell.js"}, "test.sh");
	}
}
