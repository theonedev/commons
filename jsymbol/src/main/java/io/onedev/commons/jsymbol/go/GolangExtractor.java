package io.onedev.commons.jsymbol.go;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.go.GolangParser.ConstSpecContext;
import io.onedev.commons.jsymbol.go.GolangParser.DeclarationContext;
import io.onedev.commons.jsymbol.go.GolangParser.FieldDeclContext;
import io.onedev.commons.jsymbol.go.GolangParser.FunctionContext;
import io.onedev.commons.jsymbol.go.GolangParser.FunctionDeclContext;
import io.onedev.commons.jsymbol.go.GolangParser.FunctionOrSignatureContext;
import io.onedev.commons.jsymbol.go.GolangParser.InterfaceTypeContext;
import io.onedev.commons.jsymbol.go.GolangParser.MethodDeclContext;
import io.onedev.commons.jsymbol.go.GolangParser.MethodSpecContext;
import io.onedev.commons.jsymbol.go.GolangParser.ParametersContext;
import io.onedev.commons.jsymbol.go.GolangParser.StructTypeContext;
import io.onedev.commons.jsymbol.go.GolangParser.TopLevelDeclContext;
import io.onedev.commons.jsymbol.go.GolangParser.TypeContext;
import io.onedev.commons.jsymbol.go.GolangParser.TypeDeclContext;
import io.onedev.commons.jsymbol.go.GolangParser.TypeSpecContext;
import io.onedev.commons.jsymbol.go.GolangParser.VarSpecContext;
import io.onedev.commons.jsymbol.go.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.go.symbols.GolangSymbol;
import io.onedev.commons.jsymbol.go.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.go.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.util.Utils;

public class GolangExtractor extends AbstractSymbolExtractor<GolangSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(GolangExtractor.class);
	
	@Override
	public List<GolangSymbol> extract(String fileName, String fileContent) {
		List<GolangSymbol> symbols = new ArrayList<>();

		ANTLRErrorListener errorListener = new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				if (!msg.contains("failed predicate: {!lineTerminatorAhead()}?")) {
					logger.trace("{}: {}: {}", line, charPositionInLine, msg);
				}
			}
			
		};
		
		GolangLexer lexer = new GolangLexer(CharStreams.fromString(fileContent));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		clearLexerCache(lexer);
		
		CommonTokenStream stream = new CommonTokenStream(lexer);
		GolangParser parser = new GolangParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		clearParserCache(parser);
		
		for (TopLevelDeclContext topLevelDecl: parser.sourceFile().topLevelDecl()) {
			if (topLevelDecl.declaration() != null) {
				extract(topLevelDecl.declaration(), symbols, null, fileContent);
			} else if (topLevelDecl.methodDecl() != null) {
				MethodDeclContext methodDecl = topLevelDecl.methodDecl();
				String funcName = methodDecl.IDENTIFIER().getText();
				TokenPosition position = Utils.getTokenPosition(methodDecl.IDENTIFIER().getSymbol());
				TokenPosition scope = Utils.getTokenPosition(methodDecl);
				String receiver = methodDecl.receiver().parameters().parameterList().parameterDecl(0).type().getText();
				extract(methodDecl.functionOrSignature(), symbols, null, funcName, receiver, position, scope, 
						fileContent);
			} else if (topLevelDecl.functionDecl() != null) {
				FunctionDeclContext functionDecl = topLevelDecl.functionDecl();
				String funcName = functionDecl.IDENTIFIER().getText();
				TokenPosition position = Utils.getTokenPosition(functionDecl.IDENTIFIER().getSymbol());
				TokenPosition scope = Utils.getTokenPosition(functionDecl);
				extract(functionDecl.functionOrSignature(), symbols, null, funcName, null, position, scope, 
						fileContent);
			}
		}
		
		List<FunctionSymbol> methodSymbols = new ArrayList<>();
		for (GolangSymbol symbol: symbols) {
			if (symbol instanceof FunctionSymbol) {
				FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
				if (functionSymbol.getReceiver() != null) 
					methodSymbols.add(functionSymbol);
			}
		}
		for (FunctionSymbol methodSymbol: methodSymbols) {
			GolangSymbol receiverSymbol = getSymbol(symbols, null, methodSymbol.getReceiver());
			if (receiverSymbol != null) {
				methodSymbol.setParent(receiverSymbol);
			} else {
				TypeSymbol typeSymbol = new TypeSymbol(null, methodSymbol.getReceiver(), null, null, false, 
						false);
				methodSymbol.setParent(typeSymbol);
				symbols.add(typeSymbol);
			}
		}
		
		return symbols;
	}
	
	@Nullable
	private GolangSymbol getSymbol(List<GolangSymbol> symbols, @Nullable GolangSymbol parentSymbol, String name) {
		for (GolangSymbol symbol: symbols) {
			if (symbol.getParent() == parentSymbol && symbol.getName().equals(name))
				return symbol;
		}
		return null;
	}
	
	private String describe(ParserRuleContext parserRule, String source) {
		return source.substring(parserRule.start.getStartIndex(), parserRule.stop.getStopIndex()+1);
	}
	
	private void extract(DeclarationContext declaration, List<GolangSymbol> symbols, GolangSymbol parentSymbol, 
			String source) {
		if (declaration.constDecl() != null) {
			if (!(parentSymbol instanceof FunctionSymbol)) {
				for (ConstSpecContext constSpec: declaration.constDecl().constSpec()) {
					String type;
					if (constSpec.type() != null) 
						type = describe(constSpec.type(), source);
					else
						type = null;
					for (TerminalNode identifier: constSpec.identifierList().IDENTIFIER()) {
						String varName = identifier.getText();
						TokenPosition position = Utils.getTokenPosition(identifier.getSymbol());
						symbols.add(new VariableSymbol(parentSymbol, varName, type, position, true));
					}
				}
			}
		} else if (declaration.varDecl() != null) {
			if (!(parentSymbol instanceof FunctionSymbol)) {
				for (VarSpecContext varSpec: declaration.varDecl().varSpec()) {
					String type;
					if (varSpec.type() != null) 
						type = describe(varSpec.type(), source);
					else
						type = null;
					for (TerminalNode identifier: varSpec.identifierList().IDENTIFIER()) {
						String varName = identifier.getText();
						TokenPosition position = Utils.getTokenPosition(identifier.getSymbol());
						symbols.add(new VariableSymbol(parentSymbol, varName, type, position, true));
					}
				}
			}
		} else if (declaration.typeDecl() != null) {
			TypeDeclContext typeDecl = declaration.typeDecl();
			for (TypeSpecContext typeSpec: typeDecl.typeSpec()) {
				String typeName = typeSpec.IDENTIFIER().getText();
				TokenPosition position = Utils.getTokenPosition(typeSpec.IDENTIFIER().getSymbol());
				TokenPosition scope = Utils.getTokenPosition(typeSpec);
				boolean isIntf = typeSpec.type().typeLit() != null && typeSpec.type().typeLit().interfaceType() != null;
				TypeSymbol typeSymbol = new TypeSymbol(parentSymbol, typeName, position, scope, isIntf, true);
				symbols.add(typeSymbol);				
				extract(typeSpec.type(), symbols, typeSymbol, source);
			}
		}
	}
	
	private boolean extract(TypeContext type, List<GolangSymbol> symbols, GolangSymbol parentSymbol, String source) {
		if (type.typeLit() != null) {
			if (type.typeLit().structType() != null) {
				StructTypeContext structType = type.typeLit().structType();
				for (FieldDeclContext fieldDecl: structType.fieldDecl()) {
					if (fieldDecl.anonymousField() != null) {
						TokenPosition fieldPosition = Utils.getTokenPosition(fieldDecl.anonymousField().typeName());
						String fieldName = fieldDecl.anonymousField().typeName().getText();
						symbols.add(new VariableSymbol(parentSymbol, fieldName, null, fieldPosition, false));
					} else if (fieldDecl.type() != null) { // type might be null due to semantic prediction
						for (TerminalNode identifier: fieldDecl.identifierList().IDENTIFIER()) {
							String fieldName = identifier.getText();
							TokenPosition fieldPosition = Utils.getTokenPosition(identifier.getSymbol());
							VariableSymbol variableSymbol = new VariableSymbol(parentSymbol, fieldName, null, 
									fieldPosition, true);
							symbols.add(variableSymbol);
							if (!extract(fieldDecl.type(), symbols, variableSymbol, source))
								variableSymbol.setType(describe(fieldDecl.type(), source));
						}
					} else {
						TokenPosition fieldPosition = Utils.getTokenPosition(fieldDecl.identifierList());
						String fieldName = fieldDecl.identifierList().getText();
						symbols.add(new VariableSymbol(parentSymbol, fieldName, null, fieldPosition, false));
					}
				}
				return true;
			} else if (type.typeLit().interfaceType() != null) {
				InterfaceTypeContext interfaceType = type.typeLit().interfaceType();
				for (MethodSpecContext methodSpec: interfaceType.methodSpec()) {
					if (methodSpec.typeName() != null) {
						String interfaceName = methodSpec.typeName().getText();
						TokenPosition position = Utils.getTokenPosition(methodSpec.typeName());
						symbols.add(new TypeSymbol(parentSymbol, interfaceName, position, null, true, false));
					} else if (methodSpec.signature() != null) { // signature might be null due to semantic prediction
						ParametersContext parameters = methodSpec.signature().parameters();
						String functionParams = describe(parameters, source);
						String returnType;
						if (methodSpec.signature().result() != null) {
							returnType = describe(methodSpec.signature().result(), source);
						} else {
							returnType = null;
						}
						String methodName = methodSpec.IDENTIFIER().getText();
						TokenPosition position = Utils.getTokenPosition(methodSpec.IDENTIFIER().getSymbol());
						TokenPosition scope = Utils.getTokenPosition(methodSpec);
						symbols.add(new FunctionSymbol(parentSymbol, methodName, functionParams, returnType, null, 
								position, scope));
					} else {
						String interfaceName = methodSpec.IDENTIFIER().getText();
						TokenPosition position = Utils.getTokenPosition(methodSpec.IDENTIFIER().getSymbol());
						symbols.add(new TypeSymbol(parentSymbol, interfaceName, position, null, true, false));
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	private void extract(FunctionOrSignatureContext functionOrSignature, List<GolangSymbol> symbols, 
			GolangSymbol parentSymbol, String funcName, String receiver, TokenPosition position, 
			TokenPosition scope, String source) {
		if (functionOrSignature.function() != null) {
			FunctionContext function = functionOrSignature.function();
			String functionParams = describe(function.signature().parameters(), source);
			String returnType;
			if (function.signature().result() != null) {
				returnType = describe(function.signature().result(), source);
			} else {
				returnType = null;
			}
			FunctionSymbol functionSymbol = new FunctionSymbol(parentSymbol, funcName, functionParams, returnType, 
					receiver, position, scope);
			symbols.add(functionSymbol);
		} else {
			String functionParams = describe(functionOrSignature.signature().parameters(), source);
			String returnType;
			if (functionOrSignature.signature().result() != null) {
				returnType = describe(functionOrSignature.signature().result(), source);
			} else {
				returnType = null;
			}
			FunctionSymbol functionSymbol = new FunctionSymbol(null, funcName, functionParams, returnType, 
					receiver, position, scope);
			symbols.add(functionSymbol);
		}
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "go");
	}

	@Override
	public int getVersion() {
		return 0;
	}

}
