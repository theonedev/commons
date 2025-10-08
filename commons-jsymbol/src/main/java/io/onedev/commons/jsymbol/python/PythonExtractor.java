package io.onedev.commons.jsymbol.python;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.python.Python3Parser.Async_stmtContext;
import io.onedev.commons.jsymbol.python.Python3Parser.ClassdefContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Compound_stmtContext;
import io.onedev.commons.jsymbol.python.Python3Parser.DecoratedContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Dotted_as_nameContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Expr_assignmentContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Expr_stmtContext;
import io.onedev.commons.jsymbol.python.Python3Parser.FuncdefContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Import_as_nameContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Import_nameContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Import_stmtContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Simple_stmtContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Small_stmtContext;
import io.onedev.commons.jsymbol.python.Python3Parser.StmtContext;
import io.onedev.commons.jsymbol.python.Python3Parser.SuiteContext;
import io.onedev.commons.jsymbol.python.Python3Parser.Testlist_star_exprContext;
import io.onedev.commons.jsymbol.python.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.python.symbols.ImportedSymbol;
import io.onedev.commons.jsymbol.python.symbols.MethodSymbol;
import io.onedev.commons.jsymbol.python.symbols.PythonSymbol;
import io.onedev.commons.jsymbol.python.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.jsymbol.util.Utils;

public class PythonExtractor extends AbstractSymbolExtractor<PythonSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(PythonExtractor.class);
	
	@Override
	public List<PythonSymbol> extract(String fileName, String fileContent) {
		List<PythonSymbol> symbols = new ArrayList<>();

		ANTLRErrorListener errorListener = new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				logger.debug("{}: {}: {}", line, charPositionInLine, msg);
			}
			
		};
		
		Python3Lexer lexer = new Python3Lexer(CharStreams.fromString(fileContent));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		
		clearLexerCache(lexer);

		CommonTokenStream stream = new CommonTokenStream(lexer);
		Python3Parser parser = new Python3Parser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		
		clearParserCache(parser);
		
		for (StmtContext stmt: parser.file_input().stmt()) {
			extract(stmt, symbols, null);
		}
		
		/*
		 * Remove all symbols not contributing substantial information to outline
		 */
		for (Iterator<PythonSymbol> it = symbols.iterator(); it.hasNext();) {
			PythonSymbol symbol = it.next();
			if (symbol instanceof VariableSymbol) {
				VariableSymbol variableSymbol = (VariableSymbol) symbol;
				if (variableSymbol.isLocal() && !variableSymbol.isAttribute() 
						&& getChildren(symbols, symbol).isEmpty()) {
					it.remove();
				} 
			}
		}
		
		return symbols;
	}
	
	private List<PythonSymbol> getChildren(List<PythonSymbol> symbols, @Nullable PythonSymbol parentSymbol) {
		List<PythonSymbol> children = new ArrayList<>();
		for (PythonSymbol symbol: symbols) {
			if (symbol.getParent() == parentSymbol)
				children.add(symbol);
		}
		return children;
	}
	
	private void extract(StmtContext stmt, List<PythonSymbol> symbols, PythonSymbol parentSymbol) {
		if (stmt.simple_stmt() != null)
			extract(stmt.simple_stmt(), symbols, parentSymbol);
		else
			extract(stmt.compound_stmt(), symbols, parentSymbol);
	}
	
	private void extract(Simple_stmtContext simpleStmt, List<PythonSymbol> symbols, PythonSymbol parentSymbol) {
		for (Small_stmtContext smallStmt: simpleStmt.small_stmt()) {
			if (smallStmt.import_stmt() != null) {
				Import_stmtContext importStmt = smallStmt.import_stmt();
				if (importStmt.import_name() != null) {
					Import_nameContext importName = importStmt.import_name();
					for (Dotted_as_nameContext dottedAsName: importName.dotted_as_names().dotted_as_name()) {
						PlanarRange scope = Utils.getTextRange(dottedAsName);
						if (dottedAsName.NAME() != null) {
							PlanarRange position = Utils.getTextRange(dottedAsName.NAME().getSymbol());
							QualifiedName name = new QualifiedName(dottedAsName.NAME().getText(), null, null);
							symbols.add(new ImportedSymbol(parentSymbol, name, position, scope));
						} else {
							TerminalNode name = dottedAsName.dotted_name().NAME(dottedAsName.dotted_name().NAME().size()-1);
							PlanarRange position = Utils.getTextRange(name.getSymbol());
							symbols.add(new ImportedSymbol(parentSymbol, new QualifiedName(dottedAsName.dotted_name().getText(), "."), 
									position, scope));
						}
					}
				} else if (importStmt.import_from() != null && importStmt.import_from().import_as_names() != null) {
					for (Import_as_nameContext importAsName: 
							importStmt.import_from().import_as_names().import_as_name()) {
						PlanarRange scope = Utils.getTextRange(importAsName);
						TerminalNode name;
						if (importAsName.NAME().size()>1) {
							name = importAsName.NAME().get(importAsName.NAME().size()-1);
						} else {
							name = importAsName.NAME().get(0);
						}
						PlanarRange position = Utils.getTextRange(name.getSymbol());
						symbols.add(new ImportedSymbol(parentSymbol, new QualifiedName(name.getText()), 
								position, scope));
					}
				}
			} else if (smallStmt.expr_stmt() != null) {
				extract(smallStmt.expr_stmt(), symbols, parentSymbol);
			}
		}
	}
	
	private void extract(Expr_stmtContext exprStmt, List<PythonSymbol> symbols, PythonSymbol parentSymbol) {
		List<List<Token>> variablePaths = getVariablePaths(exprStmt.testlist_star_expr());
		if (variablePaths.isEmpty())
			return;
		for (Expr_assignmentContext expAssignment: exprStmt.expr_assignment()) {
			for (List<Token> variablePath: variablePaths)
				processAssignment(symbols, parentSymbol, variablePath);
			if (expAssignment.testlist_star_expr() != null) {
				variablePaths = getVariablePaths(expAssignment.testlist_star_expr());
				if (variablePaths.isEmpty())
					return;
			}
		}
	}

	private void extract(FuncdefContext funcDef, List<PythonSymbol> symbols, PythonSymbol parentSymbol) {
		PlanarRange position = Utils.getTextRange(funcDef.NAME().getSymbol());
		PlanarRange scope = Utils.getTextRange(funcDef);
		String params = funcDef.parameters().getText();
		MethodSymbol methodSymbol = new MethodSymbol(parentSymbol, funcDef.NAME().getText(), position, scope, params);
		symbols.add(methodSymbol);
		extract(funcDef.suite(), symbols, methodSymbol);
	}
	
	private List<Token> getTokens(ParseTree tree) {
		List<Token> tokens = new ArrayList<>();
		for (int i=0; i<tree.getChildCount(); i++) {
			ParseTree child = tree.getChild(i);
			if (child instanceof TerminalNode) {
				tokens.add(((TerminalNode)child).getSymbol());
			} else {
				tokens.addAll(getTokens(child));
			}
		}
		return tokens;
	}
	
	private List<List<Token>> getVariablePaths(Testlist_star_exprContext testListStarExpr) {
		List<List<Token>> variablePaths = new ArrayList<>();
		List<Token> variablePath = new ArrayList<>();
		for (Token token: getTokens(testListStarExpr)) {
			if (token.getType() == Python3Lexer.COMMA) {
				if (!variablePath.isEmpty()) {
					variablePaths.add(variablePath);
					variablePath = new ArrayList<>();
				}
			}
			if (token.getType() == Python3Lexer.NAME)
				variablePath.add(token);
			else if (token.getType() != Python3Lexer.DOT && token.getType() != Token.EOF)
				return new ArrayList<>();
		}
		if (!variablePath.isEmpty())
			variablePaths.add(variablePath);
		return variablePaths;
	}
	
	private void extract(ClassdefContext classDef, List<PythonSymbol> symbols, PythonSymbol parentSymbol) {
		PlanarRange position = Utils.getTextRange(classDef.NAME().getSymbol());
		PlanarRange scope = Utils.getTextRange(classDef);
		String params;
		if (classDef.arglist() != null)
			params = "(" + classDef.arglist().getText() + ")";
		else
			params = null;
		ClassSymbol classSymbol = new ClassSymbol(parentSymbol, classDef.NAME().getText(), position, scope, params);
		symbols.add(classSymbol);
		extract(classDef.suite(), symbols, classSymbol);
	}
	
	private void extract(Compound_stmtContext compoundStmt, List<PythonSymbol> symbols, PythonSymbol parentSymbol) {
		if (compoundStmt.funcdef() != null) {
			extract(compoundStmt.funcdef(), symbols, parentSymbol);
		} else if (compoundStmt.classdef() != null) {
			extract(compoundStmt.classdef(), symbols, parentSymbol);
		} else if (compoundStmt.if_stmt() != null) {
			for (SuiteContext suite: compoundStmt.if_stmt().suite()) {
				extract(suite, symbols, parentSymbol);
			}
		} else if (compoundStmt.while_stmt() != null) {
			for (SuiteContext suite: compoundStmt.while_stmt().suite()) {
				extract(suite, symbols, parentSymbol);
			}
		} else if (compoundStmt.for_stmt() != null) {
			for (SuiteContext suite: compoundStmt.for_stmt().suite()) {
				extract(suite, symbols, parentSymbol);
			}
		} else if (compoundStmt.try_stmt() != null) {
			for (SuiteContext suite: compoundStmt.try_stmt().suite()) {
				extract(suite, symbols, parentSymbol);
			}
		} else if (compoundStmt.with_stmt() != null) {
			extract(compoundStmt.with_stmt().suite(), symbols, parentSymbol);
		} else if (compoundStmt.decorated() != null) {
			DecoratedContext decorated = compoundStmt.decorated();
			if (decorated.funcdef() != null) 
				extract(decorated.funcdef(), symbols, parentSymbol);
			else if (decorated.classdef() != null) 
				extract(decorated.classdef(), symbols, parentSymbol);
			else if (decorated.async_funcdef() != null)
				extract(decorated.async_funcdef().funcdef(), symbols, parentSymbol);
		} else if (compoundStmt.async_stmt() != null) {
			Async_stmtContext asyncStmt = compoundStmt.async_stmt();
			if (asyncStmt.funcdef() != null) {
				extract(asyncStmt.funcdef(), symbols, parentSymbol);
			} else if (asyncStmt.with_stmt() != null) {
				extract(asyncStmt.with_stmt().suite(), symbols, parentSymbol);
			} else if (asyncStmt.for_stmt() != null) {
				for (SuiteContext suite: asyncStmt.for_stmt().suite()) {
					extract(suite, symbols, parentSymbol);
				}
			}
		}
	}
	
	@Nullable
	private PythonSymbol getChild(List<PythonSymbol> symbols, @Nullable PythonSymbol parent, String... childPath) {
		Preconditions.checkArgument(childPath.length != 0);
		String childName = childPath[0];
		PythonSymbol child = null;
		for (PythonSymbol symbol: symbols) {
			if (symbol.getParent() == parent && childName.equals(symbol.getName())) {
				child = symbol;
				break;
			}
		}
		if (child != null && childPath.length > 1) {
			return getChild(symbols, child, Arrays.copyOfRange(childPath, 1, childPath.length));
		} else {
			return child;
		}
	}
	
	@Nullable
	private PythonSymbol getSymbolInHierarchy(List<PythonSymbol> symbols, @Nullable PythonSymbol parent, 
			String symbolName) {
		while (true) {
			PythonSymbol symbol = getChild(symbols, parent, symbolName);
			if (symbol != null)
				return symbol;
			else if (parent == null)
				return null;
			parent = (PythonSymbol) parent.getParent();
		}
	}

	private ClassSymbol findEnclosingClass(PythonSymbol parentSymbol) {
		while (parentSymbol != null && !(parentSymbol instanceof ClassSymbol)) {
			parentSymbol = parentSymbol.getParent();
		}
		return (ClassSymbol) parentSymbol;
	}
	
	private void processAssignment(List<PythonSymbol> symbols, PythonSymbol parentSymbol, List<Token> tokenPath) {
		if (!tokenPath.isEmpty()) {
			Token rootToken = tokenPath.get(0);
			PythonSymbol symbol = null;

			boolean isSelf = rootToken.getText().equals("self");
			if (isSelf) {
				symbol = findEnclosingClass(parentSymbol);
			} 
			if (symbol == null) {
				if (tokenPath.size() != 1)
					symbol = getSymbolInHierarchy(symbols, parentSymbol, rootToken.getText());
				else
					symbol = getChild(symbols, parentSymbol, rootToken.getText());
			}
			if (symbol == null) {
				PlanarRange position = Utils.getTextRange(rootToken);
				symbol = new VariableSymbol(parentSymbol, rootToken.getText(), position, null, 
						parentSymbol instanceof ClassSymbol);
				symbols.add(symbol);
			}
			
			/*
			 * Then we iterate over all subsequent identifiers to find the child symbol under root symbol
			 */
			for (int i=1; i<tokenPath.size(); i++) {
				Token token = tokenPath.get(i);
				PythonSymbol childSymbol = getChild(symbols, symbol, token.getText());
				if (childSymbol == null) {
					PlanarRange position = Utils.getTextRange(token);
					childSymbol = new VariableSymbol(symbol, token.getText(), position, null, true);
					
					/*
					 * We want to insert instance properties to start of class definition to make the outline clear					
					 */
					if (symbol instanceof ClassSymbol && isSelf) 
						symbols.add(getPropertyInsertPosition(symbols, (ClassSymbol) symbol), childSymbol);
					else
						symbols.add(childSymbol);
				}
				symbol = childSymbol;
			}
		}
	}
	
	private int getPropertyInsertPosition(List<PythonSymbol> symbols, ClassSymbol classSymbol) {
		int index = symbols.indexOf(classSymbol) + 1;
		while (index < symbols.size()) {
			PythonSymbol symbol = symbols.get(index);
			if (symbol.getParent() != classSymbol || !(symbol instanceof VariableSymbol))
				break;
			else
				index++;
		}
		return index;
	}
	
	private void extract(SuiteContext suite, List<PythonSymbol> symbols, PythonSymbol parentSymbol) {
		for (StmtContext stmt: suite.stmt()) {
			extract(stmt, symbols, parentSymbol);
		}
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "py");
	}

	@Override
	public int getVersion() {
		return 1;
	}

}
