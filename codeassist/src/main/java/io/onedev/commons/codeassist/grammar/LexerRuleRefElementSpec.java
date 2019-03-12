package io.onedev.commons.codeassist.grammar;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import io.onedev.commons.codeassist.LiteralScan;

public class LexerRuleRefElementSpec extends TokenElementSpec {

	private static final long serialVersionUID = 1L;

	private final Grammar grammar;
	
	private final String ruleName;
	
	private transient Optional<RuleSpec> rule;
	
	public LexerRuleRefElementSpec(Grammar grammar, String label, Multiplicity multiplicity, 
			int tokenType, String ruleName) {
		super(label, multiplicity, tokenType);
		
		this.grammar = grammar;
		this.ruleName = ruleName;
	}

	public String getRuleName() {
		return ruleName;
	}
	
	public RuleSpec getRule() {
		if (rule == null)
			rule = Optional.fromNullable(grammar.getRule(ruleName));
		return rule.orNull();
	}

	@Override
	public LiteralScan scanMandatories() {
		if (getRule() != null) 
			return getRule().scanMandatories().merge();
		else 
			return LiteralScan.stop();
	}
	
	@Override
	public LiteralScan scanPrefixes() {
		if (getRule() != null) 
			return getRule().scanPrefixes().copy();
		else 
			return LiteralScan.stop();
	}
	
	@Override
	protected String toStringOnce() {
		if (grammar.isBlockRule(ruleName))
			return "(" + Preconditions.checkNotNull(getRule()) + ")";
		else 
			return ruleName;
	}

	@Override
	protected boolean isAllowEmptyOnce() {
		if (getRule() != null)
			return getRule().isAllowEmpty();
		else
			return false;
	}

}
