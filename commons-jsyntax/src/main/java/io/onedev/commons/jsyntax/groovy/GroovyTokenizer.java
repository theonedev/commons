package io.onedev.commons.jsyntax.groovy;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class GroovyTokenizer extends AbstractTokenizer<GroovyTokenizer.State> {
	static class State {
		ArrayList<Processor> tokenize;
		Context context;
		int indented;
		boolean startOfLine;
		String lastToken;

		public State(ArrayList<Processor> tokenize, Context context, int indented, boolean startOfLine,
				String lastToken) {
			this.tokenize = tokenize;
			this.context = context;
			this.indented = indented;
			this.startOfLine = startOfLine;
			this.lastToken = lastToken;
		}

	}

	static interface Processor {
		String process(String quote, StringStream stream, State state);
	}

	static class Context {
		int indented;
		int column;
		String type;
		Boolean align;
		Context prev;

		public Context(int indented, int column, String type, Boolean align, Context prev) {
			super();
			this.indented = indented;
			this.column = column;
			this.type = type;
			this.align = align;
			this.prev = prev;
		}

	}

	static Pattern[] pattern = new Pattern[10];

	static {
		pattern[0] = Pattern.compile("[\\.\\[\\{\\(,;:]");
		pattern[1] = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.]");
		pattern[2] = Pattern.compile("\\d");
		pattern[3] = Pattern.compile("[\\w\\.]");
		pattern[4] = Pattern.compile("eE");
		pattern[5] = Pattern.compile("\\+\\-");
		pattern[6] = Pattern.compile("[+\\-*&%=<>!?|\\/~]");
		pattern[7] = Pattern.compile("[+\\-*&%=<>|~]");
		pattern[8] = Pattern.compile("[\\w\\$_]");
		pattern[9] = Pattern.compile("[\\w\\$_\\.]");
	}

	static Set<String> keywords = wordsOf(
			"abstract as assert boolean break byte case catch char class const continue def default "
					+ "do double else enum extends final finally float for goto if implements import in "
					+ "instanceof int interface long native new package private protected public return "
					+ "short static strictfp super switch synchronized threadsafe throw throws transient "
					+ "try void volatile while");
	static Set<String> blockKeywords = wordsOf(
			"catch class do else finally for if switch try while enum interface def");
	static Set<String> standaloneKeywords = wordsOf("return break continue");
	static Set<String> atoms = wordsOf("null true false this");
	String curPunc;

	class tokenBase implements Processor {

		@Override
		public String process(String quote, StringStream stream, State state) {
			String ch = stream.next();
			if (ch.equals("\"") || ch.equals("'")) {
				return new startString().process(ch, stream, state);
			}
			if (pattern[1].matcher(ch).matches()) {
				curPunc = ch;
				return "";
			}
			if (pattern[2].matcher(ch).matches()) {
				stream.eatWhile(pattern[3]);
				if (stream.eat(pattern[4]).length() != 0) {
					stream.eat(pattern[5]);
					stream.eatWhile(pattern[2]);
				}
				return "number";
			}
			if (ch.equals("/")) {
				if (stream.eat("*").length() != 0) {
					state.tokenize.add(new tokenComment());
					return new tokenComment().process("", stream, state);
				}
				if (stream.eat("/").length() != 0) {
					stream.skipToEnd();
					return "comment";
				}
				if (expectExpression(state.lastToken, false)) {
					return new startString().process(ch, stream, state);
				}
			}
			if (ch.equals("-") && stream.eat(">").length() != 0) {
				curPunc = "->";
				return "";
			}
			if (pattern[6].matcher(ch).matches()) {
				stream.eatWhile(pattern[7]);
				return "operator";
			}
			stream.eatWhile(pattern[8]);
			if (ch.equals("@")) {
				stream.eatWhile(pattern[9]);
				return "meta";
			}
			if (state.lastToken.equals("."))
				return "property";
			if (stream.eat(":").length() != 0) {
				curPunc = "proplabel";
				return "property";
			}
			String cur = stream.current();
			if (atoms.contains(cur)) {
				return "atom";
			}
			if (keywords.contains(cur)) {
				if (blockKeywords.contains(cur)) {
					curPunc = "newstatement";
				} else if (standaloneKeywords.contains(cur)) {
					curPunc = "standalone";
				}
				return "keyword";
			}
			return "variable";
		}

	}

	class tokenBaseUntilBrace implements Processor {

	
		@Override
		public String process(String quote, StringStream stream, State state) {
			int depth = 1;

			if (stream.peek().equals("}")) {
				depth--;
				if (depth == 0) {
					state.tokenize.remove(state.tokenize.size() - 1);
					return state.tokenize.get(state.tokenize.size() - 1).process(quote, stream, state);
				}
			} else if (stream.peek().equals("{")) {
				depth++;
			}
			return new tokenBase().process(quote, stream, state);

		}
	}

	class startString implements Processor {

		@Override
		public String process(String quote, StringStream stream, State state) {
			boolean tripleQuoted = false;
			if (!quote.equals("/") && stream.eat(quote).length() != 0) {
				if (stream.eat(quote).length() != 0) {
					tripleQuoted = true;
				} else {
					return "string";
				}
			}
			state.tokenize.add(new startString());
			boolean escaped = false;
			String next;
			boolean end = !tripleQuoted;
			while ((next = stream.next()).length() != 0) {
				if (next.equals(quote) && !escaped) {
					if (!tripleQuoted) {
						break;
					}
					if (stream.match(quote + quote)) {
						end = true;
						break;
					}
				}
				if (quote.equals("\"") && next.equals("$") && !escaped && stream.eat("{").length() != 0) {
					state.tokenize.add(new tokenBaseUntilBrace());
					return "string";
				}
				escaped = !escaped && next.equals("\\");
			}
			if (end)
				state.tokenize.remove(state.tokenize.size() - 1);
			return "string";
		}

	}

	class tokenComment implements Processor {

		@Override
		public String process(String quote, StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals("/") && maybeEnd) {
					state.tokenize.remove(state.tokenize.size() - 1);
					break;
				}
				maybeEnd = ch.equals("*");
			}
			return "comment";
		}

	}

	private boolean expectExpression(String last, boolean newline) {
		return last.length() == 0 || last.equals("operator") || last.equals("->") || pattern[0].matcher(last).matches()
				|| last.equals("newstatement") || last.equals("keyword") || last.equals("proplabel")
				|| (last.equals("standalone") && !newline);
	}

	private Context pushContext(State state, int col, String type) {
		state.context = new Context(state.indented, col, type, null, state.context);
		return state.context;
	}

	private Context popContext(State state) {
		String t = state.context.type;
		if (t.equals(")") || t.equals("]") || t.equals("}"))
			state.indented = state.context.indented;
		state.context = state.context.prev;
		return state.context;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "groovy", "gradle");
	}

	@Override
	public State startState() {
		ArrayList<Processor> tokenize = new ArrayList<>();
		tokenize.add(0, new tokenBase());
		return new State(tokenize, new Context(-indentUnit(), 0, "top", false, null), 0, true, "");
	}

	@Override
	public String token(StringStream stream, State state) {
		Context ctx = state.context;
		if (stream.sol()) {
			if (ctx.align == null)
				ctx.align = false;
			state.indented = stream.indentation();
			state.startOfLine = true;

			if (ctx.type.equals("statement") && !expectExpression(state.lastToken, true)) {
				popContext(state);
				ctx = state.context;
			}
		}

		if (stream.eatSpace())
			return "";
		curPunc = "";
		String style = state.tokenize.get(0).process("", stream, state);
		if (style.equals("comment"))
			return style;
		if (ctx.align == null)
			ctx.align = true;

		if ((curPunc.equals(";") || curPunc.equals(":")) && ctx.type.equals("statement")) {
			popContext(state);
		} else if (curPunc.equals("->") && ctx.type.equals("statement") && ctx.prev.type.equals("}")) {
			popContext(state);
			state.context.align = false;
		} else if (curPunc.equals("{")) {
			pushContext(state, stream.column(), "}");
		} else if (curPunc.equals("[")) {
			pushContext(state, stream.column(), "]");
		} else if (curPunc.equals("(")) {
			pushContext(state, stream.column(), ")");
		} else if (curPunc.equals("}")) {
			while (ctx.type.equals("statement")) {
				ctx = popContext(state);
			}
			if (ctx.type.equals("}"))
				ctx = popContext(state);
			while (ctx.type.equals("statement"))
				ctx = popContext(state);
		} else if (curPunc.equals(ctx.type)) {
			popContext(state);
		} else if (ctx.type.equals("}") || ctx.type.equals("top")
				|| (ctx.type.equals("statement") && curPunc.equals("newstatement"))) {
			pushContext(state, stream.column(), "statement");
		}

		state.startOfLine = false;
		if (curPunc.length() != 0) {
			state.lastToken = curPunc;
		} else {
			state.lastToken = style;
		}
		return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-groovy");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("groovy");
	}
}
