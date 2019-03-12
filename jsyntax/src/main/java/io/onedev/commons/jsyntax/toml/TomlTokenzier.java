package io.onedev.commons.jsyntax.toml;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class TomlTokenzier extends AbstractTokenizer<TomlTokenzier.State> {

	static class State {
		boolean inString;
		String stringType;
		boolean lhs;
		int inArray;

		public State(boolean inString, String stringType, boolean lhs, int inArray) {
			this.inString = inString;
			this.stringType = stringType;
			this.lhs = lhs;
			this.inArray = inArray;
		}
	}


	static final Pattern pattern[] = new Pattern[4];
	static {
		pattern[0] = Pattern.compile("^.[^\\\\\\\"\\']*");
		pattern[1] = Pattern.compile("^\\d\\d\\d\\d[\\d\\-\\:\\.T]*Z");
		pattern[2] = Pattern.compile("^\\-?\\d+(?:\\.\\d+)?");
		pattern[3] = Pattern.compile("^(\\s|=)");
	}

	@Override
	public String token(StringStream stream, State state) {
		// TODO Auto-generated method stub
		if (!state.inString && ((stream.peek().equals("\"")) || (stream.peek().equals("'")))) {
			state.stringType = stream.peek();
			stream.next();
			state.inString = true;
		}
		if (stream.sol() && state.inArray == 0) {
			state.lhs = true;
		}
		if (state.inString) {
			while (state.inString && !stream.eol()) {
				if (stream.peek().equals(state.stringType)) {
					stream.next();
					state.inString = false;
				} else if (stream.peek().equals("\\")) {
					stream.next();
					stream.next();
				} else {
					stream.match(pattern[0]).isEmpty();
				}
			}
			return state.lhs ? "property string" : "string";
		} else if (state.inArray != 0 && stream.peek().equals("]")) {
			stream.next();
			state.inArray--;
			return "bracket";
		} else if (state.lhs && stream.peek().equals("[") && stream.skipTo("]")) {
			stream.next();
			if (stream.peek().equals("]"))
				stream.next();
			return "atom";
		} else if (stream.peek().equals("#")) {
			stream.skipToEnd();
			return "comment";
		} else if (stream.eatSpace()) {
			return "";
		} else if (state.lhs && stream.eatWhile(Pattern.compile("[^\\s=]"))) {
			return "property";
		} else if (state.lhs && stream.peek().equals("=")) {
			stream.next();
			state.lhs = false;
			return "";
		} else if (!state.lhs && !stream.match(pattern[1]).isEmpty()) {
			return "atom";
		} else if (!state.lhs && (stream.match("true") || stream.match("false"))) {
			return "atom";
		} else if (!state.lhs && stream.peek().equals("[")) {
			state.inArray++;
			stream.next();
			return "bracket";
		} else if (!state.lhs && !stream.match(pattern[2]).isEmpty()) {
			return "number";
		} else if (!stream.eatSpace()) {
			stream.next();
		}
		return "";
	}

	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "toml");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(false, "", true, 0);
	}
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-toml");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("toml");
	}
}
