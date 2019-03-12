package io.onedev.commons.jsyntax.commonlisp;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class CommonlispTokenizer extends AbstractTokenizer<CommonlispTokenizer.State> {

	private static Pattern specialForm = Pattern.compile("^(block|let*|return-from|catch|load-time-value|"
			+ "setq|eval-when|locally|symbol-macrolet|flet|macrolet|tagbody|function|multiple-value-call|"
			+ "the|go|multiple-value-prog1|throw|if|progn|unwind-protect|labels|progv|let|quote)$");

	private static Pattern assumeBody = Pattern.compile("^with.*|^def.*|^do.*|^prog.*|.*case$|^cond$|.*bind$|.*when$|.*unless$");

	private static Pattern numLiteral = Pattern.compile(
			"^(?:[+\\-]?(?:\\d+|\\d*\\.\\d+)(?:[efd][+\\-]?\\d+)?|[+\\-]?\\d+(?:\\/[+\\-]?\\d+)?|#b[+\\-]?[01]+|#o[+\\-]?[0-7]+|#x[+\\-]?[\\da-f]+)");

	private static Pattern symbol = Pattern.compile("[^\\s'`,@()\\[\\]\";]");

	static class State {
		Processor tokenize;
		Context context;
		String type;
		String lastType;

		State(Processor tokenize, Context context, String type, String lastType) {
			this.tokenize = tokenize;
			this.context = context;
			this.type = type;
			this.lastType = lastType;
		}
	}

	static class Context {
		Context prev;
		int start;
		int indenTo;
		Boolean indentToIsNum;

		Context(Context prev, int start, int indenTo, Boolean indentToIsNum) {
			this.prev = prev;
			this.start = start;
			this.indenTo = indenTo;
			this.indentToIsNum = indentToIsNum;
		}
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	private String readSym(StringStream stream) {

		String ch;
		while ((ch = stream.next()).length() != 0) {
			if (ch.equals("\\")) {
				stream.next();
			} else if (!symbol.matcher(ch).matches()) {
				stream.backUp(1);
				break;
			}
		}
		return stream.current();
	}

	private static Pattern baseToken1 = Pattern.compile("['`,@]");
	private static Pattern baseToken2 = Pattern.compile("[+\\-=\\.']");
	private static Pattern baseToken3 = Pattern.compile("\\d");
	private static Pattern baseToken4 = Pattern.compile("^\\d*#");

	class Base implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (stream.eatSpace()) {
				state.type = "ws";
				return "";
			}
			if (!stream.match(numLiteral).isEmpty()) {
				return "number";
			}
			String ch = stream.next();
			if (ch.equals("\\")) {
				ch = stream.next();
			}
			if (ch.equals("\"")) {
				state.tokenize = new inString();
				return state.tokenize.process(stream, state);
			} else if (ch.equals("(")) {
				state.type = "open";
				return "bracket";
			} else if (ch.equals(")") || ch.equals("]")) {
				state.type = "close";
				return "bracket";
			} else if (ch.equals(";")) {
				stream.skipToEnd();
				state.type = "ws";
				return "comment";
			} else if (baseToken1.matcher(ch).matches()) {
				return "";
			} else if (ch.equals("|")) {
				if (stream.skipTo("|")) {
					stream.next();
					return "symbol";
				} else {
					stream.skipToEnd();
					return "error";
				}
			} else if (ch.equals("#")) {
				String c = stream.next();
				if (c.equals("[")) {
					state.type = "open";
					return "bracket";
				} else if (baseToken2.matcher(c).matches()) {
					return "";
				} else if (baseToken3.matcher(c).matches() && !stream.match(baseToken4).isEmpty()) {
					return "";
				} else if (c.equals("|")) {
					state.tokenize = new inComment();
					return state.tokenize.process(stream, state);
				} else if (c.equals(":")) {
					readSym(stream);
					return "meta";
				} else {
					return "error";
				}
			} else {
				String name = readSym(stream);
				if (name.equals(".")) {
					return "";
				}
				state.type = "symbol";
				if (name.equals("nil") || name.equals("t") || name.substring(0, 1).equals(":")) {
					return "atom";
				}
				if (state.lastType.equals("open")
						&& (specialForm.matcher(name).matches() || assumeBody.matcher(name).matches())) {
					return "keyword";
				}
				if (name.substring(0, 1).equals("&")) {
					return "variable-2";
				}
				return "variable";
			}

		}
	}

	class inString implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			Boolean escaped = false;
			String next;
			while ((next = stream.next()).length() != 0) {
				if (next.equals("\"") && !escaped) {
					state.tokenize = new Base();
					break;
				}
				escaped = !escaped && next.equals("\\");
			}
			return "string";
		}
	}

	class inComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			String next;
			String last = "";
			while ((next = stream.next()).length() != 0) {
				if (next.equals("#") && last.equals("|")) {
					state.tokenize = new Base();
					break;
				}
				last = next;
			}
			state.type = "ws";
			return "comment";
		}
	}

	@Override
	public boolean accept(String fileName) {

		return acceptExtensions(fileName, "cl", "el", "lisp");
	}

	@Override
	public State startState() {

		return new State(new Base(), new Context(null, 0, 0, false), "", "");
	}

	@Override
	public String token(StringStream stream, State state) {

		if (stream.sol() && !state.context.indentToIsNum) {
			state.context.indenTo = state.context.start + 1;
			state.context.indentToIsNum = true;
		}
		state.type = "";
		String style = state.tokenize.process(stream, state);
		if (state.type != "ws") {
			if (state.context.indenTo == 0) {
				if (state.type.equals("symbol") && assumeBody.matcher(stream.current()).matches()) {
					state.context.indenTo = state.context.start + indentUnit();
					state.context.indentToIsNum = true;
				} else {
					state.context.indentToIsNum = false;
				}
			} else if (!state.context.indentToIsNum) {
				state.context.indenTo = stream.column();
				state.context.indentToIsNum = true;
			}
			state.lastType = state.type;
		}
		if (state.type.equals("open")) {
			state.context.prev = state.context;
			state.context.start = stream.column();
			state.context.indenTo = 0;
			state.context.indentToIsNum = false;
		} else if (state.type.equals("close")) {
			state.context = (state.context.prev != null ? state.context.prev : state.context);
		}
		return style;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-common-lisp");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("commonlisp");
	}
}
