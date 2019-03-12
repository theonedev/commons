package io.onedev.commons.jsymbol.java.symbols;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.jsymbol.TokenPosition;

public abstract class JavaSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final String name;
	
	private final JavaSymbol parent;
	
	private final TokenPosition position;
	
	private final TokenPosition scope;
	
	public JavaSymbol(JavaSymbol parent, String name, TokenPosition position, TokenPosition scope) {
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
