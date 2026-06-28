package io.onedev.commons.jsymbol.rust;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.SymbolExtractorRegistry;
import io.onedev.commons.jsymbol.rust.symbols.BlockSymbol;
import io.onedev.commons.jsymbol.rust.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.rust.symbols.RustSymbol;
import io.onedev.commons.jsymbol.rust.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.rust.symbols.VariableSymbol;

public class RustExtractorTest extends DescriptableExtractorTest<RustSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new RustExtractor().extract(null, readFile("test.source")));
	}

	@Test
	public void testRegistry() {
		Assert.assertEquals(RustExtractor.class, SymbolExtractorRegistry.getExtractor("lib.rs").getClass());
	}

	@Override
	protected List<String> describe(List<RustSymbol> context, RustSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			builder.append(typeSymbol.getKind()).append(" ").append(typeSymbol.getName());
		} else if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			builder.append("fn ").append(functionSymbol.getName()).append(functionSymbol.getParameters());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			builder.append(variableSymbol.getKind()).append(" ").append(variableSymbol.getName());
		} else if (symbol instanceof BlockSymbol) {
			BlockSymbol blockSymbol = (BlockSymbol) symbol;
			builder.append(blockSymbol.getKind()).append(" ").append(blockSymbol.getName());
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
