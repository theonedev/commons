package io.onedev.commons.jsymbol.golang;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.go.GolangExtractor;
import io.onedev.commons.jsymbol.go.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.go.symbols.GolangSymbol;
import io.onedev.commons.jsymbol.go.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.go.symbols.VariableSymbol;

public class GolangExtractorTest extends DescriptableExtractorTest<GolangSymbol> {

	@Test
	public void testNamespace() {
		verify(readFile("test.outline"), new GolangExtractor().extract(null, readFile("test.source")));
	}
	
	@Override
	protected List<String> describe(List<GolangSymbol> context, GolangSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			builder.append("func ").append(functionSymbol.getName()).append(functionSymbol.getFunctionParams());
			if (functionSymbol.getType() != null)
				builder.append(" ").append(functionSymbol.getType());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			builder.append(variableSymbol.getName());
			if (variableSymbol.getType() != null)
				builder.append(" ").append(variableSymbol.getType());
		} else if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			builder.append(typeSymbol.getName());
		} else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		
		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
