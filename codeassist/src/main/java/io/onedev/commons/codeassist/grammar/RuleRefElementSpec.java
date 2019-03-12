package io.onedev.commons.codeassist.grammar;

import com.google.common.base.Preconditions;

import io.onedev.commons.codeassist.LiteralScan;

public class RuleRefElementSpec extends ElementSpec {

	private static final long serialVersionUID = 1L;

	private final Grammar grammar;
	
	private final String ruleName;
	
	private transient RuleSpec rule;
	
	public RuleRefElementSpec(Grammar grammar, String label, Multiplicity multiplicity, String ruleName) {
		super(label, multiplicity);
	
		this.grammar = grammar;
		this.ruleName = ruleName;
	}

	public RuleSpec getRule() {
		if (rule == null)
			rule = Preconditions.checkNotNull(grammar.getRule(ruleName));
		return rule;
	}
	
	public String getRuleName() {
		return ruleName;
	}

	@Override
	public LiteralScan scanMandatories() {
		return getRule().scanMandatories().copy();
	}
	
	@Override
	public LiteralScan scanPrefixes() {
		return getRule().scanPrefixes().copy();
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
		return getRule().isAllowEmpty();
	}

}
