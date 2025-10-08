package io.onedev.commons.jsymbol.csharp.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;
import io.onedev.commons.jsymbol.util.QualifiedName;

public abstract class CSharpSymbol extends Symbol {

	public enum Modifier {NEW, PUBLIC, PROTECTED, INTERNAL, PRIVATE, READONLY, VOLATILE, VIRTUAL, SEALED, OVERRIDE,
			ABSTRACT, STATIC, UNSAFE, EXTERN, PARTIAL, ASYNC}

	private static final long serialVersionUID = 1L;
	
	private final QualifiedName qualifiedName;
	
	private final CSharpSymbol parent;
	
	private final PlanarRange position;
	
	private final PlanarRange scope;
	
	public CSharpSymbol(CSharpSymbol parent, String qualifiedName, @Nullable PlanarRange position, 
			@Nullable PlanarRange scope) {
		this.parent = parent;
		this.position = position;
		this.scope = scope;
		this.qualifiedName = new QualifiedName(qualifiedName, "~", "::", ".");
	}

	public CSharpSymbol(CSharpSymbol parent, QualifiedName qualifiedName, @Nullable PlanarRange position, 
			@Nullable PlanarRange scope) {
		this.parent = parent;
		this.position = position;
		this.scope = scope;
		this.qualifiedName = qualifiedName;
	}
	
	@Override
	public String getName() {
		return qualifiedName.getUnqualified();
	}

	public QualifiedName getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public CSharpSymbol getParent() {
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
	public String getFQNSeparator() {
		return ".";
	}
	
}
