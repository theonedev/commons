package io.onedev.commons.jsymbol.r.symbols;

import javax.annotation.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.jsymbol.TokenPosition;

/**
 * @author robin
 *
 */
public abstract class RSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final RSymbol parent;
	
	private final String name;
	
	private final TokenPosition position;
	
	private final TokenPosition scope;
	
	public RSymbol(RSymbol parent, String name, @Nullable TokenPosition position, 
			@Nullable TokenPosition scope) {
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
	public RSymbol getParent() {
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
		return ":";
	}
	
}
