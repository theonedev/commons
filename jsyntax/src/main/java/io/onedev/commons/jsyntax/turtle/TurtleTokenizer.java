package io.onedev.commons.jsyntax.turtle;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class TurtleTokenizer extends AbstractTokenizer<TurtleTokenizer.State> {
	static Pattern ops = wordRegexp(Lists.newArrayList(""));
	static Pattern keywords = wordRegexp(Lists.newArrayList("@prefix", "@base", "a"));
	static Pattern operatorChars = Pattern.compile("[*+\\-<>=&|]");
	static final Pattern pattern[] = new Pattern[4];
	static final Pattern pattern1[] = new Pattern[3];

	static Pattern wordRegexp(List<String> words) {
		return Pattern.compile("^(?:" + StringUtils.join(words, "|") + ")$", Pattern.CASE_INSENSITIVE);
	}

	static class State {
		Processor tokenize;
		Context context;
		String curPunc;
		int col;

		State(Processor tokenize, Context context, String curPunc, int col) {
			this.tokenize = tokenize;
			this.context = context;
			this.curPunc = curPunc;
			this.col = col;
		}
	}

	static class Context {
		String type;
		Boolean align;
		int col;
		Context prev;

		Context(String type, Boolean align, int col, Context prev) {
			this.type = type;
			this.align = align;
			this.col = col;
			this.prev = prev;
		}
	}

	static interface Processor {
		String processor(StringStream stream, State state);
	}

	static {
		pattern[0] = Pattern.compile("^[\\s\\u00a0=]");
		pattern[1] = Pattern.compile("^[^\\s\\u00a0>]*>?");
		pattern[2] = Pattern.compile("[{}\\(\\),\\.;\\[\\]]");
		pattern[3] = Pattern.compile("[_\\w\\d]");
	}

	class tokenBase implements Processor {
		@Override
		public String processor(StringStream stream, State state) {
			state.curPunc = "";
			String ch = stream.next();
			if (ch.equals("<") && stream.match(pattern[0], false).isEmpty()) {
				stream.match(pattern[1]);
				return "atom";
			} else if (ch.equals("\"") || ch.equals("'")) {
				state.tokenize = new tokenLiteral(ch);
				return state.tokenize.processor(stream, state);
			} else if (pattern[2].matcher(ch).matches()) {
				state.curPunc = ch;
				return "";
			} else if (ch.equals("#")) {
				stream.skipToEnd();
				return "comment";
			} else if (operatorChars.matcher(ch).matches()) {
				stream.eatWhile(operatorChars);
				return "";
			} else if (ch.equals(":")) {
				return "operator";
			} else {
				stream.eatWhile(pattern[3]);
				if (stream.peek().equals(":")) {
					return "variable-3";
				} else {
					String word = stream.current();
					if (keywords.matcher(word).matches()) {
						return "meta";
					}
					if (ch.compareTo("A") >= 0 && ch.compareTo("Z") <= 0) {
						return "comment";
					} else {
						return "keyword";
					}
				}
			}
		}
	}

	class tokenLiteral implements Processor {
		String quote;

		public tokenLiteral(String quote) {
			this.quote = quote;
		}

		@Override
		public String processor(StringStream stream, State state) {
			boolean escaped = false;
			String ch;
			while (!(ch = stream.next()).isEmpty()) {
				if (ch.equals(quote) && !escaped) {
					state.tokenize = new tokenBase();
					break;
				}
				escaped = !escaped && ch.equals("\\");
			}
			return "string";
		}
	}

	static void pushContext(State state, String type, int col) {
		state.context = new Context(type, false, col, state.context);
	}

	static void popContext(State state) {
		state.context = state.context.prev;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "turtle");
	}

	@Override
	public State startState() {
		return new State(new tokenBase(), null, "", 0);
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-turtle");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("turtle");
	}

	static {
		pattern1[0] = Pattern.compile("[\\]\\}\\)]");
		pattern1[1] = Pattern.compile("[\\}\\]]");
		pattern1[2] = Pattern.compile("atom|string|variable");
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.sol()) {
			if (state.context != null && state.context.align == null)
				state.context.align = false;
		}
		if (stream.eatSpace())
			return "";
		String style = state.tokenize.processor(stream, state);
		if (style.equals(style) == false && state.context != null && state.context.align == null
				&& !state.context.type.equals("pattern")) {
			state.context.align = true;
		}
		if (state.curPunc.equals("(")) {
			pushContext(state, ")", stream.column());
		} else if (state.curPunc.equals("["))
			pushContext(state, "]", stream.column());
		else if (state.curPunc.equals("{"))
			pushContext(state, "}", stream.column());
		else if (pattern1[0].matcher(state.curPunc).matches()) {
			while (state.context != null && state.context.type.equals("pattern"))
				popContext(state);
			if (state.context != null && state.curPunc.equals(state.context.type))
				popContext(state);
		} else if (state.curPunc.equals(".") && state.context != null && state.context.type.equals("pattern"))
			popContext(state);
		else if (pattern1[2].matcher(style).matches() && state.context != null) {
			if (pattern1[1].matcher(state.context.type).matches())
				pushContext(state, "pattern", stream.column());
			else if (state.context.type.equals("pattern") && !state.context.align) {
				state.context.align = true;
				state.context.col = stream.column();
			}
		}
		return style;
	}
}
