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
	public void testRanges() {
		for (String extension : new String[] { "js", "jsx", "ts", "tsx", "mts", "cts" })
			verifyRanges(new TypescriptExtractor(), "ranges." + extension);
	}

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

	@Test
	public void testEnclosingScopes() {
		String source = "function top() {\n return 1;\n}\n"
				+ "const arrow = (x) => {\n return x;\n}, constant = 1;\n"
				+ "String.prototype.escape = function() {\n return this;\n};\n"
				+ "onedev.server = {\n form: {\n markDirty: function() {\n return true;\n},\n"
				+ "markClean() {\n return false;\n},\ncompact: (x) => x\n}\n};\n"
				+ "class Service {\n run() {\n return true;\n}\n}\n"
				+ "const object = {\n nested: {\n f: () => {\n return 3;\n}\n}\n};";
		var symbols = new TypescriptExtractor().extract("test.js", source);
		assertScope(symbols, "top", 0, 2);
		assertScope(symbols, "arrow", 3, 5);
		assertScope(symbols, "String.prototype.escape", 6, 8);
		assertScope(symbols, "onedev.server", 9, 19);
		assertScope(symbols, "onedev.server.form", 10, 18);
		assertScope(symbols, "onedev.server.form.markDirty", 11, 13);
		assertScope(symbols, "onedev.server.form.markClean", 14, 16);
		assertScope(symbols, "onedev.server.form.compact", 17, 17);
		assertScope(symbols, "Service", 20, 24);
		assertScope(symbols, "Service.run", 21, 23);
		assertScope(symbols, "object", 25, 31);
		assertScope(symbols, "object.nested", 26, 30);
		assertScope(symbols, "object.nested.f", 27, 29);
		Assert.assertNull(symbols.stream().filter(it -> it.getName().equals("constant")).findFirst().get().getScope());
	}

	private void assertScope(List<TypeScriptSymbol> symbols, String fqn, int from, int to) {
		var symbol = symbols.stream().filter(it -> it.getFQN().equals(fqn)).findFirst().orElseThrow();
		Assert.assertNotNull(fqn, symbol.getScope());
		Assert.assertEquals(fqn, from, symbol.getScope().getFromRow());
		Assert.assertEquals(fqn, to, symbol.getScope().getToRow());
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
