package io.onedev.commons.jsymbol.java;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.javaparser.ast.Modifier;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.java.symbols.CompilationUnitSymbol;
import io.onedev.commons.jsymbol.java.symbols.FieldSymbol;
import io.onedev.commons.jsymbol.java.symbols.JavaSymbol;
import io.onedev.commons.jsymbol.java.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.java.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.java.symbols.TypeSymbol.Kind;

public class JavaExtractorTest extends DescriptableExtractorTest<JavaSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new JavaExtractor().extract(null, readFile("test.source")));
		verify(readFile("composite.outline"), new JavaExtractor().extract(null, readFile("composite.source")));
		verify(readFile("lcount.outline"), new JavaExtractor().extract(null, readFile("lcount.source")));
		verify(readFile("resource.outline"), new JavaExtractor().extract(null, readFile("resource.source")));
	}

	@Override
	protected List<String> describe(List<JavaSymbol> context, JavaSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof CompilationUnitSymbol) {
			CompilationUnitSymbol compilationUnit = (CompilationUnitSymbol) symbol;
			if (compilationUnit.getName() != null)
				builder.append("package ").append(compilationUnit.getName());
		} else if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			for (Modifier modifier: typeSymbol.getModifiers()) 
				builder.append(modifier.name().toLowerCase()).append(" ");

			if (typeSymbol.getKind() == Kind.ANNOTATION)
				builder.append("@interface").append(" ");
			else
				builder.append(typeSymbol.getKind().toString().toLowerCase()).append(" ");
			builder.append(typeSymbol.getName());
			if (typeSymbol.getTypeParams() != null)
				builder.append(typeSymbol.getTypeParams());
		} else if (symbol instanceof FieldSymbol) {
			FieldSymbol fieldSymbol = (FieldSymbol) symbol;
			for (Modifier modifier: fieldSymbol.getModifiers()) 
				builder.append(modifier.name().toLowerCase()).append(" ");
			if (fieldSymbol.getType() != null)
				builder.append(fieldSymbol.getType()).append(" ");
			builder.append(fieldSymbol.getName());
		} else if (symbol instanceof MethodSymbol) {
			MethodSymbol methodSymbol = (MethodSymbol) symbol;
			for (Modifier modifier: methodSymbol.getModifiers()) 
				builder.append(modifier.name().toLowerCase()).append(" ");
			if (methodSymbol.getReturnTypeParams() != null)
				builder.append(methodSymbol.getReturnTypeParams()).append(" ");
			if (methodSymbol.getReturnType() != null)
				builder.append(methodSymbol.getReturnType()).append(" ");
			builder.append(methodSymbol.getName());
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
