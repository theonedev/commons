package io.onedev.commons.jsymbol.golang;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Joiner;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.golang.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.golang.symbols.GolangSymbol;
import io.onedev.commons.jsymbol.golang.symbols.PackageSymbol;
import io.onedev.commons.jsymbol.golang.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.golang.symbols.VariableSymbol;

public class GolangExtractorTest extends DescriptableExtractorTest<GolangSymbol> {

	@Test
	public void test() throws IOException {
		var symbols = new GolangExtractor().extract(null, readFile("test.source"));
		List<String> lines = new ArrayList<>();
		for (var symbol: symbols) {
			if (symbol.getParent() == null)
				lines.addAll(describe(symbols, symbol));
		}
		Assert.assertEquals(
			Joiner.on("\n").join(readFile("test.outline")), 
			Joiner.on("\n").join(lines));
		
		symbols = new GolangExtractor().extract(null, readFile("test2.source"));
		lines = new ArrayList<>();
		for (var symbol: symbols) {
			if (symbol.getParent() == null)
				lines.addAll(describe(symbols, symbol));
		}
		Assert.assertEquals(
			Joiner.on("\n").join(readFile("test2.outline")), 
			Joiner.on("\n").join(lines));
	}
	
	@Override
	protected List<String> describe(List<GolangSymbol> context, GolangSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof PackageSymbol) {
			PackageSymbol packageSymbol = (PackageSymbol) symbol;
			builder.append("package ").append(packageSymbol.getName());
		} else if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			builder.append("func ");
			if (functionSymbol.getReceiver() != null)
				builder.append("(").append(functionSymbol.getReceiver()).append(") ");
			builder.append(functionSymbol.getName()).append(functionSymbol.getFunctionParams());
			if (functionSymbol.getReturnType() != null)
				builder.append(" ").append(functionSymbol.getReturnType());
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
