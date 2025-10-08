package io.onedev.commons.jsymbol.go.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class GolangSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private GolangSymbol parent;
	
	private final String name;
	
	private final PlanarRange position;
	
	private final PlanarRange scope;	
	
	public GolangSymbol(GolangSymbol parent, String name, @Nullable PlanarRange position, @Nullable PlanarRange scope) {
		this.parent = parent;
		this.position = position;
		this.scope = scope;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public GolangSymbol getParent() {
		return parent;
	}

	public void setParent(GolangSymbol parent) {
		this.parent = parent;
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

	@Override
	public boolean isPrimary() {
		return getParent() == null || getParent() instanceof PackageSymbol;
	}

}
