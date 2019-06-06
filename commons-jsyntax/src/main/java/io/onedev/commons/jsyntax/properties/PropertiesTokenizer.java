package io.onedev.commons.jsyntax.properties;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class PropertiesTokenizer extends AbstractTokenizer<PropertiesTokenizer.State> {
	static class State {
		String position;
		boolean nextMultiline;
		boolean inMultiline;
		boolean afterSection;

		public State(String position, boolean nextMultiline, boolean inMultiline, boolean afterSection) {
			this.position = position;
			this.nextMultiline = nextMultiline;
			this.inMultiline = inMultiline;
			this.afterSection = afterSection;
		}

	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "properties", "ini", "in");
	}

	@Override
	public State startState() {
		return new State("def", false, false, false);
	}

	@Override
	public String token(StringStream stream, State state) {
		boolean sol = stream.sol() || state.afterSection;
		boolean eol = stream.eol();

		state.afterSection = false;
		
		if (sol) {
			if (state.nextMultiline) {
				state.inMultiline = true;
				state.nextMultiline = false;
			} else {
				state.position = "def";
			}
		}

		if (eol && !state.nextMultiline) {
			state.inMultiline = false;
			state.position = "def";
		}

		if (sol) {
			while (stream.eatSpace()) {       
			}
		}

		
		String ch = stream.next();
		if (sol && (ch.equals("#") || ch.equals("!") || ch.equals(";"))) {
			state.position = "comment";
			stream.skipToEnd();
			return "comment";
		} else if (sol && ch.equals("[")) {
			state.afterSection = true;
			stream.skipTo("]");
			stream.eat("]");
			return "header";
		} else if (ch.equals("=") || ch.equals(":")) {
			state.position = "quote";
			return "";
		} else if (ch.equals("\\") && state.position.equals("quote")) {
			if (stream.sol()) {
				state.nextMultiline = true;				
			}			
		}

		return state.position;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-properties");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("properties");
	}
}