package io.onedev.commons.jsymbol.scss;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.scss.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.scss.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.scss.symbols.IdSymbol;
import io.onedev.commons.jsymbol.scss.symbols.MixinSymbol;
import io.onedev.commons.jsymbol.scss.symbols.ScssSymbol;
import io.onedev.commons.jsymbol.scss.symbols.SelectorSymbol;
import io.onedev.commons.jsymbol.scss.symbols.VariableSymbol;

public class ScssExtractorTest extends DescriptableExtractorTest<ScssSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new ScssExtractor().extract(null, readFile("test.source")));
	}

	@Override
	protected List<String> describe(List<ScssSymbol> context, ScssSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();

		if (symbol instanceof ClassSymbol) {
			ClassSymbol classSymbol = (ClassSymbol) symbol;
			builder.append(".").append(classSymbol.getName());
		} else if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			if (functionSymbol.isLocal())
				builder.append("local ");
			builder.append("@function ").append(functionSymbol.getName());
			if (functionSymbol.getParams() != null)
				builder.append(functionSymbol.getParams());
		} else if (symbol instanceof IdSymbol) {
			IdSymbol idSymbol = (IdSymbol) symbol;
			builder.append("#").append(idSymbol.getName());
		} else if (symbol instanceof MixinSymbol) {
			MixinSymbol mixinSymbol = (MixinSymbol) symbol;
			if (mixinSymbol.isLocal())
				builder.append("local ");
			builder.append("@mixin ").append(mixinSymbol.getName());
			if (mixinSymbol.getParams() != null)
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
