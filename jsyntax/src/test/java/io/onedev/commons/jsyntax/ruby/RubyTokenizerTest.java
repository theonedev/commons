package io.onedev.commons.jsyntax.ruby;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.ruby.RubyTokenizer;

public class RubyTokenizerTest extends AbstractTokenizerTest {
	
	@Test
	public void test() {
		verify(new RubyTokenizer(), new String[] {"ruby/ruby.js"}, "test.rb");
	}
}
