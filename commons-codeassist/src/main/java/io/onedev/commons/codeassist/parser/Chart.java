package io.onedev.commons.codeassist.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.antlr.v4.runtime.Token;

import io.onedev.commons.codeassist.grammar.ElementSpec;
import io.onedev.commons.codeassist.grammar.TerminalElementSpec;

/**
 * Represent a chart (or state set) as mentioned in https://en.wikipedia.org/wiki/Earley_parser
 * 
 * @author robin
 *
 */
public class Chart {
	
	private final EarleyParser parser;
	
	private final int nextTokenIndex;
	
	private final Set<State> states;
	
	/**
	 * Construct a Earley chart. 
	 * 
	 * @param parser
	 * 			current Earley parser instance
	 * @param position
	 * 			position of the chart, representing index of next token to be matched for this chart
	 * @param states
	 * 			states in this chart
	 */
	public Chart(EarleyParser parser, int nextTokenIndex, Set<State> states) {
		this.parser = parser;
		this.nextTokenIndex = nextTokenIndex;
		this.states = states;
	}
	
	public EarleyParser getParser() {
		return parser;
	}

	/**
	 * Get position of this chart
	 * 
	 * @return
	 * 			position of the chart, representing index of next token to be matched for this chart
	 */
	public int getNextTokenIndex() {
		return nextTokenIndex;
	}
	
	/**
	 * Get the token being scanned to create this chart.
	 * 
	 * @return
	 * 			the token being scanned to create this chart, or <tt>null</tt> for initial chart
	 */
	@Nullable
	public Token getLastToken() {
		if (nextTokenIndex > 0)
			return parser.getTokens().get(nextTokenIndex-1);
		else
			return null;
	}

	/**
	 * Get set of states in this chart
	 * 
	 * @return
	 * 			set of states in this chart
	 */
	public Set<State> getStates() {
		return states;
	}
	
	/**
	 * Get matched states in this chart.
	 * 
	 * @return
	 * 			matched states in this chart
	 */
	public List<State> getMatches() {
		List<State> matches = new ArrayList<>();
		for (State state: states) {
			if (state.getRuleSpec().getName().equals(parser.getRule().getName()) 
					&& state.getPrevTokenIndex() == 0 && state.isCompleted()) {
				matches.add(state);
			}
		}
		return matches;
	}
	
	public List<State> getStatesExpectingTerminal() {
		List<State> statesExpectingTerminal = new ArrayList<>();
		for (State state: states) {
			ElementSpec expectingSpec = state.getExpectedElementSpec();
			if (expectingSpec instanceof TerminalElementSpec)
				statesExpectingTerminal.add(state);
		}
		return statesExpectingTerminal;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (State state: states)
			buffer.append(state).append("\n");
		return buffer.toString();
	}
		
}
