package io.onedev.commons.jsymbol.swift.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class SwiftSymbol extends Symbol {

	private static final long serialVersionUID = 1L;

	private final SwiftSymbol parent;

	private final String name;

	private final PlanarRange position;

	private final PlanarRange scope;

	private final boolean local;

	public SwiftSymbol(@Nullable SwiftSymbol parent, String name, PlanarRange position,
			@Nullable PlanarRange scope, boolean local) {
		this.parent = parent;
		this.name = name;
		this.position = position;
		this.scope = scope;
		this.local = local;
	}

	@Override
	public SwiftSymbol getParent() {
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
		return ".";
	}

}
