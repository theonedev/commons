package io.onedev.commons.jsymbol.less;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.onedev.commons.jsymbol.AbstractSymbolExtractor;
import io.onedev.commons.jsymbol.TokenPosition;
import io.onedev.commons.jsymbol.less.symbols.ClassSymbol;
import io.onedev.commons.jsymbol.less.symbols.IdSymbol;
import io.onedev.commons.jsymbol.less.symbols.LessSymbol;
import io.onedev.commons.jsymbol.less.symbols.MixinSymbol;
import io.onedev.commons.jsymbol.less.symbols.SelectorSymbol;
import io.onedev.commons.jsymbol.less.symbols.VariableSymbol;
import io.onedev.commons.jsymbol.util.QualifiedName;
import io.onedev.commons.jsymbol.util.Utils;
import io.onedev.commons.jsymbol.less.LessLexer;
import io.onedev.commons.jsymbol.less.LessParser;
import io.onedev.commons.jsymbol.less.LessParser.BlockContext;
import io.onedev.commons.jsymbol.less.LessParser.ElementContext;
import io.onedev.commons.jsymbol.less.LessParser.MixinDefinitionContext;
import io.onedev.commons.jsymbol.less.LessParser.RulesetContext;
import io.onedev.commons.jsymbol.less.LessParser.SelectorContext;
import io.onedev.commons.jsymbol.less.LessParser.SelectorsContext;
import io.onedev.commons.jsymbol.less.LessParser.StatementContext;
import io.onedev.commons.jsymbol.less.LessParser.StylesheetContext;
import io.onedev.commons.jsymbol.less.LessParser.VariableDeclarationContext;

public class LessExtractor extends AbstractSymbolExtractor<LessSymbol> {

	private static final Logger logger = LoggerFactory.getLogger(LessExtractor.class);
	
	@Override
	public List<LessSymbol> extract(String fileName, String fileContent) {
		List<LessSymbol> symbols = new ArrayList<>();

		ANTLRErrorListener errorListener = new BaseErrorListener() {

			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				logger.trace("{}: {}: {}", line, charPositionInLine, msg);
			}
			
		};
		
		LessLexer lexer = new LessLexer(new ANTLRInputStream(fileContent));
		lexer.removeErrorListeners();
		lexer.addErrorListener(errorListener);
		
		clearLexerCache(lexer);
		
		CommonTokenStream stream = new CommonTokenStream(lexer);
		LessParser parser = new LessParser(stream);
		parser.removeErrorListeners();
		parser.addErrorListener(errorListener);
		
		clearParserCache(parser);
		
		extract(parser.stylesheet(), symbols, null, fileContent);
		
		return symbols;
	}
	
	private void extract(StylesheetContext stylesheet, List<LessSymbol> symbols, LessSymbol parentSymbol, 
			String source) {
		for (StatementContext statement: stylesheet.statement()) 
			extract(statement, symbols, parentSymbol, source);
	}

	private MixinSymbol findEnclosingMixin(LessSymbol parentSymbol) {
		while (parentSymbol != null && !(parentSymbol instanceof MixinSymbol)) {
			parentSymbol = parentSymbol.getParent();
		}
		return (MixinSymbol) parentSymbol;
	}
	
	private LessSymbol extract(SelectorContext selector, List<LessSymbol> symbols, LessSymbol parentSymbol, 
			String source) {
		for (ElementContext element: selector.element()) {
			// if the element contains identifier without interpolation part
			if (element.identifier() != null && !element.identifier().getText().contains("@{")) { 
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
	
	private void extract(StatementContext statement, List<LessSymbol> symbols, LessSymbol parentSymbol, String source) {
		if (statement.ruleset() != null) {
			RulesetContext ruleset = statement.ruleset();
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
		} else if (statement.mixinDefinition() != null) {
			MixinDefinitionContext mixinDefinition = statement.mixinDefinition();
			SelectorsContext selectors = mixinDefinition.selectors();
			String mixinName = selectors.getText();
			String prefix;
			if (mixinName.startsWith(".")) {
				mixinName = mixinName.substring(1);
				prefix = ".";
			} else if (mixinName.startsWith("#")) {
				mixinName = mixinName.substring(1);
				prefix = "#";
			} else {
				prefix = null;
			}
			
			String params = "(" + mixinDefinition.ignoreInsideParens().getText() + ")";
			MixinSymbol mixinSymbol = new MixinSymbol(parentSymbol, mixinName, prefix, params,
					Utils.getTokenPosition(selectors), 
					Utils.getTokenPosition(mixinDefinition));
			symbols.add(mixinSymbol);
			extract(mixinDefinition.block(), symbols, mixinSymbol, source);
		} else if (statement.variableDeclaration() != null) {
			VariableDeclarationContext variableDeclaration = statement.variableDeclaration();
			TerminalNode identifier = variableDeclaration.variableName().Identifier(); 
			if (identifier != null) {
				String name = identifier.getText();
				TokenPosition position = Utils.getTokenPosition(identifier.getSymbol());
				boolean local = findEnclosingMixin(parentSymbol) != null;
				VariableSymbol variableSymbol = new VariableSymbol(parentSymbol, name, position, 
						Utils.getTokenPosition(variableDeclaration), local);
				symbols.add(variableSymbol);
			}
		}
	}
	
	private void extract(BlockContext block, List<LessSymbol> symbols, LessSymbol parentSymbol, String source) {
		for (StatementContext statement: block.statement())
			extract(statement, symbols, parentSymbol, source);
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "less");
	}

	@Override
	public int getVersion() {
		return 1;
	}

}
