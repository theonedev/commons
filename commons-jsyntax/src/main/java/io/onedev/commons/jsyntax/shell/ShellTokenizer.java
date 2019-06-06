package io.onedev.commons.jsyntax.shell;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class ShellTokenizer extends AbstractTokenizer<ShellTokenizer.State> {
	static class State {
		ArrayList<Processor> tokens;

		public State(ArrayList<Processor> tokens) {
			this.tokens = tokens;
		}
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	static Pattern[] pattern = new Pattern[5];

	static {
		pattern[0] = Pattern.compile("\\d");
		pattern[1] = Pattern.compile("\\w");
		pattern[2] = Pattern.compile("[\\w-]");
		pattern[3] = Pattern.compile("\\w+");
		pattern[4] = Pattern.compile("['\"({]");
	}

	class tokenBase implements Processor {
		Map<String, String> words = Maps.newHashMap();

		void define(String style, String string) {
			String[] split = string.split(" ");
			for (int i = 0; i < split.length; i++) {
				words.put(split[i], style);
			}
		}

		@Override
		public String process(StringStream stream, State state) {
			define("atom", "true false");
			define("keyword",
					"if then do else elif while until for in esac fi fin " + "fil done exit set unset export function");
			define("builtin",
					"ab awk bash beep cat cc cd chown chmod chroot clear cp "
							+ "curl cut diff echo find gawk gcc get git grep kill killall ln ls make "
							+ "mkdir openssl mv nc node npm ping ps restart rm rmdir sed service sh "
							+ "shopt shred source sort sleep ssh start stop su sudo tee telnet top "
							+ "touch vi vim wall wc wget who write yes zsh");
			if (stream.eatSpace())
				return "";

			boolean sol = stream.sol();
			String ch = stream.next();

			if (ch.equals("\\")) {
				stream.next();
				return "";
			}
			if (ch.equals("'") || ch.equals("\"") || ch.equals("`")) {
				state.tokens.add(0, new tokenString(ch, ch.equals("`") ? "quote" : "string"));
				return new tokenize().process(stream, state);
			}
			if (ch.equals("#")) {
				if (sol && (stream.eat("!").length() != 0)) {
					stream.skipToEnd();
					return "meta"; // 'comment'?
				}
				stream.skipToEnd();
				return "comment";
			}
			if (ch.equals("$")) {
				state.tokens.add(0, new tokenDollar());
				return new tokenize().process(stream, state);
			}
			if (ch.equals("+") || ch.equals("=")) {
				return "operator";
			}
			if (ch.equals("-")) {
				stream.eat("-");
				stream.eatWhile(pattern[1]);
				return "attribute";
			}
			if (pattern[0].matcher(ch).matches()) {
				stream.eatWhile(pattern[0]);
				if (stream.eol() || !pattern[1].matcher(stream.peek()).matches()) {
					return "number";
				}
			}
			stream.eatWhile(pattern[2]);
			String cur = stream.current();
			if (stream.peek().equals("=") && pattern[3].matcher(cur).matches())
				return "def";
			return words.containsKey(cur) ? words.get(cur) : "";
		}

	}

	class tokenString implements Processor {
		String quote;
		String style;

		public tokenString(String quote, String style) {
			this.quote = quote;
			this.style = style;
		}

		@Override
		public String process(StringStream stream, State state) {
			String close = quote.equals("(") ? ")" : quote.equals("{") ? "}" : quote;
			String next;
			boolean end = false;
			boolean escaped = false;
			while ((next = stream.next()).length() != 0) {
				if (next.equals(close) && !escaped) {
					end = true;
					break;
				}
				if (next.equals("$") && !escaped && !quote.equals("'")) {
					escaped = true;
					stream.backUp(1);
					state.tokens.add(0, new tokenDollar());
					break;
				}
				if (!escaped && next.equals(quote) && !quote.equals(close)) {
					state.tokens.add(0, new tokenString(quote, style));
					return new tokenize().process(stream, state);
				}
				escaped = (!escaped && next.equals("\\"));
			}
			if (end || !escaped)
				state.tokens.remove(0);
			return style;
		}
	}

	class tokenDollar implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (state.tokens.size() > 1)
				stream.eat("$");
			String ch = stream.next();
			if (pattern[4].matcher(ch).matches()) {
				state.tokens.set(0, new tokenString(ch, ch.equals("(") ? "quote" : ch.equals("{") ? "def" : "string"));
				return new tokenize().process(stream, state);
			}
			if (!pattern[0].matcher(ch).matches()) {
				stream.eatWhile(pattern[1]);
			}
			state.tokens.remove(0);
			return "def";
		}

	}

	class tokenize implements Processor {
		@Override
		public String process(StringStream stream, State state) {
			if (state.tokens.size() != 0) {
				return state.tokens.get(0).process(stream, state);
			}
			return new tokenBase().process(stream, state);
		}

	}

	@Override
	public boolean accept(String fileName) {

		return acceptExtensions(fileName, "sh", "ksh", "bash");
	}

	@Override
	public State startState() {

		ArrayList<Processor> tokens = new ArrayList<>();
		tokens.add(0, new tokenBase());
		return new State(tokens);
	}

	@Override
	public String token(StringStream stream, State state) {
		return state.tokens.get(0).process(stream, state);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-sh");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("shell");
	}
}
