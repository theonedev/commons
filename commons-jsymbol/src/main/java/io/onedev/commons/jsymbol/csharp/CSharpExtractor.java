package io.onedev.commons.jsymbol.csharp;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.csharp.CSharpParser.*;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.csharp.symbols.CSharpSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.FieldSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.csharp.symbols.TypeSymbol.Kind;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.jsymbol.util.Utils;

public class CSharpExtractor extends AbstractSymbolExtractor<CSharpSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(CSharpExtractor.class);
	
	@Override
	public List<CSharpSymbol> extract(String fileName, String fileContent) {
		List<CSharpSymbol> symbols = new ArrayList<>();
		TokenStream tokenStream = new CommonTokenStream(new ListTokenSource(preprocess(fileContent)));
		CSharpParser parser = new CSharpParser(tokenStream);
		parser.removeErrorListeners();
		parser.addErrorListener(newErrorListener());
		clearParserCache(parser);

		Compilation_unitContext compilationUnit = parser.compilation_unit();
		var fileNamespace = compilationUnit.file_namespace();
		CSharpSymbol parentSymbol = null;
		if (fileNamespace != null) {
			parentSymbol = extractNamespaceNames(fileNamespace.qualified_identifier(),
					Utils.getTextRange(fileNamespace), symbols, null);
		}

		extract(compilationUnit.namespace_member_declarations(), symbols, parentSymbol, fileContent);
		return symbols;
	}

	private CSharpSymbol extractNamespaceNames(Qualified_identifierContext qualifiedIdentifier, PlanarRange scope,
									   List<CSharpSymbol> symbols, @Nullable CSharpSymbol parentSymbol) {
		for (IdentifierContext identifier: qualifiedIdentifier.identifier()) {
			String namespaceName = getText(identifier);
			NamespaceSymbol namespaceSymbol = null;
			for (CSharpSymbol symbol: symbols) {
				if (symbol instanceof NamespaceSymbol
						&& symbol.getParent() == parentSymbol
						&& symbol.getName().equals(namespaceName)) {
					namespaceSymbol = (NamespaceSymbol) symbol;
					break;
				}
			}
			if (namespaceSymbol == null) {
				PlanarRange position = Utils.getTextRange(identifier);
				namespaceSymbol = new NamespaceSymbol(parentSymbol, namespaceName, position, scope);
				symbols.add(namespaceSymbol);
			}
			parentSymbol = namespaceSymbol;
		}
		return parentSymbol;
	}

	private void extract(@Nullable Namespace_member_declarationsContext namespaceMemberDeclarations, 
			List<CSharpSymbol> symbols, @Nullable CSharpSymbol parentSymbol, String source) {
		if (namespaceMemberDeclarations != null) {
			for (Namespace_member_declarationContext namespaceMemberDeclaration: 
					namespaceMemberDeclarations.namespace_member_declaration()) {
				if (namespaceMemberDeclaration.namespace_declaration() != null) {
					Namespace_declarationContext namespaceDeclaration = namespaceMemberDeclaration.namespace_declaration();
					CSharpSymbol newParentSymbol = extractNamespaceNames(namespaceDeclaration.qualified_identifier(),
							Utils.getTextRange(namespaceDeclaration), symbols, parentSymbol);
					extract(namespaceDeclaration.namespace_body().namespace_member_declarations(), symbols,
							newParentSymbol, source);
				} else {
					Type_declarationContext typeDeclaration = namespaceMemberDeclaration.type_declaration();
					EnumSet<CSharpSymbol.Modifier> modifiers = getModifiers(typeDeclaration.all_member_modifiers());
					if (typeDeclaration.class_definition() != null) {
						extract(typeDeclaration.class_definition(), symbols, parentSymbol, modifiers, source);
					} else if (typeDeclaration.delegate_definition() != null) {
						extract(typeDeclaration.delegate_definition(), symbols, parentSymbol, modifiers);
					} else if (typeDeclaration.interface_definition() != null) {
						extract(typeDeclaration.interface_definition(), symbols, parentSymbol, modifiers);
					} else if (typeDeclaration.struct_definition() != null) {
						extract(typeDeclaration.struct_definition(), symbols, parentSymbol, modifiers, source);
					} else if (typeDeclaration.enum_definition() != null) {
						extract(typeDeclaration.enum_definition(), symbols, parentSymbol, modifiers);
					} 
				}
			}
		}
	}
	
	private void extract(Method_declarationContext methodDeclaration, List<CSharpSymbol> symbols, 
			CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers, String type) {
		String methodName = getText(methodDeclaration.method_member_name());
		PlanarRange position = Utils.getTextRange(methodDeclaration.method_member_name());
		PlanarRange scope = Utils.getTextRange(methodDeclaration);
		String typeParams = getTypeParameters(methodDeclaration.type_parameter_list());
		String methodParams = getMethodParams(methodDeclaration.formal_parameter_list());
		symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.NORMAL_METHOD, methodName, position, scope, 
				typeParams, type, methodParams, null, modifiers));
	}
	
	private BaseErrorListener newErrorListener() {
		return new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				logger.debug("{}: {}: {}", line, charPositionInLine, msg);
			}
			
		};
	}
	
	private QualifiedName getQualifiedName(TypeContext type, String source) {
		ParserRuleContext unqualified;
		if (type.base_type().class_type() != null 
				&& type.base_type().class_type().namespace_or_type_name() != null) {
			Namespace_or_type_nameContext namespaceOrTypeName = type.base_type().class_type().namespace_or_type_name();
			if (!namespaceOrTypeName.identifier().isEmpty()) {
				unqualified = namespaceOrTypeName.identifier(namespaceOrTypeName.identifier().size()-1);
			} else {
				unqualified = namespaceOrTypeName.qualified_alias_member().identifier(1);
			}
		} else {
			unqualified = type.base_type();
		}

		String prefix = source.substring(type.start.getStartIndex(), unqualified.start.getStartIndex());
		if (prefix != null)
			prefix = StringUtils.remove(prefix, "@");
		if (prefix.length() == 0)
			prefix = null;
		String suffix = source.substring(unqualified.stop.getStopIndex()+1, type.stop.getStopIndex()+1);
		if (suffix != null)
			suffix = StringUtils.remove(suffix, "@");
		if (suffix.length() == 0)
			suffix = null;
		String unqualifiedName = getText(unqualified);
		return new QualifiedName(unqualifiedName, prefix, suffix);
	}
	
	private void extract(Common_member_declarationContext commonMemberDeclaration, List<CSharpSymbol> symbols, 
			CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers, String source) {
		if (commonMemberDeclaration.constant_declaration() != null) {
			Constant_declarationContext constantDeclaration = commonMemberDeclaration.constant_declaration();
			String type = getText(constantDeclaration.type());
			for (Constant_declaratorContext constantDeclarator: 
					constantDeclaration.constant_declarators().constant_declarator()) {
				IdentifierContext identifier = constantDeclarator.identifier();
				String fieldName = getText(identifier);
				PlanarRange position = Utils.getTextRange(identifier);
				PlanarRange scope = Utils.getTextRange(constantDeclarator);
				symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.NORMAL_FIELD, fieldName, position, scope, 
						type, null, modifiers));
			}
		} else if (commonMemberDeclaration.typed_member_declaration() != null) {
			Typed_member_declarationContext typedMemberDeclaration = commonMemberDeclaration.typed_member_declaration();
			String type = getText(typedMemberDeclaration.type());
			if (typedMemberDeclaration.indexer_declaration() != null) {
				Indexer_declarationContext indexerDeclaration = typedMemberDeclaration.indexer_declaration();
				String propertyName;
				if (typedMemberDeclaration.namespace_or_type_name() != null)
					propertyName = getText(typedMemberDeclaration.namespace_or_type_name()) + ".this";
				else
					propertyName = "this";
				
				String indexParams = "[" + getMethodParams(indexerDeclaration.formal_parameter_list()) + "]";
				PlanarRange position = Utils.getTextRange(indexerDeclaration.THIS().getSymbol());
				PlanarRange scope = Utils.getTextRange(typedMemberDeclaration);
				symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.PROPERTY, propertyName, position, scope, 
						type, indexParams, modifiers));
			} else if (typedMemberDeclaration.method_declaration() != null) {
				extract(typedMemberDeclaration.method_declaration(), symbols, parentSymbol, modifiers, type);
			} else if (typedMemberDeclaration.property_declaration() != null) {
				Property_declarationContext propertyDeclaration = typedMemberDeclaration.property_declaration();
				String propertyName = getText(propertyDeclaration.member_name());
				PlanarRange position = Utils.getTextRange(propertyDeclaration.member_name());
				PlanarRange scope = Utils.getTextRange(propertyDeclaration);
				symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.PROPERTY, propertyName, position, scope, 
						type, null, modifiers));
			} else if (typedMemberDeclaration.field_declaration() != null) {
				Field_declarationContext fieldDeclaration = typedMemberDeclaration.field_declaration();
				for (Variable_declaratorContext variableDeclarator: 
						fieldDeclaration.variable_declarators().variable_declarator()) {
					String fieldName = getText(variableDeclarator.identifier());
					PlanarRange position = Utils.getTextRange(variableDeclarator.identifier());
					PlanarRange scope = Utils.getTextRange(variableDeclarator);
					symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.NORMAL_FIELD, fieldName, position, scope, 
							type, null, modifiers));
				}
			} else if (typedMemberDeclaration.operator_declaration() != null) {
				Operator_declarationContext operatorDeclaration = typedMemberDeclaration.operator_declaration();
				String methodName = getText(operatorDeclaration.overloadable_operator());
				List<String> methodParamList = new ArrayList<>();
				for (Arg_declarationContext argDeclaration: operatorDeclaration.arg_declaration()) {
					methodParamList.add(getText(argDeclaration.type()));
				}
				String methodParams = Joiner.on(", ").join(methodParamList);
				PlanarRange position = Utils.getTextRange(operatorDeclaration.overloadable_operator());
				PlanarRange scope = Utils.getTextRange(operatorDeclaration);
				symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.OPERATOR, methodName, position, scope, 
						null, type, methodParams, "operator", modifiers));
			}
		} else if (commonMemberDeclaration.event_declaration() != null) {
			Event_declarationContext eventDeclaration = commonMemberDeclaration.event_declaration();
			String type = getText(eventDeclaration.type()) + "(...)";
			if (eventDeclaration.member_name() != null) {
				String eventName = getText(eventDeclaration.member_name());
				PlanarRange position = Utils.getTextRange(eventDeclaration.member_name());
				PlanarRange scope = Utils.getTextRange(eventDeclaration);
				symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.EVENT, eventName, position, scope, type, 
						null, modifiers));
			} else {
				for (Variable_declaratorContext variableDeclarator: 
						eventDeclaration.variable_declarators().variable_declarator()) {
					String eventName = getText(variableDeclarator.identifier());
					PlanarRange position = Utils.getTextRange(variableDeclarator.identifier());
					PlanarRange scope = Utils.getTextRange(variableDeclarator);
					symbols.add(new FieldSymbol(parentSymbol, FieldSymbol.Kind.EVENT, eventName, position, scope, type, 
							null, modifiers));
				}
			}
		} else if (commonMemberDeclaration.conversion_operator_declarator() != null) {
			Conversion_operator_declaratorContext conversionOperatorDeclarator = 
					commonMemberDeclaration.conversion_operator_declarator();
			String prefix = "operator";
			if (conversionOperatorDeclarator.IMPLICIT() != null)
				prefix = "implicit " + prefix;
			else  
				prefix = "explicit " + prefix;
			QualifiedName name = getQualifiedName(conversionOperatorDeclarator.type(), source);
			String methodParams = getText(conversionOperatorDeclarator.arg_declaration().type());
			PlanarRange position = Utils.getTextRange(conversionOperatorDeclarator.type());
			PlanarRange scope = Utils.getTextRange(commonMemberDeclaration);
			symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.OPERATOR, name, position, scope, 
					null, null, methodParams, prefix, modifiers));
		} else if (commonMemberDeclaration.constructor_declaration() != null) {
			Constructor_declarationContext constructorDeclaration = commonMemberDeclaration.constructor_declaration();
			PlanarRange position = Utils.getTextRange(constructorDeclaration.identifier());
			PlanarRange scope = Utils.getTextRange(constructorDeclaration);
			String name = getText(constructorDeclaration.identifier());
			String methodParams = getMethodParams(constructorDeclaration.formal_parameter_list());
			symbols.add(new MethodSymbol(parentSymbol, MethodSymbol.Kind.NORMAL_METHOD, name, position, scope, 
					null, null, methodParams, null, modifiers));
		} else if (commonMemberDeclaration.method_declaration() != null) {
			extract(commonMemberDeclaration.method_declaration(), symbols, parentSymbol, modifiers, "void");
		} else if (commonMemberDeclaration.class_definition() != null) {
			extract(commonMemberDeclaration.class_definition(), symbols, parentSymbol, modifiers, source);
		} else if (commonMemberDeclaration.struct_definition() != null) {
			extract(commonMemberDeclaration.struct_definition(), symbols, parentSymbol, modifiers, source);
		} else if (commonMemberDeclaration.interface_definition() != null) {
			extract(commonMemberDeclaration.interface_definition(), symbols, parentSymbol, modifiers);
		} else if (commonMemberDeclaration.enum_definition() != null) {
			extract(commonMemberDeclaration.enum_definition(), symbols, parentSymbol, modifiers);
		} else if (commonMemberDeclaration.delegate_definition() != null) {
			extract(commonMemberDeclaration.delegate_definition(), symbols, parentSymbol, modifiers);
		}
	}
	
	private void extract(Class_definitionContext classDefinition, List<CSharpSymbol> symbols, 
			@Nullable CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers, String source) {
		String typeName = getText(classDefinition.identifier());
		PlanarRange position = Utils.getTextRange(classDefinition.identifier());
		PlanarRange scope = Utils.getTextRange(classDefinition);
		String typeParams = getTypeParameters(classDefinition.type_parameter_list());
		TypeSymbol symbol = new TypeSymbol(parentSymbol, typeName, position, scope, Kind.CLASS, typeParams, modifiers);
		if (classDefinition.class_body().class_member_declarations() != null) {
			for (Class_member_declarationContext classMemberDeclaration: 
					classDefinition.class_body().class_member_declarations().class_member_declaration()) {
				EnumSet<CSharpSymbol.Modifier> memberModifiers = 
						getModifiers(classMemberDeclaration.all_member_modifiers());
				if (classMemberDeclaration.destructor_definition() != null) {
					Destructor_definitionContext destructorDefinition = 
							classMemberDeclaration.destructor_definition();
					PlanarRange memberPosition = Utils.getTextRange(destructorDefinition.identifier());
					PlanarRange memberScope = Utils.getTextRange(destructorDefinition);
					symbols.add(new MethodSymbol(symbol, MethodSymbol.Kind.NORMAL_METHOD, "~"+typeName, 
							memberPosition, memberScope, null, null, null, null, memberModifiers));
				} else {
					extract(classMemberDeclaration.common_member_declaration(), symbols, symbol, memberModifiers, 
							source);
				}
			}
		}
		symbols.add(symbol);
	}
	
	private void extract(Delegate_definitionContext delegateDefinition, List<CSharpSymbol> symbols, 
			@Nullable CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers) {
		String delegateName = getText(delegateDefinition.identifier());
		PlanarRange scope = Utils.getTextRange(delegateDefinition);
		PlanarRange position = Utils.getTextRange(delegateDefinition.identifier());
		String returnType = getReturnType(delegateDefinition.return_type());
		String typeParams = getTypeParameters(delegateDefinition.variant_type_parameter_list());
		String methodParams = getMethodParams(delegateDefinition.formal_parameter_list());
		MethodSymbol symbol = new MethodSymbol(parentSymbol, MethodSymbol.Kind.DELEGATE, delegateName, position, scope, 
				typeParams, returnType, methodParams, null, modifiers);
		symbols.add(symbol);
	}
	
	private void extract(Interface_definitionContext interfaceDefinition, List<CSharpSymbol> symbols, 
			@Nullable CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers) {
		String typeName = getText(interfaceDefinition.identifier());
		PlanarRange scope = Utils.getTextRange(interfaceDefinition);
		PlanarRange position = Utils.getTextRange(interfaceDefinition.identifier());
		String typeParams = getTypeParameters(interfaceDefinition.variant_type_parameter_list());
		TypeSymbol symbol = new TypeSymbol(parentSymbol, typeName, position, scope, Kind.INTERFACE, typeParams, 
				modifiers);
		symbols.add(symbol);
		
		EnumSet<CSharpSymbol.Modifier> memberModifiers = EnumSet.noneOf(CSharpSymbol.Modifier.class);
		for (Interface_member_declarationContext interfaceMemberDeclaration: 
				interfaceDefinition.interface_body().interface_member_declaration()) {
			PlanarRange memberScope = Utils.getTextRange(interfaceMemberDeclaration);
			if (interfaceMemberDeclaration.VOID() != null) {
				PlanarRange memberPosition = Utils.getTextRange(interfaceMemberDeclaration.identifier());
				String methodName = getText(interfaceMemberDeclaration.identifier());
				String memberTypeParams = getTypeParameters(interfaceMemberDeclaration.type_parameter_list());
				String methodParams = getMethodParams(interfaceMemberDeclaration.formal_parameter_list());
				symbols.add(new MethodSymbol(symbol, MethodSymbol.Kind.NORMAL_METHOD, methodName, memberPosition, 
						memberScope, memberTypeParams, "void", methodParams, null, memberModifiers));
			} else if (interfaceMemberDeclaration.EVENT() != null) {
				PlanarRange memberPosition = Utils.getTextRange(interfaceMemberDeclaration.identifier());
				String eventName = getText(interfaceMemberDeclaration.identifier());
				String memberType = getText(interfaceMemberDeclaration.type()) + "(...)";
				symbols.add(new FieldSymbol(symbol, FieldSymbol.Kind.EVENT, eventName, memberPosition, memberScope, 
						memberType, null, memberModifiers));
			} else {
				String memberType = getText(interfaceMemberDeclaration.type());
				if (interfaceMemberDeclaration.THIS() != null) {
					String indexParams = getMethodParams(interfaceMemberDeclaration.formal_parameter_list());
					PlanarRange propertyPosition = Utils.getTextRange(interfaceMemberDeclaration.THIS().getSymbol());
					symbols.add(new FieldSymbol(symbol, FieldSymbol.Kind.PROPERTY, "this", propertyPosition, 
							memberScope, memberType, indexParams, memberModifiers));
				} else if (interfaceMemberDeclaration.interface_accessors() != null) {
					String propertyName = getText(interfaceMemberDeclaration.identifier());
					PlanarRange propertyPosition = Utils.getTextRange(interfaceMemberDeclaration.identifier());
					symbols.add(new FieldSymbol(symbol, FieldSymbol.Kind.PROPERTY, propertyName, propertyPosition, 
							memberScope, memberType, null, memberModifiers));
				} else {
					PlanarRange memberPosition = Utils.getTextRange(interfaceMemberDeclaration.identifier());
					String methodName = getText(interfaceMemberDeclaration.identifier());
					String memberTypeParams = getTypeParameters(interfaceMemberDeclaration.type_parameter_list());
					String methodParams = getMethodParams(interfaceMemberDeclaration.formal_parameter_list());
					symbols.add(new MethodSymbol(symbol, MethodSymbol.Kind.NORMAL_METHOD, methodName, memberPosition, 
							memberScope, memberTypeParams, memberType, methodParams, null, memberModifiers));
				}
			}
		}
	}
	
	private void extract(Struct_definitionContext structDefinition, List<CSharpSymbol> symbols, 
			@Nullable CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers, String source) {
		String typeName = getText(structDefinition.identifier());
		PlanarRange position = Utils.getTextRange(structDefinition.identifier());
		PlanarRange scope = Utils.getTextRange(structDefinition);		
		String typeParams = getTypeParameters(structDefinition.type_parameter_list());
		TypeSymbol symbol = new TypeSymbol(parentSymbol, typeName, position, scope, Kind.STRUCT, typeParams, modifiers);
		for (Struct_member_declarationContext structMemberDeclaration: 
				structDefinition.struct_body().struct_member_declaration()) {
			EnumSet<CSharpSymbol.Modifier> memberModifiers = 
					getModifiers(structMemberDeclaration.all_member_modifiers());
			if (structMemberDeclaration.common_member_declaration() != null) {
				extract(structMemberDeclaration.common_member_declaration(), symbols, symbol, memberModifiers, source);
			} else {
				String memberType = getText(structMemberDeclaration.type()) + "*";
				for (Fixed_size_buffer_declaratorContext fixedSizeBufferDeclarator: 
						structMemberDeclaration.fixed_size_buffer_declarator()) {
					String memberName = getText(fixedSizeBufferDeclarator.identifier());
					PlanarRange memberPosition = Utils.getTextRange(fixedSizeBufferDeclarator.identifier());
					PlanarRange memberScope = Utils.getTextRange(fixedSizeBufferDeclarator);
					symbols.add(new FieldSymbol(symbol, FieldSymbol.Kind.NORMAL_FIELD, memberName, memberPosition, 
							memberScope, memberType, null, memberModifiers));
				}
			}
		}
		
		symbols.add(symbol);
	}
	
	private void extract(Enum_definitionContext enumDefinition, List<CSharpSymbol> symbols, 
			@Nullable CSharpSymbol parentSymbol, EnumSet<CSharpSymbol.Modifier> modifiers) {
		String typeName = getText(enumDefinition.identifier());
		PlanarRange position = Utils.getTextRange(enumDefinition.identifier());
		PlanarRange scope = Utils.getTextRange(enumDefinition);		
		TypeSymbol symbol = new TypeSymbol(parentSymbol, typeName, position, scope, Kind.ENUM, null, modifiers);
		for (Enum_member_declarationContext enumMemberDeclaration: 
				enumDefinition.enum_body().enum_member_declaration()) {
			String itemName = getText(enumMemberDeclaration.identifier());
			PlanarRange itemPosition = Utils.getTextRange(enumMemberDeclaration.identifier());
			symbols.add(new FieldSymbol(symbol, FieldSymbol.Kind.ENUM_ITEM, itemName, itemPosition, itemPosition, 
					null, null, EnumSet.noneOf(CSharpSymbol.Modifier.class)));
		}
		symbols.add(symbol);
	}
	
	private String getText(RuleContext context) {
		return StringUtils.remove(context.getText(), '@');
	}
	
	private List<String> getMethodParams(@Nullable Fixed_parametersContext fixedParameters) {
		List<String> methodParams = new ArrayList<>();
		if (fixedParameters != null) {
			for (Fixed_parameterContext fixedParameter: fixedParameters.fixed_parameter()) {
				StringBuilder builder = new StringBuilder();
				if (fixedParameter.parameter_modifier() != null)
					builder.append(fixedParameter.parameter_modifier().getText()).append(" ");
				if (fixedParameter.ARGLIST() != null) {
					builder.append(fixedParameter.ARGLIST().getText());
				} else {
					builder.append(getText(fixedParameter.arg_declaration().type()));
				}
				methodParams.add(builder.toString());
			}
		}
		return methodParams;
	}

	@Nullable 
	private String getMethodParams(@Nullable Formal_parameter_listContext formalParameterList) {
		List<String> methodParams = new ArrayList<>();
		if (formalParameterList != null) {
			methodParams.addAll(getMethodParams(formalParameterList.fixed_parameters()));
			if (formalParameterList.parameter_array() != null)
				methodParams.add("params " + getText(formalParameterList.parameter_array().array_type()));
		}
		if (methodParams.isEmpty())
			return null;
		else
			return Joiner.on(", ").join(methodParams);
	}
	
	private String getReturnType(Return_typeContext returnType) {
		if (returnType.VOID() != null)
			return "void";
		else
			return getText(returnType.type());
	}
		
	@Nullable
	private String getTypeParameters(@Nullable Type_parameter_listContext typeParameterList) {
		List<String> typeParams = new ArrayList<>();
		if (typeParameterList != null) {
			for (Type_parameterContext typeParameter: typeParameterList.type_parameter()) {
				typeParams.add(getText(typeParameter.identifier()));
			}
		}
		if (typeParams.isEmpty()) 
			return null;
		else
			return "<" + Joiner.on(", ").join(typeParams) + ">";
	}
	
	private String getTypeParameters(@Nullable Variant_type_parameter_listContext variantTypeParameterList) {
		List<String> typeParams = new ArrayList<>();
		if (variantTypeParameterList != null) {
			for (Variant_type_parameterContext typeParameter: variantTypeParameterList.variant_type_parameter()) {
				typeParams.add(getText(typeParameter.identifier()));
			}
		}
		if (typeParams.isEmpty()) 
			return null;
		else
			return "<" + Joiner.on(", ").join(typeParams) + ">";
	}
	
	private EnumSet<CSharpSymbol.Modifier> getModifiers(@Nullable All_member_modifiersContext allMemberModifiers) {
		EnumSet<CSharpSymbol.Modifier> modifiers = EnumSet.noneOf(CSharpSymbol.Modifier.class);
		if (allMemberModifiers != null) {
			for (All_member_modifierContext allMemberModifier: allMemberModifiers.all_member_modifier()) {
				modifiers.add(CSharpSymbol.Modifier.valueOf(allMemberModifier.getText().toUpperCase()));
			}
		}
		return modifiers;
	}
	
	private List<Token> preprocess(String sourceCode) {
		List<Token> codeTokens = new ArrayList<>();

		BaseErrorListener errorListener = newErrorListener();
		Lexer preprocessorLexer = new CSharpLexer(CharStreams.fromString(sourceCode));
		preprocessorLexer.removeErrorListeners();
		preprocessorLexer.addErrorListener(errorListener);
		clearLexerCache(preprocessorLexer);
		List<? extends Token> tokens = preprocessorLexer.getAllTokens();
		
		List<Token> directiveTokens = new ArrayList<>();
		TokenSource directiveTokenSource = new ListTokenSource(directiveTokens);
		TokenStream directiveTokenStream = new CommonTokenStream(directiveTokenSource, CSharpLexer.DIRECTIVE);
		CSharpPreprocessorParser preprocessorParser = new CSharpPreprocessorParser(directiveTokenStream);
		preprocessorParser.removeErrorListeners();
		preprocessorParser.addErrorListener(errorListener);
		clearParserCache(preprocessorParser);
	
		int index = 0;
		boolean compiliedTokens = true;
		while (index < tokens.size()) {
		    Token token = tokens.get(index);
		    if (token.getType() == CSharpLexer.SHARP) {
		        directiveTokens.clear();
		        int directiveTokenIndex = index + 1;
		        while (directiveTokenIndex < tokens.size() 
		        		&& tokens.get(directiveTokenIndex).getType() != CSharpLexer.EOF 
		        		&& tokens.get(directiveTokenIndex).getType() != CSharpLexer.DIRECTIVE_NEW_LINE 
		        		&& tokens.get(directiveTokenIndex).getType() != CSharpLexer.SHARP) {
		        	int channel = tokens.get(directiveTokenIndex).getChannel();
		            if (channel != CSharpLexer.COMMENTS_CHANNEL && channel != Lexer.HIDDEN) {
		                directiveTokens.add(tokens.get(directiveTokenIndex));
		            }
		            directiveTokenIndex++;
		        }
	
		        directiveTokenSource = new ListTokenSource(directiveTokens);
		        directiveTokenStream = new CommonTokenStream(directiveTokenSource, CSharpLexer.DIRECTIVE);
		        preprocessorParser.setInputStream(directiveTokenStream);
		        preprocessorParser.reset();
		        CSharpPreprocessorParser.Preprocessor_directiveContext directive = preprocessorParser.preprocessor_directive();
		        compiliedTokens = directive.value;
		        index = directiveTokenIndex - 1;
		    } else if (token.getChannel() != CSharpLexer.COMMENTS_CHANNEL 
		    		&& token.getChannel() != Lexer.HIDDEN 
		    		&& token.getType() != CSharpLexer.DIRECTIVE_NEW_LINE 
		    		&& compiliedTokens) {
		        codeTokens.add(token);
		    }
		    index++;
		}

		return codeTokens;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "cs");
	}

	@Override
	public int getVersion() {
		return 3;
	}

}