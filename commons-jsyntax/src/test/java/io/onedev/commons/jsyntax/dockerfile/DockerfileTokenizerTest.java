package io.onedev.commons.jsyntax.dockerfile;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;
import io.onedev.commons.jsyntax.dockerfile.DockerfileTokenizer;

public class DockerfileTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new DockerfileTokenizer(), new String[] {"dockerfile/dockerfile.js"}, "Dockerfile", "dockerfile");
	}
}
