package io.onedev.commons.jsymbol.scss;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.scss.ScssParser.BlockContext;
import io.onedev.commons.jsymbol.scss.ScssParser.ElementContext;
import io.onedev.commons.jsymbol.scss.ScssParser.ElseIfStatementContext;
import io.onedev.commons.jsymbol.scss.ScssParser.FunctionDeclarationContext;
import io.onedev.commons.jsymbol.scss.ScssParser.FunctionStatementContext;
import io.onedev.commons.jsymbol.scss.ScssParser.IfDeclarationContext;
import io.onedev.commons.jsymbol.scss.ScssParser.MixinDeclarationContext;
import io.onedev.commons.jsymbol.scss.ScssParser.NestedContext;
import io.onedev.commons.jsymbol.scss.ScssParser.RulesetContext;
import io.onedev.commons.jsymbol.scss.ScssParser.SelectorContext;
import io.onedev.commons.jsymbol.scss.ScssParser.StatementContext;
import io.onedev.commons.jsymbol.scss.ScssParser.StylesheetContext;
import io.onedev.commons.jsymbol.scss.ScssParser.VariableDeclarationContext;
import io.onedev.commons.jsymbol.scss.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.scss.symbols.FunctionSymbol;
import io.onedev.commons.jsymbol.scss.symbols.IdSymbol;
import io.onedev.commons.jsymbol.scss.symbols.MixinSymbol;
import io.onedev.commons.jsymbol.scss.symbols.ScssSymbol;
import io.onedev.commons.jsymbol.scss.symbols.SelectorSymbol;
import io.onedev.commons.jsymbol.scss.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.jsymbol.util.Utils;

public class ScssExtractor extends AbstractSymbolExtractor<ScssSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(ScssExtractor.class);
	
	@Override
	public List<ScssSymbol> extract(String fileName, String fileContent) {
		List<ScssSymbol> symbols = new ArrayList<>();

		ANTLRErrorListener errorListener = new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				logger.trace("{}: {}: {}", line, charPositionInLine, msg);
			}
			
		};
		
		ScssLexer lexer = new ScssLexer(CharStreams.fromString(fileContent));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		
		clearLexerCache(lexer);
		
		CommonTokenStream stream = new CommonTokenStream(lexer);
		ScssParser parser = new ScssParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		
		clearParserCache(parser);
		
		extract(parser.stylesheet(), symbols, null, fileContent);
		
		return symbols;
	}
	
	private void extract(StylesheetContext stylesheet, List<ScssSymbol> symbols, ScssSymbol parentSymbol, 
			String source) {
		for (StatementContext statement: stylesheet.statement()) 
			extract(statement, symbols, parentSymbol, source);
	}

	private MixinSymbol findEnclosingMixin(ScssSymbol parentSymbol) {
		while (parentSymbol != null && !(parentSymbol instanceof MixinSymbol)) {
			parentSymbol = parentSymbol.getParent();
		}
		return (MixinSymbol) parentSymbol;
	}
	
	private FunctionSymbol findEnclosingFunction(ScssSymbol parentSymbol) {
		while (parentSymbol != null && !(parentSymbol instanceof FunctionSymbol)) {
			parentSymbol = parentSymbol.getParent();
		}
		return (FunctionSymbol) parentSymbol;
	}
	
	private ScssSymbol extract(SelectorContext selector, List<ScssSymbol> symbols, ScssSymbol parentSymbol, 
			String source) {
		for (ElementContext element: selector.element()) {
			// if the element contains identifier without interpolation part
			if (element.identifier() != null && element.PERC() == null 
					&& !element.identifier().getText().contains("#{")) { 
				String unqualifiedName = element.identifier().getText();
				String prefix = source.substring(selector.start.getStartIndex(), 
						element.identifier().start.getStartIndex());
				if (prefix.length() == 0)
					prefix = null;
				String suffix = source.substring(element.identifier().stop.getStopIndex()+1, 
						selector.stop.getStopIndex()+1);
				if (suffix.length() == 0)
					suffix = null;
				QualifiedName name = new QualifiedName(unqualifiedName, prefix, suffix); 
				TokenPosition position = Utils.getTokenPosition(element.identifier());
				if (element.HASH() != null) 
					symbols.add(new IdSymbol(parentSymbol, name, position));
				else if (element.DOT() != null)
					symbols.add(new ClassSymbol(parentSymbol, name, position));
			}
		}
		
		String selectorText = source.substring(selector.start.getStartIndex(), selector.stop.getStopIndex()+1);
		SelectorSymbol selectorSymbol = new SelectorSymbol(parentSymbol, selectorText, 
				Utils.getTokenPosition(selector));
		symbols.add(selectorSymbol);
		return selectorSymbol;
	}
	
	private void extract(StatementContext statement, List<ScssSymbol> symbols, ScssSymbol parentSymbol, String source) {
		if (statement.nested() != null) {
			NestedContext nested = statement.nested();
			for (SelectorContext selector: nested.selectors().selector()) {
				ScssSymbol elementSymbol = extract(selector, symbols, parentSymbol, source);
				extract(nested.stylesheet(), symbols, elementSymbol, source);
			}
			if (nested.selectors().directive() != null) {
				String directiveText = source.substring(
						nested.selectors().directive().start.getStartIndex(), 
						nested.selectors().directive().stop.getStopIndex()+1);
				SelectorSymbol directiveSymbol = new SelectorSymbol(parentSymbol, directiveText, 
						Utils.getTokenPosition(nested.selectors().directive()));
				symbols.add(directiveSymbol);
				extract(nested.stylesheet(), symbols, directiveSymbol, source);
			}
		} else if (statement.ruleset() != null) {
			RulesetContext ruleset = statement.ruleset();
			
			/* 
			 * ruleset ends with colon are actually not a ruleset, it is a mixin property partial name, for instance:
			 * 
			 * @mixin large-text {
  			 *   font: {
    		 *	   family: Arial;
    		 *	   size: 20px;
    		 * 	   weight: bold;
  			 *   }
             *   color: #ff0000;
             * }
             * 
             * Here font is not a selector, and it will be compiled as below css:
             * font-family: Arial;
             * font-size: 20px;
             * font-weight: bold;
             * color: #ff0000;
			 */
			if (ruleset.COLON() == null) { 
				for (SelectorContext selector: ruleset.selectors().selector())
					extract(ruleset.block(), symbols, extract(selector, symbols, parentSymbol, source), source);
				if (ruleset.selectors().directive() != null) {
					String directiveText = source.substring(
							ruleset.selectors().directive().start.getStartIndex(), 
							ruleset.selectors().directive().stop.getStopIndex()+1);
					SelectorSymbol directiveSymbol = new SelectorSymbol(parentSymbol, directiveText, 
							Utils.getTokenPosition(ruleset.selectors().directive()));
					symbols.add(directiveSymbol);
					extract(ruleset.block(), symbols, directiveSymbol, source);
				}
			}
		} else if (statement.mixinDeclaration() != null) {
			MixinDeclarationContext mixinDeclaration = statement.mixinDeclaration();
			String name = mixinDeclaration.Identifier().getText();
			String params;
			if (mixinDeclaration.LPAREN() != null) {
				if (mixinDeclaration.params()!= null)
					params = "(" + mixinDeclaration.params().getText() + ")";
				else
					params = "()";
			} else {
				params = null;
			}
			boolean local = findEnclosingFunction(parentSymbol) != null || findEnclosingMixin(parentSymbol) != null;
			MixinSymbol mixinSymbol = new MixinSymbol(parentSymbol, name, params,
					Utils.getTokenPosition(mixinDeclaration.Identifier().getSymbol()), 
					Utils.getTokenPosition(mixinDeclaration), local);
			symbols.add(mixinSymbol);
			extract(mixinDeclaration.block(), symbols, mixinSymbol, source);
		} else if (statement.functionDeclaration() != null) {
			FunctionDeclarationContext functionDeclaration = statement.functionDeclaration();
			String name = functionDeclaration.Identifier().getText();
			String params;
			if (functionDeclaration.LPAREN() != null) {
				if (functionDeclaration.params()!= null)
					params = "(" + functionDeclaration.params().getText() + ")";
				else
					params = "()";
			} else {
				params = null;
			}
			boolean local = findEnclosingFunction(parentSymbol) != null || findEnclosingMixin(parentSymbol) != null;
			FunctionSymbol functionSymbol = new FunctionSymbol(parentSymbol, name, params,
					Utils.getTokenPosition(functionDeclaration.Identifier().getSymbol()), 
					Utils.getTokenPosition(functionDeclaration), local);
			symbols.add(functionSymbol);
			if (functionDeclaration.functionBody() != null) {
				for (FunctionStatementContext functionStatement: 
						functionDeclaration.functionBody().functionStatement()) {
					if (functionStatement.statement() != null) 
						extract(functionStatement.statement(), symbols, functionSymbol, source);
				}
			}
		} else if (statement.variableDeclaration() != null) {
			VariableDeclarationContext variableDeclaration = statement.variableDeclaration();
			TerminalNode identifier = variableDeclaration.variableName().Identifier(); 
			String name = identifier.getText();
			TokenPosition position = Utils.getTokenPosition(identifier.getSymbol());
			boolean local = findEnclosingFunction(parentSymbol) != null || findEnclosingMixin(parentSymbol) != null;
			VariableSymbol variableSymbol = new VariableSymbol(parentSymbol, name, position, 
					Utils.getTokenPosition(variableDeclaration), local);
			symbols.add(variableSymbol);
		} else if (statement.includeDeclaration() != null) {
			if (statement.includeDeclaration().block() != null)
				extract(statement.includeDeclaration().block(), symbols, parentSymbol, source);
		} else if (statement.ifDeclaration() != null) {
			IfDeclarationContext ifDeclaration = statement.ifDeclaration();
			extract(ifDeclaration.block(), symbols, parentSymbol, source);
			for (ElseIfStatementContext elseIfStatement: ifDeclaration.elseIfStatement())
				extract(elseIfStatement.block(), symbols, parentSymbol, source);
			if (ifDeclaration.elseStatement() != null)
				extract(ifDeclaration.elseStatement().block(), symbols, parentSymbol, source);
		} else if (statement.forDeclaration() != null) {
			extract(statement.forDeclaration().block(), symbols, parentSymbol, source);
		} else if (statement.whileDeclaration() != null) {
			extract(statement.whileDeclaration().block(), symbols, parentSymbol, source);
		} else if (statement.eachDeclaration() != null) {
			extract(statement.eachDeclaration().block(), symbols, parentSymbol, source);
		}
	}
	
	private void extract(BlockContext block, List<ScssSymbol> symbols, ScssSymbol parentSymbol, String source) {
		for (StatementContext statement: block.statement())
			extract(statement, symbols, parentSymbol, source);
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "scss", "css");
	}

	@Override
	public int getVersion() {
		return 1;
	}

}
