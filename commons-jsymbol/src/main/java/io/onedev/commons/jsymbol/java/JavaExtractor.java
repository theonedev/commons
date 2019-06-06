package io.onedev.commons.jsymbol.java;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
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

	@Override
	public List<JavaSymbol> extract(String fileName, String fileContent) {
		List<JavaSymbol> symbols = new ArrayList<>();

		CompilationUnitSymbol symbol;		
		CompilationUnit compilationUnit;
		try {
			compilationUnit = JavaParser.parse(fileContent);
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
				typeDeclaration.getModifiers());
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
				typeParams, constructorDeclaration.getModifiers());
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
				typeParams, methodDeclaration.getModifiers());
		symbols.add(symbol);
	}
	
	private void processAnnotationMemberDeclaration(AnnotationMemberDeclaration annotationMemberDeclaration, TypeSymbol parent, 
			List<JavaSymbol> symbols) {
		String methodName = annotationMemberDeclaration.getNameAsString();
		PlanarRange position = getPosition(annotationMemberDeclaration.getName());
		PlanarRange scope = getPosition(annotationMemberDeclaration);
		String type = annotationMemberDeclaration.getType().toString();
		MethodSymbol symbol = new MethodSymbol(parent, methodName, position, scope, type, null, null, 
				annotationMemberDeclaration.getModifiers());
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
					fieldDeclaration.getModifiers());
			symbols.add(symbol);
		}
	}
	
	private void processEnumConstantDeclaration(EnumConstantDeclaration enumConstantDeclaration, TypeSymbol parent, 
			List<JavaSymbol> symbols) {
		String fieldName = enumConstantDeclaration.getNameAsString();
		PlanarRange position = getPosition(enumConstantDeclaration.getName());
		PlanarRange scope = getPosition(enumConstantDeclaration);
		FieldSymbol symbol = new FieldSymbol(parent, fieldName, position, scope, null, 
				EnumSet.noneOf(Modifier.class));
		symbols.add(symbol);
	}
	
	private PlanarRange getPosition(Node node) {
		return new PlanarRange(node.getBegin().get().line-1, node.getBegin().get().column-1, 
				node.getEnd().get().line-1, node.getEnd().get().column);
	}
	
	@Override
	public int getVersion() {
		return 2;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "java");
	}
	
}

