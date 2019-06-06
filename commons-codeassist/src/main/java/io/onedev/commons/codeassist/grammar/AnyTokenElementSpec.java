package io.onedev.commons.codeassist.grammar;

import io.onedev.commons.codeassist.LiteralScan;

public class AnyTokenElementSpec extends TerminalElementSpec {

	private static final long serialVersionUID = 1L;

	public AnyTokenElementSpec(String label, Multiplicity multiplicity) {
		super(label, multiplicity);
	}

	@Override
	protected String toStringOnce() {
		return ".";
	}

	@Override
	public LiteralScan scanMandatories() {
		return LiteralScan.stop();
	}
	
	@Override
	public boolean isToken(int tokenType) {
		return true;
	}

	@Override
	public LiteralScan scanPrefixes() {
		return LiteralScan.stop();
	}
	
	@Override
	protected boolean isAllowEmptyOnce() {
		return false;
	}

}
