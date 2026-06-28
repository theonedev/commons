package io.onedev.commons.jsymbol.typescript.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class TypeScriptSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final TypeScriptSymbol parent;
	
	private final String name;
	
	private final PlanarRange position;
	
	private final PlanarRange scope;
	
	private final boolean local;

	private boolean exported;
	
	public TypeScriptSymbol(@Nullable TypeScriptSymbol parent, String name, PlanarRange position, 
			@Nullable PlanarRange scope, boolean local) {
		this.parent = parent;
		this.name = name;
		this.position = position;
		this.scope = scope;
		this.local = local;
	}

	public void markExported() {
		exported = true;
	}

	@Override
	public TypeScriptSymbol getParent() {
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
		return local && !exported;
	}

	@Override
	public String getFQNSeparator() {
		return ".";
	}
	
}
