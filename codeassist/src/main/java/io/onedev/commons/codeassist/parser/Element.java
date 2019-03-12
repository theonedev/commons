package io.onedev.commons.codeassist.parser;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.Token;

import io.onedev.commons.codeassist.grammar.ElementSpec;

/**
 * Represent a matched element.  
 * 
 * @author robin
 *
 */
public class Element {
	
	private final ElementSpec spec;
	
	private final int nextTokenIndex;
	
	private final State state;
	
	private final EarleyParser parser;
	
	public Element(EarleyParser parser, @Nullable ElementSpec spec, int nextTokenIndex, @Nullable State state) {
		this.parser = parser;
		this.spec = spec;
		this.nextTokenIndex = nextTokenIndex;
		this.state = state;
	}

	/**
	 * Get spec of this element
	 * 
	 * @return
	 * 			spec of this element, or <tt>null</tt> for root element, as root element 
	 * 			is a pseudo element wrapping the root state
	 */
	@Nullable
	public ElementSpec getSpec() {
		return spec;
	}
	
	public boolean isRoot() {
		return spec == null;
	}
	
	public boolean isTerminal() {
		return state == null;
	}
	
	/**
	 * Get label of this element defined in grammar
	 * 
	 * @return
	 * 			label of this element, or <tt>null</tt> if there is no label associated 
	 * 			with the element 
	 */
	@Nullable
	public String getLabel() {
		return spec!=null?spec.getLabel():null;
	}

	/**
	 * Get position of the chart the element is matched in 
	 * 
	 * @return
	 * 			position of the chart the element is matched in. It also represents index of 
	 * 			next token after matching of this element
	 */
	public int getNextTokenIndex() {
		return nextTokenIndex;
	}

	/**
	 * Get associated state of the element
	 * 
	 * @return
	 * 			associated state of the element, or <tt>null</tt> if this element represents 
	 * 			a terminal element which will be created by scanning a matched token
	 */
	@Nullable
	public State getState() {
		return state;
	}
	
	/**
	 * Get tokens matched by this element so far.
	 * 
	 * @return
	 * 			tokens matched by this element so far, or empty if this element does not 
	 * 			match any tokens yet
	 */
	public List<Token> getMatchedTokens() {
		if (state != null)
			return parser.getTokens().subList(state.getPrevTokenIndex(), nextTokenIndex);
		else if (nextTokenIndex > 0)
			return parser.getTokens().subList(nextTokenIndex-1, nextTokenIndex);
		else
			return new ArrayList<>();
	}
	
	/**
	 * Get the first token matched by this element. 
	 * 
	 * @return
	 * 			first token matched by this element, or <tt>null</tt> if this element 
	 * 			does not match any tokens yet
	 */
	@Nullable
	public Token getFirstMatchedToken() {
		if (state != null) {
			if (nextTokenIndex > state.getPrevTokenIndex())
				return parser.getTokens().get(state.getPrevTokenIndex());
			else
				return null;
		} else if (nextTokenIndex > 0) {
			return parser.getTokens().get(nextTokenIndex-1);
		} else {
			return null;
		}
	}
	
	/**
	 * Get the last token matched by this element. 
	 * 
	 * @return
	 * 			last token matched by this element, or <tt>null</tt> if this element does not 
	 * 			match any tokens yet
	 */
	@Nullable
	public Token getLastMatchedToken() {
		if (state != null) {
			if (nextTokenIndex > state.getPrevTokenIndex())
				return parser.getTokens().get(nextTokenIndex-1);
			else
				return null;
		} else if (nextTokenIndex > 0) {
			return parser.getTokens().get(nextTokenIndex-1);
		} else {
			return null;
		}
	}
	
	public String getMatchedText() {
		StringBuilder builder = new StringBuilder();
		for (Token token: getMatchedTokens())
			builder.append(token.getText());
		return builder.toString();
	}
	
	public EarleyParser getParser() {
		return parser;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("(");
		if (spec != null)
			builder.append("spec: " + spec + ", ");
		if (state != null)
			builder.append("state: " + state + ", ");
		builder.append("endTokenIndex: " + nextTokenIndex + ")");
		return builder.toString();
	}
	
}
