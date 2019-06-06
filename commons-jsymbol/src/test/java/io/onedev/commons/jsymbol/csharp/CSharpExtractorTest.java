package io.onedev.commons.jsymbol.csharp;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.csharp.CSharpExtractor;
import io.onedev.commons.jsymbol.csharp.symbols.CSharpSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.FieldSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.CSharpSymbol.Modifier;

public class CSharpExtractorTest extends DescriptableExtractorTest<CSharpSymbol> {

	@Test
	public void testNamespace() {
		verify(readFile("namespace.outline"), new CSharpExtractor().extract(null, readFile("namespace.source")));
	}

	@Test
	public void testClass() {
		verify(readFile("class.outline"), new CSharpExtractor().extract(null, readFile("class.source")));
	}
	
	@Test
	public void testPreprocess() {
		verify(readFile("preprocess.outline"), new CSharpExtractor().extract(null, readFile("preprocess.source")));
	}
	
	@Test
	public void testExplicitImplements() {
		List<CSharpSymbol> symbols = new CSharpExtractor().extract(null, readFile("explicit_implements.source"));
		verify(readFile("explicit_implements.outline"), symbols);
		boolean found = false;
		for (CSharpSymbol symbol: symbols) {
			if (symbol.getName().equals("test")) {
				found = true;
				break;
			}
		}
		Assert.assertTrue(found);
	}
	
	@Test
	public void testOperatorOverloading() {
		verify(readFile("operator_overloading.outline"), 
				new CSharpExtractor().extract(null, readFile("operator_overloading.source")));
	}
	
	@Test
	public void testVarargs() {
		verify(readFile("varargs.outline"), new CSharpExtractor().extract(null, readFile("varargs.source")));
	}
	
	@Test
	public void testDelegateAndEvent() {
		verify(readFile("delegate_event.outline"), 
				new CSharpExtractor().extract(null, readFile("delegate_event.source")));
	}
	
	@Test
	public void testV7() {
		verify(readFile("v7.outline"), 
				new CSharpExtractor().extract(null, readFile("v7.source")));
	}
	
	@Override
	protected List<String> describe(List<CSharpSymbol> context, CSharpSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof NamespaceSymbol) {
			NamespaceSymbol namespaceSymbol = (NamespaceSymbol) symbol;
			builder.append("namespace ").append(namespaceSymbol.getQualifiedName());
		} else if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			for (Modifier modifier: typeSymbol.getModifiers()) 
				builder.append(modifier.name().toLowerCase()).append(" ");

			builder.append(typeSymbol.getKind().toString().toLowerCase()).append(" ");
			builder.append(typeSymbol.getQualifiedName());
			if (typeSymbol.getTypeParams() != null)
				builder.append(typeSymbol.getTypeParams());
		} else if (symbol instanceof FieldSymbol) {
			FieldSymbol fieldSymbol = (FieldSymbol) symbol;
			for (Modifier modifier: fieldSymbol.getModifiers()) 
				builder.append(modifier.name().toLowerCase()).append(" ");
			if (fieldSymbol.getKind() == FieldSymbol.Kind.EVENT)
				builder.append("event ");
			if (fieldSymbol.getType() != null)
				builder.append(fieldSymbol.getType()).append(" ");
			builder.append(fieldSymbol.getQualifiedName());
			if (fieldSymbol.getIndexParams() != null)
				builder.append(fieldSymbol.getIndexParams());
		} else if (symbol instanceof MethodSymbol) {
			MethodSymbol methodSymbol = (MethodSymbol) symbol;
			for (Modifier modifier: methodSymbol.getModifiers()) 
				builder.append(modifier.name().toLowerCase()).append(" ");
			if (methodSymbol.getMethodPrefix() != null)
				builder.append(methodSymbol.getMethodPrefix()).append(" ");
			if (methodSymbol.getKind() == MethodSymbol.Kind.DELEGATE)
				builder.append("delegate ");
			if (methodSymbol.getType() != null)
				builder.append(methodSymbol.getType()).append(" ");
			builder.append(methodSymbol.getQualifiedName());
			if (methodSymbol.getTypeParams() != null)
				builder.append(methodSymbol.getTypeParams());
			if (methodSymbol.getMethodParams() != null)
				builder.append("(").append(methodSymbol.getMethodParams()).append(")");
			else
				builder.append("()");
		} else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		
		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
