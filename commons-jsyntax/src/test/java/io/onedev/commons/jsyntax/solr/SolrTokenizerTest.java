package io.onedev.commons.jsyntax.solr;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class SolrTokenizerTest extends AbstractTokenizerTest {
	@Test
	public void test() {
		verify(new SolrTokenzier(), new String[] {"solr/solr.js"}, "test.solr","solr");
	}
	@Test
	public void teat() {
		verify(new SolrTokenzier(), new String[] {"solr/solr.js"}, "test2.solr","solr");
	}
}
