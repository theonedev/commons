package io.onedev.commons.jsymbol.rust.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class RustSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final RustSymbol parent;
	
	private final String name;
	
	private final PlanarRange position;
	
	private final PlanarRange scope;
	
	private final boolean local;
	
	public RustSymbol(@Nullable RustSymbol parent, String name, PlanarRange position, @Nullable PlanarRange scope,
			boolean local) {
		this.parent = parent;
		this.name = name;
		this.position = position;
		this.scope = scope;
		this.local = local;
	}

	@Override
	public RustSymbol getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
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
	public boolean isLocal() {
		return local;
	}

	@Override
	public String getFQNSeparator() {
		return "::";
	}
	
}
