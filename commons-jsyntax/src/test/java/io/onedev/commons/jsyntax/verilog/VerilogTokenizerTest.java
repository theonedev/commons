package io.onedev.commons.jsyntax.verilog;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class VerilogTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new VerilogTokenizer(), new String[] {"verilog/verilog.js"}, "test.v", "text/x-verilog");
		verify(new VerilogTokenizer(), new String[] {"verilog/verilog.js"}, "test2.v", "text/x-verilog");
	}
}
