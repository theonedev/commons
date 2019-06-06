package io.onedev.commons.jsyntax.sieve;

import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class SieveTokenizer extends AbstractTokenizer<SieveTokenizer.State> {

	static final Set<String> keywords = words("if elsif else stop require");
	static final Set<String> atoms = words("true false not");

	static class State {
		Processor tokenize;
		boolean _multiLineString;

		public State(Processor tokenize, boolean _multiLineString) {
			this.tokenize = tokenize;
			this._multiLineString = _multiLineString;
		}
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	static Set<String> words(String str) {
		Set<String> s = Sets.newHashSet();
		String[] arr = str.split(" ");
		for (String o : arr) {
			s.add(o);
		}
		return s;
	}

	static final Pattern pattern[] = new Pattern[7];
	static {
		pattern[0] = Pattern.compile("[{}\\(\\),;]");
		pattern[1] = Pattern.compile("\\d");
		pattern[2] = Pattern.compile("[\\d]");
		pattern[3] = Pattern.compile("[KkMmGg]");
		pattern[4] = Pattern.compile("[a-zA-Z_]");
		pattern[5] = Pattern.compile("[a-zA-Z0-9_]");
		pattern[6] = Pattern.compile("\\w");
	}

	class tokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			String ch = stream.next();
			if (ch.equals("/") && !stream.eat("*").isEmpty()) {
				state.tokenize = new tokenCComment();
				return new tokenCComment().process(stream, state);
			}
			if (ch.equals("#")) {
				stream.skipToEnd();
				return "comment";
			}
			if (ch.equals("\"")) {
				state.tokenize = new tokenString(ch);
				return state.tokenize.process(stream, state);
			}
			if (ch.equals("(")) {
				// state._indent.push("(");
				// state._indent.push("{");
				return "";
			}
			if (ch.equals("{")) {
				// state._indent.push("{");
				return "";
			}
			if (ch.equals(")")) {
				// state._indent.pop();
				// state._indent.pop();
			}
			if (ch.equals("}")) {
				// state._indent.pop();
				return "";
			}
			if (ch.equals(","))
				return "";
			if (ch.equals(";"))
				return "";
			if (pattern[0].matcher(ch).matches())
				return "";
			if (pattern[1].matcher(ch).matches()) {
				stream.eatWhile(pattern[2]);
				stream.eat(pattern[3]);
				return "number";
			}
			if (ch.equals(":")) {
				stream.eatWhile(pattern[4]);
				stream.eatWhile(pattern[5]);
				return "operator";
			}
			stream.eatWhile(pattern[6]);
			String cur = stream.current();
			if ((cur.equals("text")) && !stream.eat(":").isEmpty()) {
				state.tokenize = new tokenMultiLineString();
				return "string";
			}
			if (keywords.contains(cur))
				return "keyword";
			if (atoms.contains(cur))
				return "atom";
			return "";
		}

		class tokenMultiLineString implements Processor {

			@Override
			public String process(StringStream stream, State state) {
				state._multiLineString = true;
				if (!stream.sol()) {
					stream.eatSpace();
					if (stream.peek().equals("#")) {
						stream.skipToEnd();
						return "comment";
					}
					stream.skipToEnd();
					return "string";
				}
				if ((stream.next().equals(".")) && (stream.eol())) {
					state._multiLineString = false;
					state.tokenize = new tokenBase();
				}
				return "string";
			}

		}
	}

	class tokenCComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while (!(ch = stream.next()).isEmpty()) {
				if (maybeEnd && ch.equals("/")) {
					state.tokenize = new tokenBase();
					break;
				}
				maybeEnd = (ch.equals("*"));
			}
			return "comment";
		}

	}

	class tokenString implements Processor {
		String quote;

		public tokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state) {
			boolean escaped = false;
			String ch;
			while (!(ch = stream.next()).isEmpty()) {
				if (ch.equals(quote) && !escaped)
					break;
				escaped = !escaped && ch.equals("\\");
			}
			if (!escaped)
				state.tokenize = new tokenBase();
			return "string";
		}

	}

	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "sieve", "siv");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(new tokenBase(), false);
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.eatSpace())
			return "";
		if (state.tokenize != null)
			return state.tokenize.process(stream, state);
		else {
			return new tokenBase().process(stream, state);
		}
	}

	@Override
	public boolean acceptMime(String mime) {
		// TODO Auto-generated method stub
		return mime != null && mime.equals("text/x-sieve");
	}

	@Override
	public boolean acceptMode(String mode) {
		// TODO Auto-generated method stub
		return mode != null && mode.equals("ecl");
	}
}
