package io.onedev.commons.jsymbol.go.symbols;

import javax.annotation.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.jsymbol.TokenPosition;

public abstract class GolangSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private GolangSymbol parent;
	
	private final String name;
	
	private final TokenPosition position;
	
	private final TokenPosition scope;	
	
	public GolangSymbol(GolangSymbol parent, String name, @Nullable TokenPosition position, @Nullable TokenPosition scope) {
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
	public TokenPosition getPosition() {
		return position;
	}

	@Override
	public TokenPosition getScope() {
		return scope;
	}

	@Override
	public String getFQNSeparator() {
		return ".";
	}
	
}
