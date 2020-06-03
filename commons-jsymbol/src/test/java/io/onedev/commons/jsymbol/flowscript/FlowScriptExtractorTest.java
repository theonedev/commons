package io.onedev.commons.jsymbol.flowscript;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.SymbolExtractor;
import io.onedev.commons.jsymbol.flowscript.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.FlowScriptSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.MethodAccess;
import io.onedev.commons.jsymbol.flowscript.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.flowscript.symbols.ModuleAccess;
import io.onedev.commons.jsymbol.flowscript.symbols.ObjectSymbol;

public class FlowScriptExtractorTest extends DescriptableExtractorTest<FlowScriptSymbol>{
	@Test
	public void test() {
		SymbolExtractor<FlowScriptSymbol> extractor = new FlowScriptExtractor();
		verify(readFile("object-literal.outline"), extractor.extract(null, readFile("object-literal.source")));
		verify(readFile("variables.outline"), extractor.extract(null, readFile("variables.source")));
		verify(readFile("module.outline"), extractor.extract(null, readFile("module.source")));
		verify(readFile("class.outline"), extractor.extract(null, readFile("class.source")));
		verify(readFile("jquery.outline"), extractor.extract(null, readFile("jquery.source")));
		verify(readFile("commonjs.outline"), extractor.extract(null, readFile("commonjs.source")));
		verify(readFile("vue.outline"), extractor.extract(null, readFile("vue.source")));
	    verify(readFile("typeAnnotation.outline"), extractor.extract(null, readFile("typeAnnotation.source")));
	    verify(readFile("vueJS.outline"), extractor.extract(null, readFile("vueJS.source")));
	}

	@Override
	protected List<String> describe(List<FlowScriptSymbol> context, FlowScriptSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (symbol.getModuleAccess() == ModuleAccess.EXPORT)
			builder.append("export ");
		if (symbol.isLocal())
		    builder.append("local ");
		if (symbol instanceof MethodSymbol) {
			MethodSymbol methodSymbol = (MethodSymbol) symbol;
			if (methodSymbol.getMethodAccess() == MethodAccess.GET)
				builder.append("get ").append(methodSymbol.getName()).append(methodSymbol.getParameters());
			else if (methodSymbol.getMethodAccess() == MethodAccess.SET)
				builder.append("set ").append(methodSymbol.getName()).append(methodSymbol.getParameters());
			else
				builder.append(methodSymbol.getName()).append(methodSymbol.getParameters());
		} else if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			if (functionSymbol.getName() != null)
				builder.append("function ").append(functionSymbol.getName()).append(functionSymbol.getParameters());
			else
				builder.append("function").append(functionSymbol.getParameters());
		} else if (symbol instanceof ClassSymbol) {
			ClassSymbol classSymbol = (ClassSymbol) symbol;
			builder.append("class ").append(classSymbol.getName());
		} else {
			ObjectSymbol object = (ObjectSymbol) symbol;
			builder.append(object.getName());
		}
		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}
