package io.onedev.commons.codeassist.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.antlr.v4.runtime.Token;

import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.LiteralScan;
import io.onedev.commons.codeassist.grammar.ElementSpec;
import io.onedev.commons.codeassist.grammar.Grammar;
import io.onedev.commons.codeassist.grammar.LexerRuleRefElementSpec;
import io.onedev.commons.codeassist.grammar.RuleRefElementSpec;
import io.onedev.commons.codeassist.grammar.RuleSpec;
import io.onedev.commons.codeassist.grammar.TerminalElementSpec;

/**
 * An Earley parser (https://en.wikipedia.org/wiki/Earley_parser) to parse user inputs.
 * It is very suited for code assistance purpose as it can provide partial 
 * parse results and tell us what terminals are expected next. Note that for large 
 * inputs (thousands of lines for example), our earley implementation performs bad 
 * and consumes a lot of memory, but it serves our purpose to provide a mini code 
 * assistance for web input 
 * 
 * @author robin
 *
 */
public class EarleyParser {

	private final Grammar grammar;
	
	private final RuleSpec rule;
	
	private final String text;
	
	private final List<Token> tokens;
	
	private int tokenIndex = 0;
	
	private final List<Chart> charts = new ArrayList<>();

	public EarleyParser(Grammar grammar, String ruleName, String text) {
		this.grammar = grammar;
		rule = grammar.getRule(ruleName);
		this.text = text;
		tokens = grammar.lex(text);
		
		Set<State> states = new LinkedHashSet<>();
		for (int i=0; i<rule.getAlternatives().size(); i++) 
			states.add(new State(tokenIndex, rule, i, 0, false, new ArrayList<Element>()));

		while (!states.isEmpty()) {
			Chart chart = new Chart(this, tokenIndex, states);
			charts.add(chart);
			for (State state: Lists.newArrayList(chart.getStates())) // avoid concurrent modification
				process(state, chart);

			if (tokenIndex == tokens.size())
				break;
			
			states = new LinkedHashSet<>();
			for (State state: chart.getStates()) 
				scan(state, states);
			tokenIndex++;
		}
	}
	
	private void process(State state, Chart chart) {
		if (!state.isCompleted()) { // predict
			ElementSpec expectedElementSpec = state.getExpectedElementSpec();
			if (expectedElementSpec instanceof RuleRefElementSpec) {
				RuleRefElementSpec ruleRefElement = (RuleRefElementSpec) expectedElementSpec;
				RuleSpec elementRule = ruleRefElement.getRule();
				for (int i=0; i<elementRule.getAlternatives().size(); i++) {
					State predictedState = new State(tokenIndex, 
							elementRule, i, 0, false, new ArrayList<Element>());
					if (chart.getStates().add(predictedState))
						process(predictedState, chart);
				}
			}
			if (expectedElementSpec.isOptional() || state.isExpectedElementSpecMatchedOnce()) {
				State advancedState = new State(state.getPrevTokenIndex(), state.getRuleSpec(), 
						state.getAlternativeSpecIndex(), state.getExpectedElementSpecIndex()+1, false, 
						new ArrayList<>(state.getMatchedElements()));
				if (chart.getStates().add(advancedState))
					process(advancedState, chart);
			}
		} else { // complete
			Chart startChart = charts.get(state.getPrevTokenIndex());
			Collection<State> startStates;
			if (state.getPrevTokenIndex() == chart.getNextTokenIndex())
				startStates = Lists.newArrayList(startChart.getStates()); // avoid concurrent modification
			else
				startStates = startChart.getStates();
			for (State startState: startStates) {
				if (!startState.isCompleted()) {
					ElementSpec expectedElementSpec = startState.getExpectedElementSpec();
					if (expectedElementSpec instanceof RuleRefElementSpec) {
						RuleRefElementSpec ruleRefElement = (RuleRefElementSpec) expectedElementSpec;
						if (ruleRefElement.getRuleName().equals(state.getRuleSpec().getName())) {
							State advancedState;
							List<Element> elements = new ArrayList<>(startState.getMatchedElements());
							if (!state.getMatchedElements().isEmpty())
								elements.add(new Element(this, ruleRefElement, chart.getNextTokenIndex(), state));
							if (!expectedElementSpec.isMultiple()) {
								advancedState = new State(startState.getPrevTokenIndex(), 
										startState.getRuleSpec(), startState.getAlternativeSpecIndex(), 
										startState.getExpectedElementSpecIndex()+1, false, elements);
							} else {
								advancedState = new State(startState.getPrevTokenIndex(), 
										startState.getRuleSpec(), startState.getAlternativeSpecIndex(), 
										startState.getExpectedElementSpecIndex(), true, elements);
							}
							if (chart.getStates().add(advancedState))
								process(advancedState, chart);
						}
					}
				}
			}
		}
	}
	
	private void scan(State state, Set<State> nextStates) {
		if (!state.isCompleted()) {
			ElementSpec expectedElementSpec = state.getExpectedElementSpec();
			int tokenType = tokens.get(tokenIndex).getType();
			if ((expectedElementSpec instanceof TerminalElementSpec) 
					&& ((TerminalElementSpec)expectedElementSpec).isToken(tokenType)) {
				State scannedState;
				List<Element> elements = new ArrayList<>(state.getMatchedElements());
				elements.add(new Element(this, expectedElementSpec, tokenIndex+1, null));
				if (!expectedElementSpec.isMultiple()) {
					scannedState = new State(state.getPrevTokenIndex(), state.getRuleSpec(), 
							state.getAlternativeSpecIndex(), state.getExpectedElementSpecIndex()+1, 
							false, elements);
				} else {
					scannedState = new State(state.getPrevTokenIndex(), state.getRuleSpec(), 
							state.getAlternativeSpecIndex(), state.getExpectedElementSpecIndex(), 
							true, elements);
				}
				nextStates.add(scannedState);
			}
		}
	}
	
	public List<Chart> getCharts() {
		return charts;
	}
	
	public RuleSpec getRule() {
		return rule;
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public String getText() {
		return text;
	}

	public List<State> getMatches() {
		if (charts.size() == tokens.size()+1) 
			return charts.get(tokens.size()).getMatches();
		else
			return new ArrayList<>();
	}
	
	public boolean matches() {
		return !getMatches().isEmpty();
	}
	
	@Nullable
	public Token getLastMatchedToken() {
		int endOfMatch = getEndOfMatch();
		if (endOfMatch > 0)
			return tokens.get(endOfMatch-1);
		else
			return null;
	}
	
	public List<Token> getMatchedTokens() {
		int endOfMatch = getEndOfMatch();
		if (endOfMatch > 0)
			return tokens.subList(0, endOfMatch);
		else
			return new ArrayList<>();
	}
	
	/**
	 * Get the next token index after the match. 
	 * 
	 * @return
	 * 			index of next token after the match, or 0 if the rule does not match any tokens
	 */
	public int getEndOfMatch() {
		for (int i=charts.size()-1; i>=0; i--) {
			Chart state = charts.get(i);
			if (!state.getMatches().isEmpty())
				return i;
		}
		return 0;
	}
	
	/**
	 * @param states
	 * 			accumulated states from down to top
	 * @param terminalExpects
	 * 			list of accumulated terminal expects
	 * @param findAllPaths
	 * 			whether or not to find all possible paths to specified states. Turning off this 
	 * 			option can dramatically reduce the number of terminal expects, but may miss 
	 * 			some suggestions. For instance, considering below rule:
	 * 			
	 * 			<p>
	 * 			column: (tableName=name '.')? columnName=name;
	 * 			name: Id;
	 * 			Id: [a-z]+;
	 * 
	 * 			<p>For an empty input, there will a single terminal state expecting the 'Id' rule. However 
	 * 			depending on the parsing path going from 'column' rule to 'name' rule, the 'Id' rule can 
	 * 			represent a table name or a column name If 'findAllPaths' is set to 'true', both paths can 
	 * 			be constructed; otherwise, only the first path can be constructed
	 * 
	 * 			There is another side effect. Consider below rule:
	 * 
	 * 			<p>query: (criteria WS+ order WS* EOF) | (criteria WS* EOF); 
	 *   
	 * 			<p>For an empty input, if 'findAllPaths' is set to 'false', only the first path to criteria 
	 * 			is returned, and then a space will be appended after criteria as it is mandatory in this 
	 * 			path. To avoid this situation, the rule can be re-written as below:
	 * 
	 * 			<p>query: criteria WS* (WS order WS*)? EOF;
	 * 
	 * @return
	 * 		
	 */
	private void fillTerminalExpects(List<State> states, List<TerminalExpect> terminalExpects, boolean findAllPaths) {
		State state = states.get(states.size()-1);
		if (state.getRuleSpec().getName().equals(getRule().getName()) && state.getPrevTokenIndex() == 0) {
			ParseExpect parseExpect = new ParseExpect(null, states.get(states.size()-1), this);
			for (int i=states.size()-2; i>=0; i--) 
				parseExpect = new ParseExpect(parseExpect, states.get(i), this);
			terminalExpects.add(new TerminalExpect(parseExpect.getParent(), parseExpect.getState(), this));
		} else {
			Chart startChart = getCharts().get(state.getPrevTokenIndex());
			for (State startState: startChart.getStates()) {
				if (!startState.isCompleted()) {
					ElementSpec nextElement = startState.getExpectedElementSpec();
					if (nextElement instanceof RuleRefElementSpec) {
						RuleRefElementSpec ruleRefElement = (RuleRefElementSpec) nextElement;
						if (ruleRefElement.getRuleName().equals(state.getRuleSpec().getName()) && !states.contains(startState)) {
							List<State> newStates = new ArrayList<>(states);
							newStates.add(startState);
							int prevSize = terminalExpects.size();
							fillTerminalExpects(newStates, terminalExpects, findAllPaths);
							if (terminalExpects.size() > prevSize && !findAllPaths)
								return;
						}
					}
				}
			}
		}
	}
	
	public List<TerminalExpect> buildParseExpects(boolean findAllPaths) {
		List<TerminalExpect> terminalExpects = new ArrayList<>();

		/*
		 * Find all terminal expects from:
		 * 1. States of last chart
		 * 2. States of another chart able to provide non-space terminal specs. 
		 * 
		 * The second chart is used to handle below case:
		 * 
		 * Assume we have below grammar:
		 * 
		 * query: 'approved' | 'approved' WS+ 'by' WS+ 'me';
		 * WS: ' ';
		 * 
		 * When we input 'approved   by      ', we should be able to suggest 'approved by me'
		 */
		for (int i=charts.size()-1; i>=0; i--) {
			boolean stop = false;
			for (State state: charts.get(i).getStatesExpectingTerminal()) {
				if (i == charts.size()-1) {
					fillTerminalExpects(Lists.newArrayList(state), terminalExpects, findAllPaths);
				} else {
					LiteralScan scan = state.getExpectedElementSpec().scanMandatories();
					if (!scan.isSpaceSeparator()) {
						fillTerminalExpects(Lists.newArrayList(state), terminalExpects, findAllPaths);
						stop = true;
					}
				}
			}
			if (stop)
				break;
		}
		
		Map<TerminalExpect, Integer> orders = new HashMap<>();
		for (TerminalExpect terminalExpect: terminalExpects) {
			ElementSpec spec = terminalExpect.getElementSpec();
			String elementRuleName;
			if (spec instanceof RuleRefElementSpec) 
				elementRuleName = ((RuleRefElementSpec) spec).getRuleName();
			else if (spec instanceof LexerRuleRefElementSpec)
				elementRuleName = ((LexerRuleRefElementSpec) spec).getRuleName();
			else
				elementRuleName = null;
			orders.put(terminalExpect, grammar.getRuleIndex(elementRuleName));
		}

		Collections.sort(terminalExpects, new Comparator<TerminalExpect>() {

			@Override
			public int compare(TerminalExpect o1, TerminalExpect o2) {
				int result = orders.get(o1) - orders.get(o2);
				if (result == 0 && o1.getState().getRuleSpec() == o2.getState().getRuleSpec()) { 
					result = o1.getState().getAlternativeSpecIndex() - o2.getState().getAlternativeSpecIndex();
					if (result == 0)
						result = o1.getState().getExpectedElementSpecIndex() - o2.getState().getExpectedElementSpecIndex();
					if (result == 0)
						result = o1.getState().getPrevTokenIndex() - o2.getState().getPrevTokenIndex();
				}
				return result;
			}
			
		});
		
		return terminalExpects;
	}
	
}
