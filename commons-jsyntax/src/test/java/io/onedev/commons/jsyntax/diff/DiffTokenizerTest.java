package io.onedev.commons.jsyntax.diff;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.diff.DiffTokenizer;

public class DiffTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new DiffTokenizer(), new String[] {"diff/diff.js"}, "test.diff", "diff");
	}
}
