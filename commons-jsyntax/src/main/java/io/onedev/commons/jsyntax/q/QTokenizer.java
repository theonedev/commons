package io.onedev.commons.jsyntax.q;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class QTokenizer extends AbstractTokenizer<QTokenizer.State> {
	static class State {
		Processor tokenize;
		Context context;
		int indent;
		int col;

		public State(Processor tokenize, Context context, int indent, int col) {

			this.tokenize = tokenize;
			this.context = context;
			this.indent = indent;
			this.col = col;
		}
	}

	static class Context {

		Context prev;
		int indent;
		int col;
		String type;
		Boolean align;

		public Context(Context prev, int indent, int col, String type) {
			this.prev = prev;
			this.indent = indent;
			this.col = col;
			this.type = type;

		}

	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	static Pattern[] pattern = new Pattern[22];

	static {
		pattern[0] = Pattern.compile("\\s");
		pattern[1] = Pattern.compile("^\\\\\\s*$");
		pattern[2] = Pattern.compile("[A-Z|a-z|\\d|_|:|\\/|\\.]");
		pattern[3] = Pattern.compile("^\\d{4}\\.\\d{2}(m|\\.\\d{2}([D|T](\\d{2}(:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?)?)?)?)");
		pattern[4] = Pattern.compile("\\d");
		pattern[5] = Pattern.compile("^\\d+D(\\d{2}(:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?)?)");
		pattern[6] = Pattern.compile("^\\d{2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?");
		pattern[7] = Pattern.compile("^\\d+[ptuv]{1}");
		pattern[8] = Pattern.compile("^0[NwW]{1}");
		pattern[9] = Pattern.compile("^0x[\\d|a-f|A-F]*");
		pattern[10] = Pattern.compile("^[0|1]+[b]{1}");
		pattern[11] = Pattern.compile("^\\d+[chijn]{1}");
		pattern[12] = Pattern.compile("-?\\d*(\\.\\d*)?(e[+\\-]?\\d+)?(e|f)?");
		pattern[13] = Pattern.compile("[A-Z|a-z]|\\.");
		pattern[14] = Pattern.compile("[A-Z|a-z|\\.|_|\\d]");
		pattern[15] = Pattern.compile("[|/&^!+:\\\\\\-*%$=~#;@><\\.,?_\\']");
		pattern[16] = Pattern.compile("[{}\\(\\[\\]\\)]");
		pattern[17] = Pattern.compile("\\/\\s*$");
		pattern[18] = Pattern.compile("^\\\\\\s*$");
		pattern[19] = Pattern.compile("[\\]\\}\\)]");
		pattern[20] = Pattern.compile("atom|string|variable");
		pattern[21] = Pattern.compile("[\\}\\]]");
	}

	static String curPunc;
	static String[] list = new String[] { "abs", "acos", "aj", "aj0", "all", "and", "any", "asc", "asin", "asof",
			"atan", "attr", "avg", "avgs", "bin", "by", "ceiling", "cols", "cor", "cos", "count", "cov", "cross", "csv",
			"cut", "delete", "deltas", "desc", "dev", "differ", "distinct", "div", "do", "each", "ej", "enlist", "eval",
			"except", "exec", "exit", "exp", "fby", "fills", "first", "fkeys", "flip", "floor", "from", "get", "getenv",
			"group", "gtime", "hclose", "hcount", "hdel", "hopen", "hsym", "iasc", "idesc", "if", "ij", "in", "insert",
			"inter", "inv", "key", "keys", "last", "like", "list", "lj", "load", "log", "lower", "lsq", "ltime",
			"ltrim", "mavg", "max", "maxs", "mcount", "md5", "mdev", "med", "meta", "min", "mins", "mmax", "mmin",
			"mmu", "mod", "msum", "neg", "next", "not", "null", "or", "over", "parse", "peach", "pj", "plist", "prd",
			"prds", "prev", "prior", "rand", "rank", "ratios", "raze", "read0", "read1", "reciprocal", "reverse",
			"rload", "rotate", "rsave", "rtrim", "save", "scan", "select", "set", "setenv", "show", "signum", "sin",
			"sqrt", "ss", "ssr", "string", "sublist", "sum", "sums", "sv", "system", "tables", "tan", "til", "trim",
			"txf", "type", "uj", "ungroup", "union", "update", "upper", "upsert", "value", "var", "view", "views", "vs",
			"wavg", "where", "where", "while", "within", "wj", "wj1", "wsum", "xasc", "xbar", "xcol", "xcols", "xdesc",
			"xexp", "xgroup", "xkey", "xlog", "xprev", "xrank" };
	static Pattern E = Pattern.compile("[|/&^!+:\\\\\\-*%$=~#;@><,?_\\'\\\"\\[\\(\\]\\)\\s{}]");

	static String join(String join, String[] list) {
		StringBuffer keywords = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			if (i == (list.length - 1)) {
				keywords.append(list[i]);
			} else {
				keywords.append(list[i]).append(join);
			}
		}
		return new String(keywords);
	}

	static Pattern keywords = Pattern.compile("^(" + join("|", list) + ")$");

	private void pushContext(State state, String type, int col) {
		state.context = new Context(state.context, state.indent, col, type);
	}

	private void popContext(State state) {
		state.indent = state.context.indent;
		state.context = state.context.prev;
	}

	class tokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean sol = stream.sol();
			String c = stream.next();
			curPunc = "";
			if (sol) {
				if (c.equals("/")) {
					state.tokenize = new tokenLineComment();
					return state.tokenize.process(stream, state);
				} else if (c.equals("\\")) {
					if (stream.eol() || pattern[0].matcher(stream.peek()).matches()) {
						stream.skipToEnd();
						if (pattern[1].matcher(stream.current()).matches()) {
							state.tokenize = new tokenCommentToEOF();
							return state.tokenize.process(stream, state);
						} else {
							state.tokenize = new tokenBase();
							return "comment";
						}
					} else {
						state.tokenize = new tokenBase();
						return "builtin";
					}
				}
			}
			if (pattern[0].matcher(c).matches()) {
				if (stream.peek().equals("/")) {
					stream.skipToEnd();
					return "comment";
				} else {
					return "whitespace";
				}
			}
			if (c.equals("\"")) {
				state.tokenize = new tokenString();
				return state.tokenize.process(stream, state);
			}
			if (c.equals("`")) {
				stream.eatWhile(pattern[2]);
				return "symbol";
			}
			if ((".".equals(c) && pattern[4].matcher(stream.peek()).matches()) || pattern[4].matcher(c).matches()) {
				String t = "";
				stream.backUp(1);
				if (!stream.match(pattern[3]).isEmpty() || !stream.match(pattern[5]).isEmpty()
						|| !stream.match(pattern[6]).isEmpty() || !stream.match(pattern[7]).isEmpty()) {
					t = "temporal";
				} else if (!stream.match(pattern[8]).isEmpty() || !stream.match(pattern[9]).isEmpty()
						|| !stream.match(pattern[10]).isEmpty() || !stream.match(pattern[11]).isEmpty()
						|| !stream.match(pattern[12]).isEmpty()) {
					t = "number";
				}
				c = stream.peek();
				if (t.length() != 0 && (c.length() == 0 || E.matcher(c).matches())) {
					return t;
				} else {
					stream.next();
					return "error";
				}
			}
			if (pattern[13].matcher(c).matches()) {
				stream.eatWhile(pattern[14]);
				return keywords.matcher(stream.current()).matches() ? "keyword" : "variable";
			}
			if (pattern[15].matcher(c).matches())
				return "";
			if (pattern[16].matcher(c).matches())
				return "";
			return "error";

		}
	}

	class tokenString implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean escaped = false;
			String next;
			boolean end = false;
			while ((next = stream.next()).length() != 0) {
				if (next.equals("\"") && !escaped) {
					end = true;
					break;
				}
				escaped = !escaped && next.equals("\\");
			}
			if (end)
				state.tokenize = new tokenBase();
			return "string";
		}

	}

	class tokenLineComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.skipToEnd();
			if (pattern[17].matcher(stream.current()).matches()) {
				state.tokenize = new tokenBlockComment();
				return state.tokenize.process(stream, state);
			} else {
				state.tokenize = new tokenBase();
				return "comment";
			}
		}

	}

	class tokenCommentToEOF implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.skipToEnd();
			return "comment";
		}

	}

	class tokenBlockComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean f = stream.sol() && stream.peek().equals("\\");
			stream.skipToEnd();
			if (f && pattern[18].matcher(stream.current()).matches()) {
				state.tokenize = new tokenBase();
			}
			return "comment";
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "q");
	}

	@Override
	public State startState() {
		return new State(new tokenBase(), null, 0, 0);
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.sol()) {
			if (state.context != null && state.context.align == null)
				state.context.align = false;
			state.indent = stream.indentation();
		}
		String style = state.tokenize.process(stream, state);
		if (!style.equals("comment") && state.context != null && state.context.align == null
				&& !state.context.type.equals("pattern")) {
			state.context.align = true;
		}
		if (curPunc.equals("("))
			pushContext(state, ")", stream.column());
		else if (curPunc.equals("["))
			pushContext(state, "]", stream.column());
		else if (curPunc.equals("{"))
			pushContext(state, "}", stream.column());
		else if (pattern[19].matcher(curPunc).matches()) {
			while (state.context != null && state.context.type.equals("pattern")) {
				popContext(state);
			}
			if (state.context != null && curPunc.equals(state.context.type))
				popContext(state);
		} else if (curPunc.equals(".") && state.context != null && state.context.type.equals("pattern")) {
			popContext(state);
		} else if (pattern[20].matcher(style).matches() && state.context != null) {
			if (pattern[21].matcher(state.context.type).matches()) {
				pushContext(state, "pattern", stream.column());
			} else if (state.context.type.equals("pattern") && !state.context.align) {
				state.context.align = true;
				state.context.col = stream.column();
			}
		}
		return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-q");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("q");
	}
}
