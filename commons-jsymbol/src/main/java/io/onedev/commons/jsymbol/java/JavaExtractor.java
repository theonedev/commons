package io.onedev.commons.jsymbol.java;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.type.TypeParameter;
import com.google.common.base.Joiner;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.java.symbols.CompilationUnitSymbol;
import io.onedev.commons.jsymbol.java.symbols.FieldSymbol;
import io.onedev.commons.jsymbol.java.symbols.JavaSymbol;
import io.onedev.commons.jsymbol.java.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.java.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.java.symbols.TypeSymbol.Kind;

public class JavaExtractor extends AbstractSymbolExtractor<JavaSymbol> {

	private static final ParserConfiguration PARSER_CONFIGURATION = new ParserConfiguration()
			.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
	
	@Override
	public List<JavaSymbol> extract(String fileName, String fileContent) {
		List<JavaSymbol> symbols = new ArrayList<>();

		CompilationUnitSymbol symbol;		
		CompilationUnit compilationUnit;
		try {
			ParseResult<CompilationUnit> parseResult = new JavaParser(PARSER_CONFIGURATION).parse(skipMethodBodies(fileContent));
			if (!parseResult.isSuccessful())
				throw new ParseProblemException(parseResult.getProblems());
			compilationUnit = parseResult.getResult().orElseThrow();
		} catch (ParseProblemException e) {
			throw new RuntimeException("Error parsing java", e);
		}
		Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();
		if (packageDeclaration.isPresent()) {
			symbol = new CompilationUnitSymbol(packageDeclaration.get().getNameAsString(), 
					getPosition(packageDeclaration.get().getName()), getPosition(packageDeclaration.get()));
			symbols.add(symbol);
		} else {
			symbol = null;
		}
		
		for (TypeDeclaration<?> typeDeclaration: compilationUnit.getTypes()) {
			processTypeDeclaration(typeDeclaration, symbol, symbols);
		}
		return symbols;
	}
	
	private void processTypeDeclaration(TypeDeclaration<?> typeDeclaration, JavaSymbol parent, List<JavaSymbol> symbols) {
		TypeSymbol.Kind kind;
		String typeParameters = null;
		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) { 
			ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;
			kind = classOrInterfaceDeclaration.isInterface()?Kind.INTERFACE:Kind.CLASS;
			List<String> listOfTypeParameterDesc = new ArrayList<>();
			for (TypeParameter typeParameter: classOrInterfaceDeclaration.getTypeParameters()) {
				listOfTypeParameterDesc.add(typeParameter.toString());
			}
			if (!listOfTypeParameterDesc.isEmpty()) {
				typeParameters = "<" + Joiner.on(", ").join(listOfTypeParameterDesc) + ">";
			}
		} else if (typeDeclaration instanceof AnnotationDeclaration) {
			kind = Kind.ANNOTATION;
		} else if (typeDeclaration instanceof EnumDeclaration) {
			kind = Kind.ENUM;
		} else {
			throw new RuntimeException("Unexpected type declaration: " + typeDeclaration.getClass());
		}
		
		TypeSymbol symbol = new TypeSymbol(parent, typeDeclaration.getNameAsString(), 
				getPosition(typeDeclaration.getName()), getPosition(typeDeclaration), kind, typeParameters, 
				getModifierKeywords(typeDeclaration.getModifiers()));
		symbols.add(symbol);

		for (Node child: typeDeclaration.getChildNodes()) {
			if (child instanceof TypeDeclaration) {
				processTypeDeclaration((TypeDeclaration<?>) child, symbol, symbols);
			} else if (child instanceof MethodDeclaration) {
				processMethodDeclaration((MethodDeclaration) child, symbol, symbols);
			} else if (child instanceof FieldDeclaration) {
				processFieldDeclaration((FieldDeclaration) child, symbol, symbols);
			} else if (child instanceof ConstructorDeclaration) {
				processConstructorDeclaration((ConstructorDeclaration) child, symbol, symbols);
			} else if (child instanceof EnumConstantDeclaration) {
				processEnumConstantDeclaration((EnumConstantDeclaration) child, symbol, symbols);
			} else if (child instanceof AnnotationMemberDeclaration) {
				processAnnotationMemberDeclaration((AnnotationMemberDeclaration) child, symbol, symbols);
			}
		}
		
	}

	private void processConstructorDeclaration(ConstructorDeclaration constructorDeclaration, TypeSymbol parent, 
			List<JavaSymbol> symbols) {
		String methodName = constructorDeclaration.getNameAsString();
		PlanarRange position = getPosition(constructorDeclaration.getName());
		PlanarRange scope = getPosition(constructorDeclaration);
		String methodParams;
		if (constructorDeclaration.getParameters().isEmpty()) {
			methodParams = null;
		} else {
			List<String> listOfMethodParamDesc = new ArrayList<>();
			for (Parameter param: constructorDeclaration.getParameters()) {
				if (param.isVarArgs())
					listOfMethodParamDesc.add(param.getType().toString() + "...");
				else
					listOfMethodParamDesc.add(param.getType().toString());
			}
			methodParams = Joiner.on(", ").join(listOfMethodParamDesc);
		}
		String typeParams;
		if (constructorDeclaration.getTypeParameters().isEmpty()) {
			typeParams = null;
		} else {
			List<String> listOfTypeParamDesc = new ArrayList<>();
			for (TypeParameter param: constructorDeclaration.getTypeParameters()) {
				listOfTypeParamDesc.add(param.toString());
			}
			typeParams = "<" + Joiner.on(", ").join(listOfTypeParamDesc) + ">";
		}
		MethodSymbol symbol = new MethodSymbol(parent, methodName, position, scope, null, methodParams, 
				typeParams, getModifierKeywords(constructorDeclaration.getModifiers()));
		symbols.add(symbol);
	}

	private void processMethodDeclaration(MethodDeclaration methodDeclaration, TypeSymbol parent, 
			List<JavaSymbol> symbols) {
		String methodName = methodDeclaration.getNameAsString();
		PlanarRange position = getPosition(methodDeclaration.getName());
		PlanarRange scope = getPosition(methodDeclaration);
		String type = methodDeclaration.getType().toString();
		String methodParams;
		if (methodDeclaration.getParameters().isEmpty()) {
			methodParams = null;
		} else {
			List<String> listOfMethodParamDesc = new ArrayList<>();
			for (Parameter param: methodDeclaration.getParameters()) {
				if (param.isVarArgs())
					listOfMethodParamDesc.add(param.getType().toString() + "...");
				else
					listOfMethodParamDesc.add(param.getType().toString());
			}
			methodParams = Joiner.on(", ").join(listOfMethodParamDesc);
		}
		String typeParams;
		if (methodDeclaration.getTypeParameters().isEmpty()) {
			typeParams = null;
		} else {
			List<String> listOfTypeParamDesc = new ArrayList<>();
			for (TypeParameter param: methodDeclaration.getTypeParameters()) {
				listOfTypeParamDesc.add(param.toString());
			}
			typeParams = "<" + Joiner.on(", ").join(listOfTypeParamDesc) + ">";
		}
		MethodSymbol symbol = new MethodSymbol(parent, methodName, position, scope, type, methodParams, 
				typeParams, getModifierKeywords(methodDeclaration.getModifiers()));
		symbols.add(symbol);
	}
	
	private void processAnnotationMemberDeclaration(AnnotationMemberDeclaration annotationMemberDeclaration, TypeSymbol parent, 
			List<JavaSymbol> symbols) {
		String methodName = annotationMemberDeclaration.getNameAsString();
		PlanarRange position = getPosition(annotationMemberDeclaration.getName());
		PlanarRange scope = getPosition(annotationMemberDeclaration);
		String type = annotationMemberDeclaration.getType().toString();
		MethodSymbol symbol = new MethodSymbol(parent, methodName, position, scope, type, null, null, 
				getModifierKeywords(annotationMemberDeclaration.getModifiers()));
		symbols.add(symbol);
	}
	
	private void processFieldDeclaration(FieldDeclaration fieldDeclaration, TypeSymbol parent, 
			List<JavaSymbol> symbols) {
		for (VariableDeclarator variableDeclarator: fieldDeclaration.getVariables()) {
			String fieldName = variableDeclarator.getNameAsString();
			PlanarRange position = getPosition(variableDeclarator.getName());
			PlanarRange scope = getPosition(variableDeclarator);
			String type = variableDeclarator.getType().toString();
			FieldSymbol symbol = new FieldSymbol(parent, fieldName, position, scope, type, 
					getModifierKeywords(fieldDeclaration.getModifiers()));
			symbols.add(symbol);
		}
	}
	
	private void processEnumConstantDeclaration(EnumConstantDeclaration enumConstantDeclaration, TypeSymbol parent, 
			List<JavaSymbol> symbols) {
		String fieldName = enumConstantDeclaration.getNameAsString();
		PlanarRange position = getPosition(enumConstantDeclaration.getName());
		PlanarRange scope = getPosition(enumConstantDeclaration);
		FieldSymbol symbol = new FieldSymbol(parent, fieldName, position, scope, null, 
				EnumSet.noneOf(Keyword.class));
		symbols.add(symbol);
	}
	
	private EnumSet<Keyword> getModifierKeywords(NodeList<Modifier> modifiers) {
		EnumSet<Keyword> modifierKeywords = EnumSet.noneOf(Keyword.class);
		for (Modifier modifier: modifiers)
			modifierKeywords.add(modifier.getKeyword());
		return modifierKeywords;
	}
	
	private PlanarRange getPosition(Node node) {
		return new PlanarRange(node.getBegin().get().line-1, node.getBegin().get().column-1, 
				node.getEnd().get().line-1, node.getEnd().get().column);
	}

	private String skipMethodBodies(String source) {
		char[] chars = source.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '\'' || chars[i] == '"') {
				i = skipQuoted(source, i);
			} else if (chars[i] == '/' && i + 1 < chars.length) {
				if (chars[i + 1] == '/') {
					i = skipLineComment(source, i);
				} else if (chars[i + 1] == '*') {
					i = skipBlockComment(source, i);
				}
			} else if (chars[i] == '{' && isMethodBodyOpeningBrace(source, i)) {
				int close = findMatchingBrace(source, i);
				if (close != -1) {
					blank(chars, i + 1, close);
					i = close;
				}
			}
		}
		return new String(chars);
	}

	private boolean isMethodBodyOpeningBrace(String source, int brace) {
		int previous = previousNonWhitespace(source, brace - 1);
		if (previous != -1 && source.charAt(previous) == '>') {
			int before = previousNonWhitespace(source, previous - 1);
			if (before != -1 && source.charAt(before) == '-')
				return false;
		}

		int closeParen = findLastMatchingCloseParen(source, brace);
		if (closeParen == -1)
			return false;
		int openParen = findMatchingParen(source, closeParen);
		if (openParen == -1)
			return false;

		String name = readPreviousIdentifier(source, openParen - 1);
		if (name == null)
			return false;
		int nameStart = previousNonWhitespace(source, openParen - 1) - name.length() + 1;
		int beforeName = previousNonWhitespace(source, nameStart - 1);
		if (beforeName != -1 && source.charAt(beforeName) == '@')
			return false;

		String afterParameters = source.substring(closeParen + 1, brace);
		if (afterParameters.contains("default"))
			return false;
		if (containsKeyword(afterParameters, "class") || containsKeyword(afterParameters, "interface")
				|| containsKeyword(afterParameters, "enum") || containsKeyword(afterParameters, "record"))
			return false;

		String previousIdentifier = readPreviousIdentifier(source, previousIdentifierEnd(source, openParen - 1, name));
		if ("if".equals(name) || "for".equals(name) || "while".equals(name) || "switch".equals(name)
				|| "catch".equals(name) || "synchronized".equals(name) || "new".equals(previousIdentifier)
				|| "record".equals(previousIdentifier) || "class".equals(previousIdentifier)
				|| "interface".equals(previousIdentifier) || "enum".equals(previousIdentifier)) {
			return false;
		}
		return true;
	}

	private int findLastMatchingCloseParen(String source, int brace) {
		for (int i = brace - 1; i >= 0; i--) {
			char ch = source.charAt(i);
			if (ch == ')')
				return i;
			else if (ch == ';' || ch == '{' || ch == '}' || ch == '(' || ch == '=' || ch == '/')
				return -1;
		}
		return -1;
	}

	private boolean containsKeyword(String source, String keyword) {
		int index = -1;
		while ((index = source.indexOf(keyword, index + 1)) != -1) {
			int before = index - 1;
			int after = index + keyword.length();
			if ((before < 0 || !Character.isJavaIdentifierPart(source.charAt(before)))
					&& (after >= source.length() || !Character.isJavaIdentifierPart(source.charAt(after)))) {
				return true;
			}
		}
		return false;
	}

	private int findMatchingParen(String source, int closeParen) {
		int depth = 0;
		for (int i = closeParen; i >= 0; i--) {
			char ch = source.charAt(i);
			if (ch == ')') {
				depth++;
			} else if (ch == '(') {
				depth--;
				if (depth == 0)
					return i;
			}
		}
		return -1;
	}

	private String readPreviousIdentifier(String source, int index) {
		int end = previousNonWhitespace(source, index);
		if (end == -1 || !Character.isJavaIdentifierPart(source.charAt(end)))
			return null;
		int start = end;
		while (start > 0 && Character.isJavaIdentifierPart(source.charAt(start - 1)))
			start--;
		return source.substring(start, end + 1);
	}

	private int previousIdentifierEnd(String source, int index, String identifier) {
		int end = previousNonWhitespace(source, index);
		return end - identifier.length();
	}

	private int previousNonWhitespace(String source, int index) {
		for (int i = index; i >= 0; i--) {
			if (!Character.isWhitespace(source.charAt(i)))
				return i;
		}
		return -1;
	}

	private int findMatchingBrace(String source, int openBrace) {
		int depth = 1;
		for (int i = openBrace + 1; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '\'' || ch == '"') {
				i = skipQuoted(source, i);
			} else if (ch == '/' && i + 1 < source.length()) {
				if (source.charAt(i + 1) == '/') {
					i = skipLineComment(source, i);
				} else if (source.charAt(i + 1) == '*') {
					i = skipBlockComment(source, i);
				}
			} else if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0)
					return i;
			}
		}
		return -1;
	}

	private int skipQuoted(String source, int quote) {
		if (source.charAt(quote) == '"' && quote + 2 < source.length()
				&& source.charAt(quote + 1) == '"' && source.charAt(quote + 2) == '"') {
			int i = quote + 3;
			while (i + 2 < source.length()) {
				if (source.charAt(i) == '"' && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"')
					return i + 2;
				i++;
			}
			return source.length() - 1;
		} else {
			for (int i = quote + 1; i < source.length(); i++) {
				if (source.charAt(i) == '\\') {
					i++;
				} else if (source.charAt(i) == source.charAt(quote)) {
					return i;
				} else if (source.charAt(i) == '\n' || source.charAt(i) == '\r') {
					return i;
				}
			}
			return source.length() - 1;
		}
	}

	private int skipLineComment(String source, int slash) {
		for (int i = slash + 2; i < source.length(); i++) {
			if (source.charAt(i) == '\n' || source.charAt(i) == '\r')
				return i;
		}
		return source.length() - 1;
	}

	private int skipBlockComment(String source, int slash) {
		for (int i = slash + 2; i + 1 < source.length(); i++) {
			if (source.charAt(i) == '*' && source.charAt(i + 1) == '/')
				return i + 1;
		}
		return source.length() - 1;
	}

	private void blank(char[] chars, int from, int to) {
		for (int i = from; i < to; i++) {
			if (chars[i] != '\n' && chars[i] != '\r')
				chars[i] = ' ';
		}
	}
	
	@Override
	public int getVersion() {
		return 7;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "java");
	}
	
}
