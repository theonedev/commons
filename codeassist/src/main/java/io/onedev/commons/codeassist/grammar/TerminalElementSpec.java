package io.onedev.commons.codeassist.grammar;

import java.util.List;

import org.antlr.v4.runtime.Token;

public abstract class TerminalElementSpec extends ElementSpec {

	private static final long serialVersionUID = 1L;

	public TerminalElementSpec(String label, Multiplicity multiplicity) {
		super(label, multiplicity);
	}

	public abstract boolean isToken(int tokenType);

	public int getEndOfMatch(Grammar grammar, String content) {
		List<Token> tokens = grammar.lex(content);
		if (tokens.size() == 1) {
			Token token = tokens.get(0);
			if (token.getStartIndex() == 0 && isToken(token.getType()))
				return token.getStopIndex() + 1;
		} 
		return -1;
	}
	
	public boolean matches(Grammar grammar, String content) {
		return getEndOfMatch(grammar, content) == content.length();
	}
	
}