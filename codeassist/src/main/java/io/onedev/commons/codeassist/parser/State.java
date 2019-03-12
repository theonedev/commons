package io.onedev.commons.codeassist.parser;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import io.onedev.commons.codeassist.grammar.AlternativeSpec;
import io.onedev.commons.codeassist.grammar.ElementSpec;
import io.onedev.commons.codeassist.grammar.RuleSpec;

/**
 * Represents a Earley state as mentioned in https://en.wikipedia.org/wiki/Earley_parser
 * 
 * @author robin
 *
 */
public class State {
	
	private final int prevTokenIndex;
	
	private final RuleSpec ruleSpec;
	
	private final int alternativeSpecIndex;
	
	private final int expectedElementSpecIndex;
	
	private final boolean expectedElementSpecMatchedOnce;
	
	private final List<Element> matchedElements;
	
	private transient AlternativeSpec alternativeSpec;
	
	private transient List<ElementSpec> elementSpecs;

	/**
	 * Construct a Earley state.
	 * 
	 * @param prevTokenIndex
	 *			position of the chart originating this state. Representing index of first token matched by the state 
	 * @param ruleSpec
	 * 			rule spec of the state
	 * @param alternativeSpecIndex
	 * 			index of alternative spec above rule 
	 * @param expectedElementSpecIndex
	 * 			index of element spec expecting to be matched, representing dot position 
	 * 			of the state
	 * @param expectedElementSpecMatchedOnce
	 * 			whether or not the expected element spec has been matched at least once to
	 * 			facilitate handling of element multiplicity (* and +)
	 * @param matchedElements
	 * 			already matched elements in this state
	 */
	public State(int prevTokenIndex, RuleSpec ruleSpec, int alternativeSpecIndex, int expectedElementSpecIndex, 
			boolean expectedElementSpecMatchedOnce, List<Element> matchedElements) {
		this.prevTokenIndex = prevTokenIndex;
		this.ruleSpec = ruleSpec;
		this.alternativeSpecIndex = alternativeSpecIndex;
		this.expectedElementSpecIndex = expectedElementSpecIndex;
		this.expectedElementSpecMatchedOnce = expectedElementSpecMatchedOnce;
		this.matchedElements = matchedElements;
	}
	
	/**
	 * Get origin position of this state
	 * 
	 * @return
	 * 			position of the chart originating this state. Representing index of first token matched by the state
	 */
	public int getPrevTokenIndex() {
		return prevTokenIndex;
	}

	public RuleSpec getRuleSpec() {
		return ruleSpec;
	}

	public int getAlternativeSpecIndex() {
		return alternativeSpecIndex;
	}

	public int getExpectedElementSpecIndex() {
		return expectedElementSpecIndex;
	}

	public boolean isExpectedElementSpecMatchedOnce() {
		return expectedElementSpecMatchedOnce;
	}
	
	/**
	 * Get expected element spec
	 * 
	 * @return
	 * 			element spec expected to be matched next, or <tt>null</tt> if 
	 * 			the state is completed
	 */
	@Nullable
	public ElementSpec getExpectedElementSpec() {
		if (isCompleted())
			return null;
		else
			return getElementSpecs().get(expectedElementSpecIndex);
	}

	/**
	 * Get elements already matched in this state
	 * 
	 * @return
	 * 			elements already matched in this state
	 */
	public List<Element> getMatchedElements() {
		return matchedElements;
	}
	
	public AlternativeSpec getAlternativeSpec() {
		if (alternativeSpec == null)
			alternativeSpec = ruleSpec.getAlternatives().get(alternativeSpecIndex);
		return alternativeSpec;
	}
	
	public List<ElementSpec> getElementSpecs() {
		if (elementSpecs == null)
			elementSpecs = getAlternativeSpec().getElements();
		return elementSpecs;
	}
	
	public boolean isCompleted() {
		return expectedElementSpecIndex == getElementSpecs().size();
	}

	public State getScannedState(EarleyParser parser, int tokenIndex) {
		ElementSpec expectedElementSpec = getExpectedElementSpec();
		List<Element> elements = new ArrayList<>(getMatchedElements());
		elements.add(new Element(parser, expectedElementSpec, tokenIndex+1, null));
		if (!expectedElementSpec.isMultiple()) {
			return new State(getPrevTokenIndex(), getRuleSpec(), 
					getAlternativeSpecIndex(), getExpectedElementSpecIndex()+1, 
					false, elements);
		} else {
			return new State(getPrevTokenIndex(), getRuleSpec(), 
					getAlternativeSpecIndex(), getExpectedElementSpecIndex(), 
					true, elements);
		}
	}
	
	/**
	 * Find children by label defined in grammar
	 * 
	 * @param label
	 * 			element label defined in grammar 
	 * @param recursive
	 * 			whether or not search recursively
	 * @return
	 * 			list of children with specified label
	 */
	public List<Element> findMatchedElementsByLabel(String label, boolean recursive) {
		List<Element> children = new ArrayList<>();
		for (Element child: getMatchedElements()) {
			if (label.equals(child.getLabel()))
				children.add(child);
			if (recursive && child.getState() != null)
				children.addAll(child.getState().findMatchedElementsByLabel(label, recursive));
		}
		return children;
	}
	
	/**
	 * Find children by rule name
	 * 
	 * @param ruleName
	 * 			name of the rule
	 * @param recursive
	 * 			whether or not search recursively
	 * @return
	 * 			list of children with specified rule name
	 */
	public List<Element> findMatchedElementsByRule(String ruleName, boolean recursive) {
		List<Element> children = new ArrayList<>();
		for (Element child: getMatchedElements()) {
			if (child.getState() != null && ruleName.equals(child.getState().getRuleSpec().getName()))
				children.add(child);
			if (recursive && child.getState() != null)
				children.addAll(child.getState().findMatchedElementsByRule(ruleName, recursive));
		}
		return children;
	}
	
	@Nullable
	public Element getFirstMatchedElement() {
		if (!matchedElements.isEmpty())
			return matchedElements.get(0);
		else
			return null;
	}
	
	@Nullable
	public Element getLastMatchedElement() {
		if (!matchedElements.isEmpty())
			return matchedElements.get(matchedElements.size()-1);
		else
			return null;
	}
	
	@Nullable
	public Token getLastMatchedToken() {
		if (getLastMatchedElement() != null)
			return getLastMatchedElement().getLastMatchedToken();
		else
			return null;
	}
	
	@Nullable
	public Token getFirstMatchedToken() {
		if (getFirstMatchedElement() != null)
			return getFirstMatchedElement().getFirstMatchedToken();
		else
			return null;
	}
	
	public List<Token> getMatchedTokens() {
		List<Token> matchedTokens = new ArrayList<>();
		for (Element element: matchedElements)
			matchedTokens.addAll(element.getMatchedTokens());
		return matchedTokens;
	}
	
	public String getMatchedText() {
		StringBuilder builder = new StringBuilder();
		for (Element element: matchedElements)
			builder.append(element.getMatchedText());
		return builder.toString();
	}
	
	public int getNextTokenIndex() {
		if (getLastMatchedElement() != null) 
			return getLastMatchedElement().getNextTokenIndex();
		else 
			return getPrevTokenIndex();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof State))
			return false;
		if (this == other)
			return true;
		State otherState = (State) other;
		
		/*
		 * The standard Earley parser should also consider parsed elements into 
		 * account when compare state, however it will cause state to increase 
		 * quickly in case of ambiguity rules. By excluding parsed elements from 
		 * comparison, we normally get only the first possibility amongst all 
		 * the ambiguity possibilities, which is totally acceptable for our 
		 * code assistance purpose  
		 */
		return new EqualsBuilder()
				.append(prevTokenIndex, otherState.prevTokenIndex)
				.append(ruleSpec.getName(), otherState.ruleSpec.getName())
				.append(alternativeSpecIndex, otherState.alternativeSpecIndex)
				.append(expectedElementSpecIndex, otherState.expectedElementSpecIndex)
				.append(expectedElementSpecMatchedOnce, otherState.expectedElementSpecMatchedOnce)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(prevTokenIndex)
				.append(ruleSpec.getName())
				.append(alternativeSpecIndex)
				.append(expectedElementSpecIndex)
				.append(expectedElementSpecMatchedOnce)
				.toHashCode();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i=0; i<expectedElementSpecIndex; i++)
			buffer.append(getElementSpecs().get(i)).append(" ");
		buffer.append(expectedElementSpecMatchedOnce?"~ ":"^ ");
		for (int i=expectedElementSpecIndex; i<getElementSpecs().size(); i++)
			buffer.append(getElementSpecs().get(i)).append(" ");
		
		return ruleSpec.getName() + " -> " + buffer.toString() + ": " + prevTokenIndex;
	}
	
}
