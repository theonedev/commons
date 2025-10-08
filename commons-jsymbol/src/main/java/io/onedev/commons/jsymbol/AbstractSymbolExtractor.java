package io.onedev.commons.jsymbol;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;

public abstract class AbstractSymbolExtractor<T extends Symbol> implements SymbolExtractor<T> {

	protected boolean acceptExtensions(@Nullable String filePath, String...exts) {
		String fileExt = StringUtils.substringAfterLast(filePath, ".");
		for (String ext: exts) {
			if (ext.equalsIgnoreCase(fileExt))
				return true;
		}
		return false;
	}

	public List<T> extract(String fileName, List<String> fileContent) {
		return extract(fileName, Joiner.on("\n").join(fileContent));
	}
	
	private DFA[] newDFA(Recognizer<?, ?> recognizer) {
		ATN atn = recognizer.getATN();
		DFA[] dfa = new DFA[atn.getNumberOfDecisions()];
		for (int i = 0; i < atn.getNumberOfDecisions(); i++) {
			dfa[i] = new DFA(atn.getDecisionState(i), i);
		}
		return dfa;
	}
	
	protected void clearLexerCache(Lexer lexer) {
		lexer.setInterpreter(new LexerATNSimulator(lexer, lexer.getATN(), newDFA(lexer), new PredictionContextCache()));
	}
	
	protected void clearParserCache(Parser parser) {
		parser.setInterpreter(new ParserATNSimulator(parser, parser.getATN(), newDFA(parser), 
				new PredictionContextCache()));
	}
	
}
