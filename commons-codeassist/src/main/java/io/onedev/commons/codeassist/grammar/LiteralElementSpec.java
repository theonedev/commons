package io.onedev.commons.codeassist.grammar;

import com.google.common.collect.Lists;

import io.onedev.commons.codeassist.LiteralScan;

public class LiteralElementSpec extends TokenElementSpec {

	private static final long serialVersionUID = 1L;
	
	private final String literal;
	
	public LiteralElementSpec(String label, Multiplicity multiplicity, 
			int tokenType, String literal) {
		super(label, multiplicity, tokenType);
		
		this.literal = literal;
	}

	@Override
	public LiteralScan scanMandatories() {
		return new LiteralScan(Lists.newArrayList(literal), false);
	}
	
	@Override
	protected String toStringOnce() {
		return "'" + literal + "'";
	}

	@Override
	public LiteralScan scanPrefixes() {
		return new LiteralScan(Lists.newArrayList(literal), false);
	}
	
	@Override
	protected boolean isAllowEmptyOnce() {
		return false;
	}
	
}
