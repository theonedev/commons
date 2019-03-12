package io.onedev.commons.jsymbol.cpp.symbols;

import javax.annotation.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.jsymbol.TokenPosition;

public abstract class CppSymbol extends Symbol {

    public enum Modifier{NORMAL,PRIVATE,PROTECTED,PUBLIC,FRIENDVAR,FRIENDFUNC,FRIENDCLASS,FRIENDSTRUCT,FRIENDUNION,
    	FRIENDENUM,FRIENDTYPEDEF,FRIENDNAMESPACE,CONSTANT,STATIC,MACRO};
	
	private static final long serialVersionUID = 1L;
	
	private final String name;
	
	private final CppSymbol parent;
	
	private final boolean local;
	
	private final TokenPosition position;
	
	private final TokenPosition scope;
	
	public CppSymbol(CppSymbol parent, String name, boolean local, 
			@Nullable TokenPosition position, @Nullable TokenPosition scope) {
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
	public TokenPosition getPosition() {
		return position;
	}

	@Override
	public TokenPosition getScope() {
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
