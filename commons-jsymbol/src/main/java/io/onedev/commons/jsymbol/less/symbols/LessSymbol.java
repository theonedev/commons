package io.onedev.commons.jsymbol.less.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

/**
 * @author robin
 *
 */
public abstract class LessSymbol extends Symbol {

	private static final long serialVersionUID = 1L;
	
	private final LessSymbol parent;
	
	private final String name;
	
	private final PlanarRange position;
	
	private final PlanarRange scope;
	
	public LessSymbol(LessSymbol parent, String name, @Nullable PlanarRange position, 
			@Nullable PlanarRange scope) {
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
	public LessSymbol getParent() {
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
		return " ";
	}
	
}
