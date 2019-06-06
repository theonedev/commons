package io.onedev.commons.jsyntax.xml;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.xml.XmlTokenizer;

public class XmlTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new XmlTokenizer(), new String[] {"xml/xml.js"}, "test.xml");
		verify(new XmlTokenizer(true, null), new String[] {"xml/xml.js"}, "test.htm");
	}
}
