package io.onedev.commons.jsymbol.cpp.symbols;

import javax.annotation.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class CppSymbol extends Symbol {

    public enum Modifier{NORMAL,PRIVATE,PROTECTED,PUBLIC,FRIENDVAR,FRIENDFUNC,FRIENDCLASS,FRIENDSTRUCT,FRIENDUNION,
    	FRIENDENUM,FRIENDTYPEDEF,FRIENDNAMESPACE,CONSTANT,STATIC,MACRO};
	
	private static final long serialVersionUID = 1L;
	
	private final String name;
	
	private final CppSymbol parent;
	
	private final boolean local;
	
	private final PlanarRange position;
	
	private final PlanarRange scope;
	
	public CppSymbol(CppSymbol parent, String name, boolean local, 
			@Nullable PlanarRange position, @Nullable PlanarRange scope) {
		this.parent = parent;
		this.name = name;
		this.local = local;
		this.position = position;
		this.scope = scope;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public CppSymbol getParent() {
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
	public boolean isLocal() {
		return local;
	}

	@Override
	public String getFQNSeparator() {
		return ".";
	}
	
}
