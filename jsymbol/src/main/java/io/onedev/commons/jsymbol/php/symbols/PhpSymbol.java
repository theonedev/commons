package io.onedev.commons.jsymbol.php.symbols;

import javax.annotation.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.jsymbol.TokenPosition;

public abstract class PhpSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final PhpSymbol parent;

	private final String name;
	
	private final TokenPosition position;
	
	private TokenPosition scope;
	
	public PhpSymbol(PhpSymbol parent, String name, @Nullable TokenPosition position, @Nullable TokenPosition scope) {
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
	public PhpSymbol getParent() {
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

	public void setScope(TokenPosition scope) {
		this.scope = scope;
	}
	
	@Override
	public String getFQNSeparator() {
		return "\\";
	}

	@Override
	protected String getFQNLeading() {
		return "\\";
	}
	
}
