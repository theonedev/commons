package io.onedev.commons.jsymbol.php;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.php.symbols.ConstantSymbol;
import io.onedev.commons.jsymbol.php.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.php.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.php.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.php.symbols.PhpSymbol;
import io.onedev.commons.jsymbol.php.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.php.symbols.VariableSymbol;

public class PhpExtractorTest extends DescriptableExtractorTest<PhpSymbol> {

	@Test
	public void test() {
		verify(readFile("test.outline"), new PhpExtractor().extract(null, readFile("test.source")));
	}
	
	@Override
	protected List<String> describe(List<PhpSymbol> context, PhpSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			builder.append(functionSymbol.getVisibility().name().toLowerCase()).append(" ");
			builder.append("function ").append(functionSymbol.getName()).append(functionSymbol.getParams());
			if (functionSymbol.getReturnType() != null)
				builder.append(functionSymbol.getReturnType());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			builder.append(variableSymbol.getVisibility().name().toLowerCase()).append(" ");
			builder.append(variableSymbol.getName());
		} else if (symbol instanceof TypeSymbol) {
			TypeSymbol typeSymbol = (TypeSymbol) symbol;
			builder.append(typeSymbol.getKind().name().toLowerCase()).append(" ");
			builder.append(typeSymbol.getName());
		} else if (symbol instanceof ConstantSymbol) {
			ConstantSymbol constantSymbol = (ConstantSymbol) symbol;
			builder.append(constantSymbol.getVisibility().name().toLowerCase()).append(" ");
			builder.append("constant ").append(constantSymbol.getName());
		} else if (symbol instanceof NamespaceSymbol) {
			NamespaceSymbol namespaceSymbol = (NamespaceSymbol) symbol;
			builder.append("namespace ").append(namespaceSymbol.getName());
		} else if (symbol instanceof ImportedSymbol) {
			ImportedSymbol importedSymbol = (ImportedSymbol) symbol;
			builder.append("use ... as ").append(importedSymbol.getName());
		} else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		
		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
