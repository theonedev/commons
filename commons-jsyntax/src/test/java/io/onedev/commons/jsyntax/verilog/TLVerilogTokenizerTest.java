package io.onedev.commons.jsyntax.verilog;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class TLVerilogTokenizerTest extends AbstractTokenizerTest  {

	@Test
	public void test() {
		verify(new TLVerilogTokenizer(), new String[] {"verilog/verilog.js"}, "test.v", "text/x-tlv");
		verify(new TLVerilogTokenizer(), new String[] {"verilog/verilog.js"}, "test2.v", "text/x-tlv");
		verify(new TLVerilogTokenizer(), new String[] {"verilog/verilog.js"}, "test3.v", "text/x-tlv");
	}
}
