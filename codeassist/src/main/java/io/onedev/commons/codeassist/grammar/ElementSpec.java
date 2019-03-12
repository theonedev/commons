package io.onedev.commons.codeassist.grammar;

import java.io.Serializable;

import io.onedev.commons.codeassist.LiteralScan;

public abstract class ElementSpec implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public enum Multiplicity{ONE, ZERO_OR_ONE, ZERO_OR_MORE, ONE_OR_MORE};
	
	private final String label;
	
	private final Multiplicity multiplicity;
	
	public ElementSpec(String label, Multiplicity multiplicity) {
		this.label = label;
		this.multiplicity = multiplicity;
	}

	public String getLabel() {
		return label;
	}

	public Multiplicity getMultiplicity() {
		return multiplicity;
	}
	
	public boolean isOptional() {
		return multiplicity == Multiplicity.ZERO_OR_MORE || multiplicity == Multiplicity.ZERO_OR_ONE;
	}

	public boolean isMultiple() {
		return multiplicity == Multiplicity.ONE_OR_MORE || multiplicity == Multiplicity.ZERO_OR_MORE;
	}
	
	public abstract LiteralScan scanPrefixes();
	
	protected abstract boolean isAllowEmptyOnce();
	
	public boolean isAllowEmpty() {
		if (isOptional())
			return true;
		else
			return isAllowEmptyOnce();
	}
	
	public abstract LiteralScan scanMandatories();
	
	public final String toString() {
		if (multiplicity == Multiplicity.ONE)
			return toStringOnce();
		else if (multiplicity == Multiplicity.ONE_OR_MORE)
			return toStringOnce() + "+";
		else if (multiplicity == Multiplicity.ZERO_OR_MORE)
			return toStringOnce() + "*";
		else
			return toStringOnce() + "?";
	}
	
	protected abstract String toStringOnce();
	
}
