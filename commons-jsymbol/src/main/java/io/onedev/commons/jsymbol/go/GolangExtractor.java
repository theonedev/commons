package io.onedev.commons.jsymbol.go;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.go.GoParser.*;
import io.onedev.commons.jsymbol.go.symbols.*;
import io.onedev.commons.utils.ExplicitException;
import io.onedev.commons.utils.PlanarRange;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

import static io.onedev.commons.jsymbol.util.Utils.getTextRange;

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
		
		GoLexer lexer = new GoLexer(CharStreams.fromString(fileContent));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		clearLexerCache(lexer);
		
		CommonTokenStream stream = new CommonTokenStream(lexer);
		GoParser parser = new GoParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		clearParserCache(parser);

		var sourceFile = parser.sourceFile();
		var packageSymbol = new PackageSymbol(
				sourceFile.packageClause().IDENTIFIER().getText(),
				getTextRange(sourceFile.packageClause().IDENTIFIER().getSymbol()),
				getTextRange(sourceFile.packageClause()));
		symbols.add(packageSymbol);

		for (var declaration: sourceFile.declaration())
			extract(declaration, symbols, packageSymbol, fileContent);

		for (var methodDecl: sourceFile.methodDecl()) {
			String funcName = methodDecl.IDENTIFIER().getText();
			PlanarRange position = getTextRange(methodDecl.IDENTIFIER().getSymbol());
			PlanarRange scope = getTextRange(methodDecl);
			var parameterDecls = methodDecl.receiver().parameters().parameterDecl();
			if (parameterDecls.size() != 1)
				throw new ExplicitException("Invalid receiver parameters");
			var type_ = parameterDecls.get(0).type_();
			while (type_.type_() != null)
				type_ = type_.type_();
			String receiver;
			if (type_.typeName() != null)
				receiver = type_.typeName().getText();
			else
				receiver = type_.typeLit().getText();
			extract(methodDecl.signature(), symbols, packageSymbol, funcName, receiver, position, scope,
					fileContent);
		}

		for (var functionDecl: sourceFile.functionDecl()) {
			String funcName = functionDecl.IDENTIFIER().getText();
			PlanarRange position = getTextRange(functionDecl.IDENTIFIER().getSymbol());
			PlanarRange scope = getTextRange(functionDecl);
			extract(functionDecl.signature(), symbols, packageSymbol, funcName, null, position, scope,
					fileContent);
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
			GolangSymbol receiverSymbol = getSymbol(symbols, packageSymbol, methodSymbol.getReceiver());
			if (receiverSymbol != null) {
				methodSymbol.setParent(receiverSymbol);
			} else {
				TypeSymbol typeSymbol = new TypeSymbol(packageSymbol, methodSymbol.getReceiver(), null, null, false,
						false);
				methodSymbol.setParent(typeSymbol);
				symbols.add(typeSymbol);
			}
		}
		
		return symbols;
	}
	
	@Nullable
	private GolangSymbol getSymbol(List<GolangSymbol> symbols, GolangSymbol parentSymbol, String name) {
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
					if (constSpec.type_() != null)
						type = describe(constSpec.type_(), source);
					else
						type = null;
					for (TerminalNode identifier: constSpec.identifierList().IDENTIFIER()) {
						String varName = identifier.getText();
						PlanarRange position = getTextRange(identifier.getSymbol());
						symbols.add(new VariableSymbol(parentSymbol, varName, type, position, true));
					}
				}
			}
		} else if (declaration.varDecl() != null) {
			if (!(parentSymbol instanceof FunctionSymbol)) {
				for (GoParser.VarSpecContext varSpec: declaration.varDecl().varSpec()) {
					String type;
					if (varSpec.type_() != null)
						type = describe(varSpec.type_(), source);
					else
						type = null;
					for (TerminalNode identifier: varSpec.identifierList().IDENTIFIER()) {
						String varName = identifier.getText();
						PlanarRange position = getTextRange(identifier.getSymbol());
						symbols.add(new VariableSymbol(parentSymbol, varName, type, position, true));
					}
				}
			}
		} else if (declaration.typeDecl() != null) {
			TypeDeclContext typeDecl = declaration.typeDecl();
			for (TypeSpecContext typeSpec: typeDecl.typeSpec()) {
				TerminalNode identifier;
				Type_Context type_;
				if (typeSpec.typeDef() != null) {
					identifier = typeSpec.typeDef().IDENTIFIER();
					type_ = typeSpec.typeDef().type_();
				} else {
					identifier = typeSpec.aliasDecl().IDENTIFIER();
					type_ = typeSpec.aliasDecl().type_();
				}
				String typeName = identifier.getText();
				PlanarRange position = getTextRange(identifier.getSymbol());
				PlanarRange scope = getTextRange(typeSpec);
				boolean isIntf = type_.typeLit() != null && type_.typeLit().interfaceType() != null;
				TypeSymbol typeSymbol = new TypeSymbol(parentSymbol, typeName, position, scope, isIntf, true);
				symbols.add(typeSymbol);
				extract(type_, symbols, typeSymbol, source);
			}
		}
	}
	
	private boolean extract(Type_Context type_, List<GolangSymbol> symbols, GolangSymbol parentSymbol, String source) {
		if (type_.typeLit() != null) {
			if (type_.typeLit().structType() != null) {
				var structType = type_.typeLit().structType();
				for (var fieldDecl : structType.fieldDecl()) {
					if (fieldDecl.embeddedField() != null) {
						PlanarRange fieldPosition = getTextRange(fieldDecl.embeddedField().typeName());
						String fieldName = fieldDecl.embeddedField().typeName().getText();
						symbols.add(new VariableSymbol(parentSymbol, fieldName, null, fieldPosition, false));
					} else if (fieldDecl.type_() != null) { // type might be null due to semantic prediction
						for (var identifier : fieldDecl.identifierList().IDENTIFIER()) {
							String fieldName = identifier.getText();
							PlanarRange fieldPosition = getTextRange(identifier.getSymbol());
							VariableSymbol variableSymbol = new VariableSymbol(parentSymbol, fieldName, null,
									fieldPosition, true);
							symbols.add(variableSymbol);
							if (!extract(fieldDecl.type_(), symbols, variableSymbol, source))
								variableSymbol.setType(describe(fieldDecl.type_(), source));
						}
					} else {
						PlanarRange fieldPosition = getTextRange(fieldDecl.identifierList());
						String fieldName = fieldDecl.identifierList().getText();
						symbols.add(new VariableSymbol(parentSymbol, fieldName, null, fieldPosition, false));
					}
				}
				return true;
			} else if (type_.typeLit().interfaceType() != null) {
				var interfaceType = type_.typeLit().interfaceType();
				for (var methodSpec : interfaceType.methodSpec()) {
					if (methodSpec.parameters() != null) { // signature might be null due to semantic prediction
						String functionParams = describe(methodSpec.parameters(), source);
						String returnType;
						if (methodSpec.result() != null) {
							returnType = describe(methodSpec.result(), source);
						} else {
							returnType = null;
						}
						String methodName = methodSpec.IDENTIFIER().getText();
						PlanarRange position = getTextRange(methodSpec.IDENTIFIER().getSymbol());
						PlanarRange scope = getTextRange(methodSpec);
						symbols.add(new FunctionSymbol(parentSymbol, methodName, functionParams, returnType, null,
								position, scope));
					} else {
						String interfaceName = methodSpec.IDENTIFIER().getText();
						PlanarRange position = getTextRange(methodSpec.IDENTIFIER().getSymbol());
						symbols.add(new TypeSymbol(parentSymbol, interfaceName, position, null, true, false));
					}
				}
				for (var typeElement : interfaceType.typeElement()) {
					for (var typeTerm : typeElement.typeTerm())
						extract(typeTerm.type_(), symbols, parentSymbol, source);
				}
				return true;
			} else {
				return false;
			}
		} else if (type_.type_() != null) {
			return extract(type_.type_(), symbols, parentSymbol, source);
		} else {
			return false;
		}
	}
	
	private void extract(SignatureContext signature, List<GolangSymbol> symbols,
						 GolangSymbol parentSymbol, String funcName, String receiver,
						 PlanarRange position, PlanarRange scope, String source) {
		String functionParams = describe(signature.parameters(), source);
		String returnType;
		if (signature.result() != null) {
			returnType = describe(signature.result(), source);
		} else {
			returnType = null;
		}
		FunctionSymbol functionSymbol = new FunctionSymbol(parentSymbol, funcName, functionParams, returnType,
				receiver, position, scope);
		symbols.add(functionSymbol);
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "go");
	}

	@Override
	public int getVersion() {
		return 4;
	}

}
