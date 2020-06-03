package io.onedev.commons.jsyntax.powershell;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class PowerShellTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new PowerShellTokenizer(), new String[] {"powershell/powershell.js"}, "test.ps1");
		verify(new PowerShellTokenizer(), new String[] {"powershell/powershell.js"}, "test2.ps1");
	}
}
