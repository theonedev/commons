package io.onedev.commons.jsyntax;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

public class ToHtmlTest {
	
	@Test
	public void test() {
		String code = ""
				+ "public class Test {\n"
				+ "  public static void main(String[] args) {\n"
				+ "    System.out.println(\"hello world\");\n"
				+ "  }\n"
				+ "}";
		Tokenizer tokenizer = Preconditions.checkNotNull(TokenizerRegistry.getTokenizer("Test.java"));
		List<String> lines = Splitter.on("\n").splitToList(code);
		StringBuilder highlighted = new StringBuilder();
		for (Tokenized tokenized: tokenizer.tokenize(lines)) {
			for (long token: tokenized.getTokens())
				highlighted.append(TokenUtils.toHtml(tokenized.getText(), token, null, null));
		}
		Assert.assertEquals(""
				+ "<span class='cm-keyword'>public</span> <span class='cm-keyword'>class</span> <span class='cm-def'>Test</span> {"
				+ "  <span class='cm-keyword'>public</span> <span class='cm-keyword'>static</span> <span class='cm-variable-3'>void</span> <span class='cm-variable'>main</span>(<span class='cm-variable-3'>String</span>[] <span class='cm-variable'>args</span>) {"
				+ "    <span class='cm-variable'>System</span>.<span class='cm-variable'>out</span>.<span class='cm-variable'>println</span>(<span class='cm-string'>&quot;hello world&quot;</span>);"
				+ "  }"
				+ "}", 
				highlighted.toString());
	}
	
}
