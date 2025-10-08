package io.onedev.commons.codeassist.parser;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.codeassist.grammar.TerminalElementSpec;

public class TerminalExpect extends ParseExpect {

	public TerminalExpect(@Nullable ParseExpect parent, State state, EarleyParser parser) {
		super(parent, state, parser);
	}

	public TerminalElementSpec getElementSpec() {
		return (TerminalElementSpec) super.getElementSpec();
	}
	
}
