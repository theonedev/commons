package io.onedev.commons.jsymbol.typescript;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.SymbolExtractorRegistry;
import io.onedev.commons.jsymbol.typescript.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.typescript.symbols.TypeScriptSymbol;
import io.onedev.commons.jsymbol.typescript.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.typescript.symbols.VariableSymbol;

public class TypescriptExtractorTest extends DescriptableExtractorTest<TypeScriptSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new TypescriptExtractor().extract(null, readFile("test.source")));
	}

	@Test
	public void testAnonymousDefault() {
		verify(readFile("anonymous_default.outline"),
				new TypescriptExtractor().extract(null, readFile("anonymous_default.source")));
	}

	@Test
	public void testJsx() {
		verify(readFile("jsx.outline"), new TypescriptExtractor().extract(null, readFile("jsx.source")));
	}

	@Test
	public void testObject() {
		verify(readFile("object.outline"), new TypescriptExtractor().extract(null, readFile("object.source")));
	}

	@Test
	public void testEdgeCases() {
		verify(readFile("edge-cases.outline"), new TypescriptExtractor().extract(null, readFile("edge-cases.source")));
	}

	@Test
	public void testPropTypes() {
		verify(readFile("proptypes.outline"), new TypescriptExtractor().extract(null, readFile("proptypes.source")));
	}

	@Test
	public void testRegistry() {
		Assert.assertEquals(TypescriptExtractor.class, SymbolExtractorRegistry.getExtractor("test.js").getClass());
		Assert.assertEquals(TypescriptExtractor.class, SymbolExtractorRegistry.getExtractor("test.ts").getClass());
		Assert.assertEquals(TypescriptExtractor.class, SymbolExtractorRegistry.getExtractor("test.tsx").getClass());
		Assert.assertEquals(TypescriptExtractor.class, SymbolExtractorRegistry.getExtractor("component.jsx").getClass());
	}

	@Override
	protected List<String> describe(List<TypeScriptSymbol> context, TypeScriptSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			builder.append(typeSymbol.getKind()).append(" ").append(typeSymbol.getName());
		} else if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			builder.append("function ").append(functionSymbol.getName()).append(functionSymbol.getParameters());
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
