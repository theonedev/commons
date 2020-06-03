package io.onedev.commons.jsymbol.r;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.r.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.r.symbols.RSymbol;
import io.onedev.commons.jsymbol.r.symbols.VariableSymbol;

public class RExtractorTest extends DescriptableExtractorTest<RSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new RExtractor().extract(null, readFile("test.source")));
	}

	@Override
	protected List<String> describe(List<RSymbol> context, RSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();

		if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			if (functionSymbol.isLocal())
				builder.append("local ");
			builder.append("@function ").append(functionSymbol.getName());
			builder.append(functionSymbol.getParameters());
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
