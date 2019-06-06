package io.onedev.commons.jsymbol;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Resources;

import io.onedev.commons.jsymbol.Symbol;

public abstract class DescriptableExtractorTest<T extends Symbol> {

	/**
	 * Describe specified symbol in specified context
	 * 
	 * @param context
	 * 			context to describe the symbol in. Non-leaf symbols will use context to describe 
	 * 			itself and all its child symbols. For instance, a Java class symbol description
	 * 			contains not only the class definition, but also all declaration symbols inside 
	 * 			the class including fields and methods. In this case, it will look inside the 
	 * 			context for symbols with parent pointing to it
	 * @param symbol
	 * 			symbol to be described. Non-leaf symbol should also describe its child symbols  
	 * 		
	 * @return
	 * 			Description of the symbol
	 */
	protected abstract List<String> describe(List<T> context, T symbol);

	/**
	 * This method describes the list of symbols into a string and compares it with expected string
	 */
	protected void verify(List<String> expected, List<T> symbols) {
		List<String> lines = new ArrayList<>();
		for (T symbol: symbols) {
			if (symbol.getParent() == null)
				lines.addAll(describe(symbols, symbol));
		}
		
		Assert.assertEquals(Joiner.on("\n").join(expected), Joiner.on("\n").join(lines));
	}

	protected List<String> readFile(String fileName) {
		try {
			String content = Resources.toString(Resources.getResource(getClass(), fileName), Charset.forName("UTF8"));
			List<String> lines = new ArrayList<>();
			for (String line: Splitter.on("\n").omitEmptyStrings().split(content)) {
				line = StringUtils.stripEnd(line, " \t\r");
				if (line.length() != 0)
					lines.add(line);
			}
			return lines;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void appendChildren(List<String> lines, List<T> context, T symbol) {
		List<T> children = new ArrayList<>();
		for (T each: context) {
			if (each.getParent() == symbol)
				children.add(each);
		}
		if (!children.isEmpty()) {
			lines.add("{");
			for (T child: children) {
				for (String line: describe(context, child)) {
					line = StringUtils.stripEnd(line, " \t\r");
					if (line.length() != 0)
						lines.add("  " + line);
				}
			}
			lines.add("}");
		}
	}
	
}
