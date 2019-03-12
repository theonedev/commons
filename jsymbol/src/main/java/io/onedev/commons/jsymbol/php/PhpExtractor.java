package io.onedev.commons.jsymbol.php;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.php.symbols.ConstantSymbol;
import io.onedev.commons.jsymbol.php.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.php.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.php.symbols.NamespaceSymbol;
import io.onedev.commons.jsymbol.php.symbols.PhpSymbol;
import io.onedev.commons.jsymbol.php.symbols.TypeSymbol;
import io.onedev.commons.jsymbol.php.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.php.symbols.Visibility;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.jsymbol.util.Utils;
import io.onedev.commons.jsymbol.php.PHPLexer;
import io.onedev.commons.jsymbol.php.PHPParser;
import io.onedev.commons.jsymbol.php.PHPParser.ActualArgumentContext;
import io.onedev.commons.jsymbol.php.PHPParser.ArgumentsContext;
import io.onedev.commons.jsymbol.php.PHPParser.BlockStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.ChainBaseContext;
import io.onedev.commons.jsymbol.php.PHPParser.ChainContext;
import io.onedev.commons.jsymbol.php.PHPParser.ClassDeclarationContext;
import io.onedev.commons.jsymbol.php.PHPParser.ClassStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.ConstantContext;
import io.onedev.commons.jsymbol.php.PHPParser.DeclareStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.ElseIfColonStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.ElseIfStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.ExpressionContext;
import io.onedev.commons.jsymbol.php.PHPParser.ForStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.ForeachStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.FormalParameterListContext;
import io.onedev.commons.jsymbol.php.PHPParser.FunctionDeclarationContext;
import io.onedev.commons.jsymbol.php.PHPParser.GlobalConstantDeclarationContext;
import io.onedev.commons.jsymbol.php.PHPParser.GlobalStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.GlobalVarContext;
import io.onedev.commons.jsymbol.php.PHPParser.HtmlElementOrPhpBlockContext;
import io.onedev.commons.jsymbol.php.PHPParser.IdentifierContext;
import io.onedev.commons.jsymbol.php.PHPParser.IdentifierInititalizerContext;
import io.onedev.commons.jsymbol.php.PHPParser.IfStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.InnerStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.InnerStatementListContext;
import io.onedev.commons.jsymbol.php.PHPParser.KeyedVariableContext;
import io.onedev.commons.jsymbol.php.PHPParser.MemberModifierContext;
import io.onedev.commons.jsymbol.php.PHPParser.MemberModifiersContext;
import io.onedev.commons.jsymbol.php.PHPParser.NamespaceDeclarationContext;
import io.onedev.commons.jsymbol.php.PHPParser.NamespaceStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.NonEmptyStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.StatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.StaticVariableStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.StringContext;
import io.onedev.commons.jsymbol.php.PHPParser.SwitchBlockContext;
import io.onedev.commons.jsymbol.php.PHPParser.SwitchStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.TopStatementContext;
import io.onedev.commons.jsymbol.php.PHPParser.TryCatchFinallyContext;
import io.onedev.commons.jsymbol.php.PHPParser.UseDeclarationContentContext;
import io.onedev.commons.jsymbol.php.PHPParser.UseDeclarationContext;
import io.onedev.commons.jsymbol.php.PHPParser.UseGroupContext;
import io.onedev.commons.jsymbol.php.PHPParser.UseGroupElementContext;
import io.onedev.commons.jsymbol.php.PHPParser.VariableInitializerContext;
import io.onedev.commons.jsymbol.php.PHPParser.WhileStatementContext;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

public class PhpExtractor extends AbstractSymbolExtractor<PhpSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(PhpExtractor.class);
	
	@Override
	public List<PhpSymbol> extract(String fileName, String fileContent) {
		List<PhpSymbol> symbols = new ArrayList<>();

		ANTLRErrorListener errorListener = new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				logger.trace("{}: {}: {}", line, charPositionInLine, msg);
			}
			
		};
		
		PHPLexer lexer = new PHPLexer(new ANTLRInputStream(fileContent));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		
		clearLexerCache(lexer);
		
		List<? extends Token> tokens = lexer.getAllTokens();
		
		CommonTokenStream stream = new CommonTokenStream(new ListTokenSource(tokens));
		PHPParser parser = new PHPParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		
		clearParserCache(parser);
		
		NamespaceSymbol namespaceSymbol = null;
		Token namespaceStartToken = null;
		Token statementStopToken = null;
		for (HtmlElementOrPhpBlockContext htmlElementOrPhpBlock: parser.htmlDocument().htmlElementOrPhpBlock()) {
			if (htmlElementOrPhpBlock.phpBlock() != null) {
				for (TopStatementContext topStatement: htmlElementOrPhpBlock.phpBlock().topStatement()) {
					if (topStatement.nonEmptyStatement() != null) {
						extract(tokens, topStatement.nonEmptyStatement(), symbols, namespaceSymbol, fileContent);
					} else if (topStatement.useDeclaration() != null) {
						extract(tokens, topStatement.useDeclaration(), symbols, namespaceSymbol);
					} else if (topStatement.namespaceDeclaration() != null) {
						NamespaceSymbol newNamespaceSymbol = extract(tokens, topStatement.namespaceDeclaration(), 
								symbols, fileContent);
						if (newNamespaceSymbol != null) {
							if (namespaceSymbol != null) {
								Preconditions.checkState(namespaceStartToken != null && statementStopToken != null);
								namespaceSymbol.setScope(getTokenPosition(tokens, namespaceStartToken, statementStopToken));
							}
							namespaceSymbol = newNamespaceSymbol;
							namespaceStartToken = topStatement.namespaceDeclaration().start;
						}
					} else if (topStatement.functionDeclaration() != null) {
						extract(tokens, topStatement.functionDeclaration(), symbols, namespaceSymbol, fileContent);
					} else if (topStatement.classDeclaration() != null) {
						extract(tokens, topStatement.classDeclaration(), symbols, namespaceSymbol, fileContent);
					} else if (topStatement.globalConstantDeclaration() != null) {
						extract(tokens, topStatement.globalConstantDeclaration(), symbols, namespaceSymbol);
					}
					statementStopToken = topStatement.stop;
				}
			}
		}
		if (namespaceSymbol != null) {
			Preconditions.checkState(namespaceStartToken != null && statementStopToken != null);
			namespaceSymbol.setScope(getTokenPosition(tokens, namespaceStartToken, statementStopToken));
		}
		return symbols;
	}
	
	private TokenPosition getTokenPosition(List<? extends Token> tokens, ParserRuleContext context) {
		return getTokenPosition(tokens, context.start, context.stop);
	}
	
	private TokenPosition getTokenPosition(List<? extends Token> tokens, Token start, Token stop) {
		if (stop.getText() == null)
			stop = tokens.get(stop.getTokenIndex()-1);
		return Utils.getTokenPosition(start, stop);
	}
	
	@Nullable
	private NamespaceSymbol extract(List<? extends Token> tokens, NamespaceDeclarationContext namespaceDeclaration, 
			List<PhpSymbol> symbols, String source) {
		NamespaceSymbol namespaceSymbol = null;
		if (namespaceDeclaration.namespaceNameList() != null) {
			List<String> prefixes = new ArrayList<>();
			int identifierCount = namespaceDeclaration.namespaceNameList().identifier().size();
			for (int i=0; i<identifierCount-1; i++) {
				prefixes.add(namespaceDeclaration.namespaceNameList().identifier(i).getText());
			}
			String namespaceName = namespaceDeclaration.namespaceNameList().identifier(identifierCount-1).getText();
			QualifiedName qualifiedName;
			if (prefixes.isEmpty())
				qualifiedName = new QualifiedName(namespaceName, null, null);
			else
				qualifiedName = new QualifiedName(namespaceName, Joiner.on("\\").join(prefixes)+"\\", null);
			TokenPosition position = getTokenPosition(tokens, namespaceDeclaration.namespaceNameList());
			TokenPosition scope = getTokenPosition(tokens, namespaceDeclaration);
			namespaceSymbol = new NamespaceSymbol(null, qualifiedName, position, scope);
			symbols.add(namespaceSymbol);
		}
		for (NamespaceStatementContext namespaceStatement: namespaceDeclaration.namespaceStatement()) {
			if (namespaceStatement.nonEmptyStatement() != null) {
				extract(tokens, namespaceStatement.nonEmptyStatement(), symbols, namespaceSymbol, source);
			} else if (namespaceStatement.useDeclaration() != null) {
				extract(tokens, namespaceStatement.useDeclaration(), symbols, namespaceSymbol);
			} else if (namespaceStatement.functionDeclaration() != null) {
				extract(tokens, namespaceStatement.functionDeclaration(), symbols, namespaceSymbol, source);
			} else if (namespaceStatement.globalConstantDeclaration() != null) {
				extract(tokens, namespaceStatement.globalConstantDeclaration(), symbols, namespaceSymbol);
			} else if (namespaceStatement.classDeclaration() != null) {
				extract(tokens, namespaceStatement.classDeclaration(), symbols, namespaceSymbol, source);
			}
		}
		if (namespaceDeclaration.OpenCurlyBracket() == null)
			return namespaceSymbol;
		else
			return null;
	}
	
	private void extract(List<? extends Token> tokens, NonEmptyStatementContext nonEmptyStatement, 
			List<PhpSymbol> symbols, PhpSymbol parentSymbol, String source) {
		if (nonEmptyStatement.blockStatement() != null) {
			extract(tokens, nonEmptyStatement.blockStatement(), symbols, parentSymbol, source);
		} else if (nonEmptyStatement.ifStatement() != null) {
			IfStatementContext ifStatement = nonEmptyStatement.ifStatement();
			if (ifStatement.statement() != null)
				extract(tokens, ifStatement.statement(), symbols, parentSymbol, source);
			for (ElseIfStatementContext elseIfStatement: ifStatement.elseIfStatement()) {
				if (elseIfStatement.statement() != null)
					extract(tokens, elseIfStatement.statement(), symbols, parentSymbol, source);
			}
			if (ifStatement.elseStatement() != null && ifStatement.elseStatement().statement() != null) 
				extract(tokens, ifStatement.elseStatement().statement(), symbols, parentSymbol, source);
			if (ifStatement.innerStatementList() != null) 
				extract(tokens, ifStatement.innerStatementList(), symbols, parentSymbol, source);
			for (ElseIfColonStatementContext elseIfColonStatement: ifStatement.elseIfColonStatement()) {
				if (elseIfColonStatement.innerStatementList() != null)
					extract(tokens, elseIfColonStatement.innerStatementList(), symbols, parentSymbol, source);
			}
			if (ifStatement.elseColonStatement() != null 
					&& ifStatement.elseColonStatement().innerStatementList() != null) {
				extract(tokens, ifStatement.elseColonStatement().innerStatementList(), symbols, parentSymbol, source);
			}
		} else if (nonEmptyStatement.whileStatement() != null) {
			WhileStatementContext whileStatement = nonEmptyStatement.whileStatement();
			if (whileStatement.statement() != null)
				extract(tokens, whileStatement.statement(), symbols, parentSymbol, source);
			if (whileStatement.innerStatementList() != null)
				extract(tokens, whileStatement.innerStatementList(), symbols, parentSymbol, source);
		} else if (nonEmptyStatement.doWhileStatement() != null) {
			if (nonEmptyStatement.doWhileStatement().statement() != null)
				extract(tokens, nonEmptyStatement.doWhileStatement().statement(), symbols, parentSymbol, source);
		} else if (nonEmptyStatement.forStatement() != null) {
			ForStatementContext forStatement = nonEmptyStatement.forStatement();
			if (forStatement.statement() != null)
				extract(tokens, forStatement.statement(), symbols, parentSymbol, source);
			if (forStatement.innerStatementList() != null)
				extract(tokens, forStatement.innerStatementList(), symbols, parentSymbol, source);
		} else if (nonEmptyStatement.switchStatement() != null) {
			SwitchStatementContext switchStatement = nonEmptyStatement.switchStatement();
			for (SwitchBlockContext switchBlock: switchStatement.switchBlock()) {
				if (switchBlock.innerStatementList() != null)
					extract(tokens, switchBlock.innerStatementList(), symbols, parentSymbol, source);
			}
		} else if (nonEmptyStatement.globalStatement() != null) {
			GlobalStatementContext globalStatement = nonEmptyStatement.globalStatement();
			for (GlobalVarContext globalVar: globalStatement.globalVar()) {
				if (globalVar.VarName() != null) {
					String varName = globalVar.VarName().getText();
					VariableSymbol varSymbol = findGlobalVariable(symbols, varName);
					if (varSymbol == null) {
						varSymbol = new VariableSymbol(null, varName, Visibility.PUBLIC, 
								Utils.getTokenPosition(globalVar.VarName().getSymbol()));
						symbols.add(varSymbol);
					}
				}
			}
		} else if (nonEmptyStatement.staticVariableStatement() != null) {
			if (parentSymbol == null || parentSymbol instanceof NamespaceSymbol) {
				StaticVariableStatementContext staticVariableStatement = nonEmptyStatement.staticVariableStatement();
				for (VariableInitializerContext variableInitializer: staticVariableStatement.variableInitializer()) {
					String varName = variableInitializer.VarName().getText();
					VariableSymbol varSymbol = findGlobalVariable(symbols, varName);
					if (varSymbol == null) {
						varSymbol = new VariableSymbol(null, varName, Visibility.PUBLIC, 
								Utils.getTokenPosition(variableInitializer.VarName().getSymbol()));
						symbols.add(varSymbol);
					}
				}
			}
		} else if (nonEmptyStatement.foreachStatement() != null) {
			ForeachStatementContext foreachStatement = nonEmptyStatement.foreachStatement();
			if (foreachStatement.statement() != null) 
				extract(tokens, foreachStatement.statement(), symbols, parentSymbol, source);
			if (foreachStatement.innerStatementList() != null)
				extract(tokens, foreachStatement.innerStatementList(), symbols, parentSymbol, source);
		} else if (nonEmptyStatement.tryCatchFinally() != null) {
			TryCatchFinallyContext tryCatchFinally = nonEmptyStatement.tryCatchFinally();
			if (tryCatchFinally.blockStatement() != null)
				extract(tokens, tryCatchFinally.blockStatement(), symbols, parentSymbol, source);
			if (tryCatchFinally.finallyStatement() != null 
					&& tryCatchFinally.finallyStatement().blockStatement() != null) {
				extract(tokens, tryCatchFinally.finallyStatement().blockStatement(), symbols, parentSymbol, source);
			}
		} else if (nonEmptyStatement.declareStatement() != null) {
			DeclareStatementContext declareStatement = nonEmptyStatement.declareStatement();
			if (declareStatement.statement() != null)
				extract(tokens, declareStatement.statement(), symbols, parentSymbol, source);
			if (declareStatement.innerStatementList() != null)
				extract(tokens, declareStatement.innerStatementList(), symbols, parentSymbol, source);
		} else if (nonEmptyStatement.expressionStatement() != null 
				&& nonEmptyStatement.expressionStatement().expression() != null) {
			extract(tokens, nonEmptyStatement.expressionStatement().expression(), symbols, parentSymbol);
		}
	}
	
	private void extract(List<? extends Token> tokens, ExpressionContext expression, List<PhpSymbol> symbols, 
			PhpSymbol parentSymbol) {
		NamespaceSymbol namespaceSymbol = findNamespaceParent(parentSymbol);
		if (expression.children.size() > 0) {
			ParseTree firstChild = expression.getChild(0);
			if (firstChild instanceof ExpressionContext) {
				extract(tokens, (ExpressionContext)firstChild, symbols, parentSymbol);
			} else {
				if (firstChild instanceof ChainContext) {
					ChainContext chain = (ChainContext) firstChild;
					if (chain.functionCall() != null 
							&& chain.functionCall().functionCallName() != null
							&& chain.functionCall().functionCallName().getText().equals("define")) {
						ArgumentsContext arguments = chain.functionCall().actualArguments().arguments();
						if (arguments.actualArgument().size() == 2) {
							ActualArgumentContext actualArgument = arguments.actualArgument().get(0);
							if (actualArgument.expression() != null) {
								ExpressionContext argExpression = actualArgument.expression();
								if (argExpression.children.size() == 1) {
									ParseTree argExprChild = argExpression.getChild(0);
									if (argExprChild instanceof StringContext) {
										StringContext argExprString = (StringContext)argExprChild;
										if (argExprString.SingleQuoteString() != null) {
											String name = StringUtils.stripEnd(StringUtils.stripStart(
													argExprString.getText(), "'"), "'");
											symbols.add(new ConstantSymbol(namespaceSymbol, name, Visibility.PUBLIC,
													getTokenPosition(tokens, argExprString)));
										} else if (argExprString.DoubleQuote().size() != 0 
												&& !argExprString.getText().contains("$")) {
											String name = StringUtils.stripEnd(StringUtils.stripStart(
													argExprString.getText(), "\""), "\"");
											symbols.add(new ConstantSymbol(namespaceSymbol, name, Visibility.PUBLIC,
													getTokenPosition(tokens, argExprString)));
										}
									} else if (argExprChild instanceof ConstantContext) {
										ConstantContext argExprConstant = (ConstantContext) argExprChild;
										if (argExprConstant.literalConstant() != null 
												&& argExprConstant.literalConstant().stringConstant() != null) {
											symbols.add(new ConstantSymbol(namespaceSymbol, argExprConstant.getText(), 
													Visibility.PUBLIC, getTokenPosition(tokens, argExprConstant)));
										}
									}
								}
							}
						}
					}
				}
				if ((parentSymbol == null || parentSymbol instanceof NamespaceSymbol) 
						&& expression.children.size() >= 3 
						&& expression.getChild(0) instanceof ChainContext 
						&& expression.getChild(1).getText().equals("=")) {
					ChainContext chain = (ChainContext) expression.getChild(0);
					if (chain.chainBase() != null && chain.memberAccess().size() == 0) {
						ChainBaseContext chainBase = chain.chainBase();
						if (chainBase.keyedVariable().size() == 1 && chainBase.qualifiedStaticTypeRef() == null) {
							KeyedVariableContext keyedVariable = chainBase.keyedVariable(0);
							if (keyedVariable.Dollar().size() == 0 
									&& keyedVariable.VarName() != null 
									&& keyedVariable.squareCurlyExpression().size() == 0) {
								TerminalNode varName = keyedVariable.VarName();
								VariableSymbol variableSymbol = findGlobalVariable(symbols, varName.getText());
								if (variableSymbol == null) {
									variableSymbol = new VariableSymbol(null, varName.getText(), 
											Visibility.PUBLIC, Utils.getTokenPosition(varName.getSymbol()));
									symbols.add(variableSymbol);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private String describe(FormalParameterListContext formalParameterList, String source) {
		if (formalParameterList.start.getStartIndex() > formalParameterList.stop.getStopIndex())
			return "()";
		else
			return "(" + source.substring(formalParameterList.start.getStartIndex(), formalParameterList.stop.getStopIndex()+1) + ")";
	}
	
	private void extract(List<? extends Token> tokens, StatementContext statement, List<PhpSymbol> symbols, 
			PhpSymbol parentSymbol, String source) {
		if (statement.nonEmptyStatement() != null) 
			extract(tokens, statement.nonEmptyStatement(), symbols, parentSymbol, source);
	}
	
	private void extract(List<? extends Token> tokens, GlobalConstantDeclarationContext globalConstantDeclaration, 
			List<PhpSymbol> symbols, PhpSymbol parentSymbol) {
		for (IdentifierInititalizerContext identifierInitializer: globalConstantDeclaration.identifierInititalizer()) {
			TokenPosition position = getTokenPosition(tokens, identifierInitializer.identifier());
			String name = identifierInitializer.identifier().getText();
			symbols.add(new ConstantSymbol(parentSymbol, name, Visibility.PUBLIC, position));
		}
	}
	
	private void extract(List<? extends Token> tokens, FunctionDeclarationContext functionDeclaration, 
			List<PhpSymbol> symbols, PhpSymbol parentSymbol, String source) {
		NamespaceSymbol namespaceSymbol = findNamespaceParent(parentSymbol);
		IdentifierContext functionIdentifier = functionDeclaration.identifier();
		String params = describe(functionDeclaration.formalParameterList(), source);
		TokenPosition position = getTokenPosition(tokens, functionIdentifier);
		TokenPosition scope = getTokenPosition(tokens, functionDeclaration);
		String functionName = functionIdentifier.getText();
		String returnType;
		if (functionDeclaration.functionReturnType() != null)
			returnType = ": " + functionDeclaration.functionReturnType().typeHint().getText();
		else
			returnType = null;
		FunctionSymbol functionSymbol = new FunctionSymbol(namespaceSymbol, functionName, Visibility.PUBLIC, params, 
				returnType, position, scope);
		symbols.add(functionSymbol);
		
		extract(tokens, functionDeclaration.blockStatement(), symbols, functionSymbol, source);
	}
	
	private void extract(List<? extends Token> tokens, BlockStatementContext blockStatement, List<PhpSymbol> symbols, 
			PhpSymbol parentSymbol, String source) {
		extract(tokens, blockStatement.innerStatementList(), symbols, parentSymbol, source);
	}
	
	private void extract(List<? extends Token> tokens, InnerStatementListContext innerStatementList, 
			List<PhpSymbol> symbols, PhpSymbol parentSymbol, String source) {
		for (InnerStatementContext innerStatement: innerStatementList.innerStatement()) {
			if (innerStatement.functionDeclaration() != null) {
				extract(tokens, innerStatement.functionDeclaration(), symbols, parentSymbol, source);
			} else if (innerStatement.classDeclaration() != null) {
				extract(tokens, innerStatement.classDeclaration(), symbols, parentSymbol, source);
			} else if (innerStatement.statement() != null) {
				extract(tokens, innerStatement.statement(), symbols, parentSymbol, source);
			}
		}
	}
	
	@Nullable
	private NamespaceSymbol findNamespaceParent(@Nullable PhpSymbol parentSymbol) {
		while (parentSymbol != null) {
			if (parentSymbol instanceof NamespaceSymbol)
				break;
			parentSymbol = parentSymbol.getParent();
		}
		return (NamespaceSymbol) parentSymbol;
	}
	
	@Nullable
	private VariableSymbol findGlobalVariable(List<PhpSymbol> symbols, String varName) {
		for (PhpSymbol symbol: symbols) {
			if (symbol.getParent() == null && symbol instanceof VariableSymbol && symbol.getName().equals(varName))
				return (VariableSymbol) symbol;
		}
		return null;
	}
	
	private void extract(List<? extends Token> tokens, ClassDeclarationContext classDeclaration, 
			List<PhpSymbol> symbols, PhpSymbol parentSymbol, String source) {
		NamespaceSymbol namespaceSymbol = findNamespaceParent(parentSymbol);
		TypeSymbol typeSymbol;
		TypeSymbol.Kind kind;
		IdentifierContext identifier = classDeclaration.identifier();
		if (classDeclaration.classEntryType() != null) {
			if (classDeclaration.classEntryType().Trait() != null) {
				kind = TypeSymbol.Kind.TRAIT;
			} else {
				kind = TypeSymbol.Kind.CLASS;
			}
		} else {
			kind = TypeSymbol.Kind.INTERFACE;
		}
		typeSymbol = new TypeSymbol(namespaceSymbol, identifier.getText(), kind, getTokenPosition(tokens, identifier), 
				getTokenPosition(tokens, classDeclaration));
		symbols.add(typeSymbol);

		for (ClassStatementContext classStatement: classDeclaration.classStatement()) {
			if (!classStatement.variableInitializer().isEmpty()) {
				Visibility visibility = Visibility.PUBLIC;
				if (classStatement.propertyModifiers().memberModifiers() != null)
					visibility = getVisibility(classStatement.propertyModifiers().memberModifiers());
				for (VariableInitializerContext variableInitializer: classStatement.variableInitializer()) {
					TerminalNode varName = variableInitializer.VarName();
					symbols.add(new VariableSymbol(typeSymbol, varName.getText(), visibility, 
							Utils.getTokenPosition(varName.getSymbol())));
				}
			} else if (classStatement.Const() != null) {
				for (IdentifierInititalizerContext identifierInitializer: classStatement.identifierInititalizer()) {
					IdentifierContext constIdentifier = identifierInitializer.identifier();
					Visibility visibility = Visibility.PUBLIC;
					if (classStatement.memberModifiers() != null)
						visibility = getVisibility(classStatement.memberModifiers());
					symbols.add(new ConstantSymbol(typeSymbol, constIdentifier.getText(), visibility, 
							getTokenPosition(tokens, constIdentifier)));
				}
			} else if (classStatement.Function() != null) {
				IdentifierContext functionIdentifier = classStatement.identifier();
				String params = describe(classStatement.formalParameterList(), source);
				TokenPosition position = getTokenPosition(tokens, functionIdentifier);
				TokenPosition scope = getTokenPosition(tokens, classStatement);
				String functionName = functionIdentifier.getText();
				Visibility visibility=  Visibility.PUBLIC;
				if (classStatement.memberModifiers() != null)
					visibility = getVisibility(classStatement.memberModifiers());
				
				String returnType;
				if (classStatement.functionReturnType() != null)
					returnType = ": " + classStatement.functionReturnType().typeHint().getText();
				else
					returnType = null;
				FunctionSymbol functionSymbol = new FunctionSymbol(typeSymbol, functionName, visibility, params, 
						returnType, position, scope);
				symbols.add(functionSymbol);
				
				if (classStatement.methodBody().blockStatement() != null) {
					extract(tokens, classStatement.methodBody().blockStatement(), symbols, functionSymbol, source);
				}
			}
		}
		
	}
	
	private Visibility getVisibility(MemberModifiersContext memberModifiers) {
		Visibility visibility = Visibility.PUBLIC;
		for (MemberModifierContext memberModifier: memberModifiers.memberModifier()) {
			if (memberModifier.getText().equals("private")) {
				visibility = Visibility.PRIVATE;
				break;
			} else if (memberModifier.getText().equals("protected")) {
				visibility = Visibility.PROTECTED;
				break;
			} else if (memberModifier.getText().equals("public")) {
				break;
			}
		}
		return visibility;
	}
	
	private void extract(List<? extends Token> tokens, UseDeclarationContext useDeclaration, List<PhpSymbol> symbols, 
			PhpSymbol parentSymbol) {
		for (UseDeclarationContentContext useDeclarationContent: 
				useDeclaration.useDeclarationContentList().useDeclarationContent()) {
			if (useDeclarationContent.useGroup() != null) {
				UseGroupContext useGroup = useDeclarationContent.useGroup();
				for (UseGroupElementContext useGroupElement: useGroup.useGroupElement()) {
					if (useGroupElement.As() != null) {
						IdentifierContext identifier = useGroupElement.identifier(1);
						symbols.add(new ImportedSymbol(parentSymbol, identifier.getText(), 
								getTokenPosition(tokens, identifier), null));
					}
				}
			} else if (useDeclarationContent.identifier().size() != 0){
				IdentifierContext identifier = useDeclarationContent.identifier(0);
				symbols.add(new ImportedSymbol(parentSymbol, identifier.getText(), getTokenPosition(tokens, identifier), 
						null));
			}
		}
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "php");
	}

	@Override
	public int getVersion() {
		return 0;
	}

}
