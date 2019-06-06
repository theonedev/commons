package io.onedev.commons.jsymbol.less;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.less.LessExtractor;
import io.onedev.commons.jsymbol.less.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.less.symbols.IdSymbol;
import io.onedev.commons.jsymbol.less.symbols.LessSymbol;
import io.onedev.commons.jsymbol.less.symbols.MixinSymbol;
import io.onedev.commons.jsymbol.less.symbols.SelectorSymbol;
import io.onedev.commons.jsymbol.less.symbols.VariableSymbol;

public class LessExtractorTest extends DescriptableExtractorTest<LessSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new LessExtractor().extract(null, readFile("test.source")));
	}

	@Override
	protected List<String> describe(List<LessSymbol> context, LessSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();

		if (symbol instanceof ClassSymbol) {
			ClassSymbol classSymbol = (ClassSymbol) symbol;
			builder.append(".").append(classSymbol.getName());
		} else if (symbol instanceof IdSymbol) {
			IdSymbol idSymbol = (IdSymbol) symbol;
			builder.append("#").append(idSymbol.getName());
		} else if (symbol instanceof MixinSymbol) {
			MixinSymbol mixinSymbol = (MixinSymbol) symbol;
			if (mixinSymbol.isLocal())
				builder.append("local ");
			if (mixinSymbol.getPrefix() != null)
				builder.append(mixinSymbol.getPrefix());
			builder.append(mixinSymbol.getName());
			builder.append(mixinSymbol.getParams());
		} else if (symbol instanceof SelectorSymbol) {
			SelectorSymbol selectorSymbol = (SelectorSymbol) symbol;
			builder.append(selectorSymbol.getName());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			if (variableSymbol.isLocal())
				builder.append("local ");
			builder.append("@var ").append(variableSymbol.getName());
		} else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		
		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
