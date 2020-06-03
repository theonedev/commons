package io.onedev.commons.jsymbol.python;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.python.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.python.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.python.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.python.symbols.PythonSymbol;
import io.onedev.commons.jsymbol.python.symbols.VariableSymbol;

public class PythonExtractorTest extends DescriptableExtractorTest<PythonSymbol> {

	@Test
	public void testMethod() {
		verify(readFile("method.outline"), new PythonExtractor().extract(null, readFile("method.source")));
	}

	@Test
	public void testClass() {
		verify(readFile("class.outline"), new PythonExtractor().extract(null, readFile("class.source")));
	}
	
	@Test
	public void testVariable() {
		verify(readFile("variable.outline"), new PythonExtractor().extract(null, readFile("variable.source")));
	}
	
	@Test
	public void testRealWorld() {
		verify(readFile("realworld.outline"), new PythonExtractor().extract(null, readFile("realworld.source")));
	}
	
	@Override
	protected List<String> describe(List<PythonSymbol> context, PythonSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof ClassSymbol) {
			ClassSymbol classSymbol = (ClassSymbol) symbol;
			builder.append("class ").append(classSymbol.getName());
		} else if (symbol instanceof MethodSymbol) {
			MethodSymbol methodSymbol = (MethodSymbol) symbol;
			builder.append(methodSymbol.getName()).append(methodSymbol.getParameters());
		} else if (symbol instanceof ImportedSymbol) {
			ImportedSymbol importedSymbol = (ImportedSymbol) symbol;
			builder.append("import ").append(importedSymbol.getQualifiedName());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			builder.append(variableSymbol.getName());
		} else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		
		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
