package io.onedev.commons.jsymbol.r;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.r.RParser.ExprContext;
import io.onedev.commons.jsymbol.r.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.r.symbols.RSymbol;
import io.onedev.commons.jsymbol.r.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.util.Utils;

public class RExtractor extends AbstractSymbolExtractor<RSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(RExtractor.class);
	
	@Override
	public List<RSymbol> extract(String fileName, String fileContent) {
		List<RSymbol> symbols = new ArrayList<>();

		ANTLRErrorListener errorListener = new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				logger.trace("{}: {}: {}", line, charPositionInLine, msg);
			}
			
		};
		
		RLexer lexer = new RLexer(CharStreams.fromString(fileContent));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		
		clearLexerCache(lexer);

		CommonTokenStream stream = new CommonTokenStream(lexer);
		RParser parser = new RParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		
		clearParserCache(parser);
		
		for (ExprContext expr: parser.prog().exprlist().expr()) {
			extract(expr, symbols, null);
		}
		
		return symbols;
	}
	
	private void extract(ExprContext expr, List<RSymbol> symbols, @Nullable RSymbol parentSymbol) {
		if (expr.assignment != null) {
			ExprContext nameExpr;
			ExprContext valueExpr;
			if (expr.assignment.getText().equals("<-") || expr.assignment.getText().equals("<<-") 
					|| expr.assignment.getText().equals("=") || expr.assignment.getText().equals(":=")) {
				nameExpr = expr.expr(0);
				valueExpr = expr.expr(1);
			} else {
				nameExpr = expr.expr(1);
				valueExpr = expr.expr(0);
			}
			if (nameExpr.unaryExpr()!=null && nameExpr.unaryExpr().ID() != null 
					&& getChild(symbols, parentSymbol, nameExpr.getText()) == null) {
				PlanarRange position = Utils.getTextRange(nameExpr);
				if (valueExpr.FUNCTION() != null) {
					PlanarRange scope = Utils.getTextRange(valueExpr);
					String params = "(" + valueExpr.ignoreInsideParens().getText() + ")";
					FunctionSymbol functionSymbol = new FunctionSymbol(parentSymbol, nameExpr.getText(), position, 
							scope, params, parentSymbol!=null);
					symbols.add(functionSymbol);
					extract(valueExpr.expr(0), symbols, functionSymbol);
				} else {
					VariableSymbol variableSymbol = new VariableSymbol(parentSymbol, nameExpr.getText(), position, null, 
							parentSymbol!=null);
					symbols.add(variableSymbol);
				}
			}
		} else if (expr.exprlist() != null) {
			for (ExprContext eachExpr: expr.exprlist().expr()) {
				extract(eachExpr, symbols, parentSymbol);
			}
		} else if (expr.unaryExpr() != null && expr.unaryExpr().parensExpr() != null) {
			extract(expr.unaryExpr().parensExpr().expr(), symbols, parentSymbol);
		} else if (expr.ifExpr() != null) {
			extract(expr.ifExpr().expr(), symbols, parentSymbol);
		} else if (expr.ifElseExpr() != null) {
			extract(expr.ifElseExpr().expr(0), symbols, parentSymbol);
			extract(expr.ifElseExpr().expr(1), symbols, parentSymbol);
		} else if (expr.forExpr() != null) {
			extract(expr.forExpr().expr(), symbols, parentSymbol);
		} else if (expr.whileExpr() != null) {
			extract(expr.whileExpr().expr(), symbols, parentSymbol);
		} else if (expr.repeatExpr() != null) {
			extract(expr.repeatExpr().expr(), symbols, parentSymbol);
		}
	}

	@Nullable
	private RSymbol getChild(List<RSymbol> symbols, @Nullable RSymbol parentSymbol, String childName) {
		for (RSymbol symbol: symbols) {
			if (symbol.getParent() == parentSymbol && symbol.getName().equals(childName))
				return symbol;
		}
		return null;
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "r", "R");
	}

	@Override
	public int getVersion() {
		return 0;
	}

}
