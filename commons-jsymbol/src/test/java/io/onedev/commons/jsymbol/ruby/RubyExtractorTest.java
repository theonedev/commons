package io.onedev.commons.jsymbol.ruby;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.SymbolExtractorRegistry;
import io.onedev.commons.jsymbol.ruby.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.ruby.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.ruby.symbols.RubySymbol;
import io.onedev.commons.jsymbol.ruby.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.ruby.symbols.VariableSymbol;

public class RubyExtractorTest extends DescriptableExtractorTest<RubySymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new RubyExtractor().extract(null, readFile("test.source")));
	}

	@Test
	public void testRegistry() {
		Assert.assertEquals(RubyExtractor.class, SymbolExtractorRegistry.getExtractor("model.rb").getClass());
		Assert.assertEquals(RubyExtractor.class, SymbolExtractorRegistry.getExtractor("Rakefile").getClass());
		Assert.assertEquals(RubyExtractor.class, SymbolExtractorRegistry.getExtractor("demo.gemspec").getClass());
	}

	@Override
	protected List<String> describe(List<RubySymbol> context, RubySymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			builder.append(typeSymbol.getKind()).append(" ").append(typeSymbol.getName());
		} else if (symbol instanceof MethodSymbol) {
			MethodSymbol methodSymbol = (MethodSymbol) symbol;
			if (methodSymbol.isSingleton())
				builder.append("self.");
			builder.append(methodSymbol.getName()).append(methodSymbol.getParameters());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			builder.append(variableSymbol.getKind()).append(" ").append(variableSymbol.getName());
		} else if (symbol instanceof ImportedSymbol) {
			ImportedSymbol importedSymbol = (ImportedSymbol) symbol;
			builder.append("require ").append(importedSymbol.getImported());
		} else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		if (!(symbol instanceof ImportedSymbol) && symbol.isLocal())
			builder.append(" local");

		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
