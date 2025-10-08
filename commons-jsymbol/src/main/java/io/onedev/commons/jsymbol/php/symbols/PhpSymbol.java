package io.onedev.commons.jsymbol.php.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class PhpSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final PhpSymbol parent;

	private final String name;
	
	private final PlanarRange position;
	
	private PlanarRange scope;
	
	public PhpSymbol(PhpSymbol parent, String name, @Nullable PlanarRange position, @Nullable PlanarRange scope) {
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
	public PlanarRange getPosition() {
		return position;
	}
 
	@Override
	public PlanarRange getScope() {
		return scope;
	}

	public void setScope(PlanarRange scope) {
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
