package io.onedev.commons.jsymbol.ruby.symbols;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.jsymbol.Symbol;
import io.onedev.commons.utils.PlanarRange;

public abstract class RubySymbol extends Symbol {

	private static final long serialVersionUID = 1L;

	private final RubySymbol parent;

	private final String name;

	private final PlanarRange position;

	private final PlanarRange scope;

	public RubySymbol(@Nullable RubySymbol parent, String name, @Nullable PlanarRange position,
			@Nullable PlanarRange scope) {
		this.parent = parent;
		this.name = name;
		this.position = position;
		this.scope = scope;
	}

	@Override
	public RubySymbol getParent() {
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
	public String getFQNSeparator() {
		return "::";
	}

}
