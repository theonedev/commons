package io.onedev.commons.jsyntax.protobuf;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.protobuf.ProtobufTokenizer;

public class ProtobufTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new ProtobufTokenizer(), new String[] {"protobuf/protobuf.js"}, "test.proto","protobuf");
	}

}
