package io.onedev.commons.codeassist.parser;

import org.jspecify.annotations.Nullable;

import org.antlr.v4.runtime.Token;

import io.onedev.commons.codeassist.grammar.ElementSpec;
import io.onedev.commons.utils.StringUtils;

public class ParseExpect {

	private final EarleyParser parser;
	
	private final ParseExpect parent;
	
	private final State state;
	
	public ParseExpect(@Nullable ParseExpect parent, State state, EarleyParser parser) {
		this.parent = parent;
		this.state = state;
		this.parser = parser;
	}

	/**
	 * Get parent element
	 * 
	 * @return
	 * 			parent element, or <tt>null</tt> if current element is root
	 */
	@Nullable
	public ParseExpect getParent() {
		return parent;
	}

	public State getState() {
		return state;
	}
	
	public EarleyParser getParser() {
		return parser;
	}

	public ElementSpec getElementSpec() {
		return state.getExpectedElementSpec();
	}
	
	@Nullable
	public ParseExpect findExpectByLabel(String label) {
		ParseExpect current = this;
		while (true) {
			if (label.equals(current.getElementSpec().getLabel()))
				return current;
			current = current.parent;
			if (current == null)
				return null;
		}
	}

	@Nullable
	public ParseExpect findExpectByRule(String ruleName) {
		ParseExpect current = this;
		while (true) {
			if (ruleName.equals(current.getState().getRuleSpec().getName()))
				return current;
			current = current.parent;
			if (current == null)
				return null;
		}
	}
	
	/**
	 * Get root element
	 * 
	 * @return
	 * 			root element
	 */
	public ParseExpect getRoot() {
		if (parent == null)
			return this;
		else
			return parent.getRoot();
	}

	@Override
	public String toString() {
		if (parent != null)
			return parent.toString() + "\n" + state.toString();
		else
			return state.toString();
	}
	
	public String getUnmatchedText() {
		int nextTokenIndex = state.getNextTokenIndex();

		int endCharIndex;
		if (nextTokenIndex != 0) 
			endCharIndex = parser.getTokens().get(nextTokenIndex-1).getStopIndex()+1;
		else 
			endCharIndex = 0;
		
		String unmatchedText = parser.getText().substring(endCharIndex);
		for (Token each: parser.getTokens()) {
			if (each.getStartIndex() == endCharIndex) {
				// we can not trim leading space as it is not optional
				return unmatchedText;
			}
		}
		return StringUtils.stripStart(unmatchedText, " ");
	}
	
}
