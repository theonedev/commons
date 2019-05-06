package io.onedev.commons.jsymbol.java.symbols;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class JavaSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final String name;
	
	private final JavaSymbol parent;
	
	private final PlanarRange position;
	
	private final PlanarRange scope;
	
	public JavaSymbol(JavaSymbol parent, String name, PlanarRange position, PlanarRange scope) {
		this.parent = parent;
		this.name = name;
		this.position = position;
		this.scope = scope;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JavaSymbol getParent() {
		return parent;
	}

	@Override
	public PlanarRange getPosition() {
		return position;
	}

	@Override
	public PlanarRange getScope() {
		return scope;
	}

	@Override
	public String getFQNSeparator() {
		return ".";
	}

}
