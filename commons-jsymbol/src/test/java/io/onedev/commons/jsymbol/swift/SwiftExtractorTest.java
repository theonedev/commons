package io.onedev.commons.jsymbol.swift;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.SymbolExtractorRegistry;
import io.onedev.commons.jsymbol.swift.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.swift.symbols.SwiftSymbol;
import io.onedev.commons.jsymbol.swift.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.swift.symbols.VariableSymbol;

public class SwiftExtractorTest extends DescriptableExtractorTest<SwiftSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new SwiftExtractor().extract(null, readFile("test.source")));
	}

	@Test
	public void testEdgeCases() {
		verify(readFile("edge-cases.outline"), new SwiftExtractor().extract(null, readFile("edge-cases.source")));
	}

	@Test
	public void testRegistry() {
		Assert.assertEquals(SwiftExtractor.class, SymbolExtractorRegistry.getExtractor("test.swift").getClass());
	}

	@Override
	protected List<String> describe(List<SwiftSymbol> context, SwiftSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			builder.append(typeSymbol.getKind()).append(" ").append(typeSymbol.getName());
		} else if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			builder.append(functionSymbol.getKind()).append(" ").append(functionSymbol.getName())
					.append(functionSymbol.getParameters());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			builder.append(variableSymbol.getKind()).append(" ").append(variableSymbol.getName());
		} else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		if (symbol.isLocal())
			builder.append(" local");

		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
