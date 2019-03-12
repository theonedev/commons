package io.onedev.commons.jsymbol.cpp;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.onedev.commons.jsymbol.DescriptableExtractorTest;
import io.onedev.commons.jsymbol.cpp.CppExtractor;
import io.onedev.commons.jsymbol.cpp.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.CppSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.EnumSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.HeaderFileSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.MacroSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.MemberSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.SourceFileSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.SpecialFunctionSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.SpecialVariableSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.StructSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.TypedefSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.UnionSymbol;
import io.onedev.commons.jsymbol.cpp.symbols.VariableSymbol;

public class CPPExtractorTest extends DescriptableExtractorTest<CppSymbol> {
 
	@Test
	public void testAnonymous(){
		verify(readFile("anonymous.outline"),new CppExtractor().extract(null, readFile("anonymous.source")));
	}
	@Test
	public void testTest1(){
		verify(readFile("testSpecialDeclaration.outline"),new CppExtractor().extract(null, readFile("testSpecialDeclaration.source")));
	}
	@Test
	public void test() {
		verify(readFile("test.outline"), new CppExtractor().extract(null, readFile("test.source")));
	}
	@Test
	public void testSpecial() {
		verify(readFile("special.outline"), new CppExtractor().extract(null, readFile("special.source")));
	}
	@Test
	public void testLocal(){
		verify(readFile("testLocal.outline"), new CppExtractor().extract(null, readFile("testLocal.source")));
	}
	@Test
	public void testHeaderLocal(){
		verify(readFile("testHeaderLocal.outline"), new CppExtractor().extract("test.hpp", readFile("testHeaderLocal.source")));
	}
	@Test
	public void testSourceLocal(){
		verify(readFile("testSourceLocal.outline"), new CppExtractor().extract("test.cpp", readFile("testSourceLocal.source")));
	}
	@Test
	public void testClass() {
		verify(readFile("class.outline"), new CppExtractor().extract(null, readFile("class.source")));
	}
	@Test
	public void testEnum() {
		verify(readFile("enum.outline"), new CppExtractor().extract(null, readFile("enum.source")));
	}
	@Test
	public void testError_tolerance() {
		verify(readFile("error_tolerance.outline"), new CppExtractor().extract(null, readFile("error_tolerance.source")));
	}
	@Test
	public void testMacro() {
		verify(readFile("macro.outline"), new CppExtractor().extract(null, readFile("macro.source")));
	}
	@Test
	public void testNamespace() {
		verify(readFile("namespace.outline"), new CppExtractor().extract(null, readFile("namespace.source")));
	}
	@Test
	public void testStruct() {
		verify(readFile("struct.outline"), new CppExtractor().extract(null, readFile("struct.source")));
	}
	@Test
	public void testTypedef() {
		verify(readFile("typedef.outline"), new CppExtractor().extract(null, readFile("typedef.source")));
	}
	@Test
	public void testUnion() {
		verify(readFile("union.outline"), new CppExtractor().extract(null, readFile("union.source")));
	}

	@Override
	protected List<String> describe(List<CppSymbol> context, CppSymbol symbol) {
		List<String> lines = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if(symbol.isLocal()){
			builder.append("local ");
		}
		if (symbol instanceof EnumSymbol) {
			EnumSymbol enumSymbol = (EnumSymbol) symbol;
			builder.append("enum ").append(enumSymbol.getName());
		} else if (symbol instanceof FunctionSymbol) {
			FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
			if (functionSymbol.getType() != null) 
				builder.append(functionSymbol.getType()).append(" ");
			builder.append(functionSymbol.getName());
			builder.append("(");
			if (functionSymbol.getParams() != null) 
				builder.append(functionSymbol.getParams());
			builder.append(")");
			if (functionSymbol.isDefinition())
				builder.append("{...}");
		} else if (symbol instanceof HeaderFileSymbol) {
			HeaderFileSymbol headerFileSymbol = (HeaderFileSymbol) symbol;
			builder.append(headerFileSymbol.getName());
		} else if (symbol instanceof SourceFileSymbol) {
			SourceFileSymbol sourceFileSymbol = (SourceFileSymbol) symbol;
			builder.append(sourceFileSymbol.getName());
		} else if (symbol instanceof MacroSymbol) {
			MacroSymbol macroSymbol = (MacroSymbol) symbol;
			builder.append("macro ").append(macroSymbol.getName());
		} else if (symbol instanceof MemberSymbol) {
			MemberSymbol memberSymbol = (MemberSymbol) symbol;
			if (memberSymbol.getType() != null)
				builder.append(memberSymbol.getType()).append(" ");
			builder.append(memberSymbol.getName());
		} else if (symbol instanceof StructSymbol) {
			StructSymbol structSymbol = (StructSymbol) symbol;
			builder.append("struct ").append(structSymbol.getName());
		} else if (symbol instanceof TypedefSymbol) {
			TypedefSymbol typedefSymbol = (TypedefSymbol) symbol;
			builder.append("typedef ").append(typedefSymbol.getType()).append(" ").append(typedefSymbol.getName());
		} else if (symbol instanceof UnionSymbol) {
			UnionSymbol unionSymbol = (UnionSymbol) symbol;
			builder.append("union ").append(unionSymbol.getName());
		} else if (symbol instanceof VariableSymbol) {
			VariableSymbol variableSymbol = (VariableSymbol) symbol;
			builder.append(variableSymbol.getType()).append(" ").append(variableSymbol.getName());
		}else if(symbol instanceof ClassSymbol){
			ClassSymbol classSymbol= (ClassSymbol) symbol;
			builder.append("class ").append(classSymbol.getName());
		}else if(symbol instanceof NamespaceSymbol){
			NamespaceSymbol namespace = (NamespaceSymbol) symbol;
			builder.append("namespace ").append(namespace.getName());
	    }else if(symbol instanceof SpecialVariableSymbol){
			SpecialVariableSymbol specialVariable = (SpecialVariableSymbol)symbol;
			builder.append(specialVariable.getName());
		}else if(symbol instanceof SpecialFunctionSymbol){
			SpecialFunctionSymbol specialFunction = (SpecialFunctionSymbol) symbol;
			if(specialFunction.getModifier() == CppSymbol.Modifier.MACRO){
				builder.append("macro ");
			}
			builder.append(specialFunction.getName());
			builder.append("(");
			if (specialFunction.getParams() != null) 
				builder.append(specialFunction.getParams());
			builder.append(")");
			if (specialFunction.isDefinition())
				builder.append("{...}");
		}
		else {
			throw new RuntimeException("Unexpected symbol type: " + symbol.getClass());
		}
		lines.add(builder.toString());
		appendChildren(lines, context, symbol);
		return lines;
	}

}