package io.onedev.commons.jsyntax.d;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class DTokenizer extends AbstractTokenizer<DTokenizer.State> {

	private static Set<String> blockKeywords = Sets.newHashSet("body", "catch", "class", "do", "else", "enum", "for",
			"foreach", "foreach_reverse", "if", "in", "interface", "mixin", "out", "scope", "struct", "switch", "try",
			"union", "unittest", "version", "while", "with");

	private static Set<String> keywords = Sets.newHashSet("body", "catch", "class", "do", "else", "enum", "for",
			"foreach", "foreach_reverse", "if", "in", "interface", "mixin", "out", "scope", "struct", "switch", "try",
			"union", "unittest", "version", "while", "with", "abstract", "alias", "align", "asm", "assert", "auto",
			"break", "case", "cast", "cdouble", "cent", "cfloat", "const", "continue", "debug", "default", "delegate",
			"delete", "deprecated", "export", "extern", "final", "finally", "function", "goto", "immutable", "import",
			"inout", "invariant", "is", "lazy", "macro", "module", "new", "nothrow", "override", "package", "pragma",
			"private", "protected", "public", "pure", "ref", "return", "shared", "short ", "static", "super",
			"synchronized", "template", "this", "throw", "typedef", "typeid", "typeof", "volatile", "__FILE__",
			"__LINE__", "__gshared", "__traits", "__vector", "__parameters");

	private static Set<String> builtin = Sets.newHashSet("bool", "byte", "char", "creal", "dchar", "double", "float",
			"idouble", "ifloat", "int", "ireal", "long", "real", "short", "ubyte", "ucent", "uint", "ulong", "ushort",
			"wchar", "wstring", "void", "size_t", "sizediff_t");

	private static Set<String> atoms = Sets.newHashSet("exit", "failure", "success", "true", "false", "null");

	private static Pattern isOperatorChar = Pattern.compile("[+\\-*&%=<>!?|\\/]");

	private static Pattern PATTERN1 = Pattern.compile("[\\w\\$_]");

	private static Map<String, Processor> hooks = Maps.newHashMap();

	static {
		hooks.put("@", new Processor() {

			@Override
			public String process(StringStream stream, State state) {
				stream.eatWhile(PATTERN1);
				return "meta";
			}
		});
	}

	static class State {
		Processor tokenize;
		Context context;
		String curPunc;
		int indented;
		boolean startOfLine;

		State(Processor tokenize, Context context, int indented, boolean startOfLine) {
			this.tokenize = tokenize;
			this.context = context;
			this.indented = indented;
			this.startOfLine = startOfLine;

		}
	}

	static class Context {
		int indented;
		int column;
		String type;
		Boolean align;
		Context prev;

		Context(int indented, int column, String type, Boolean align, Context prev) {
			this.indented = indented;
			this.column = column;
			this.type = type;
			this.align = align;
			this.prev = prev;
		}

	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	private Context pushContext(State state, int col, String type) {
		int indent = state.indented;
		if (state.context != null && state.context.type.equals("statement")) {
			indent = state.context.indented;
		}
		return state.context = new Context(indent, col, type, false, state.context);
	}

	private Context popContext(State state) {
		String t = state.context.type;
		if (t.equals(")") || t.equals("]") || t.equals("}")) {
			state.indented = state.context.indented;
		}
		return state.context = state.context.prev;
	}

	protected int statementIndentUnit() {
		return indentUnit();
	}

	protected boolean multiLineStrings() {
		return false;
	}

	private static Pattern isPunctuationChar = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.]");
	private static Pattern number = Pattern.compile("\\d");
	private static Pattern allWord = Pattern.compile("[\\w\\$_\\xa1-\\uffff]");
	private static Pattern TOKEN_BASE_PATTERN1 = Pattern.compile("[\\w\\.]");

	private String tokenBase(StringStream stream, State state) {

		String ch = stream.next();
		if (hooks.get(ch) != null) {
			String result = hooks.get(ch).process(stream, state);
			if (result.length() != 0) {
				return result;
			}
		}
		if (ch.equals("\"") || ch.equals("'") || ch.equals("`")) {
			state.tokenize = new TokenString(ch);
			return state.tokenize.process(stream, state);
		}
		if (isPunctuationChar.matcher(ch).matches()) {
			state.curPunc = ch;
			return "";
		}
		if (number.matcher(ch).matches()) {
			stream.eatWhile(TOKEN_BASE_PATTERN1);
			return "number";
		}
		if (ch.equals("/")) {
			if (stream.eat("+").length() != 0) {
				state.tokenize = new TokenNestedComment();
				return state.tokenize.process(stream, state);
			}
			if (stream.eat("*").length() != 0) {
				state.tokenize = new TokenComment();
				return state.tokenize.process(stream, state);
			}
			if (stream.eat("/").length() != 0) {
				stream.skipToEnd();
				return "comment";
			}
		}
		if (isOperatorChar.matcher(ch).matches()) {
			stream.eatWhile(isOperatorChar);
			return "operator";
		}
		stream.eatWhile(allWord);

		String cur = stream.current();
		if (keywords.contains(cur)) {
			if (blockKeywords.contains(cur)) {
				state.curPunc = "newstatement";
			}
			return "keyword";
		}
		if (builtin.contains(cur)) {
			if (blockKeywords.contains(cur)) {
				state.curPunc = "newstatement";
			}
			return "builtin";
		}
		if (atoms.contains(cur)) {
			return "atom";
		}
		return "variable";
	}

	class TokenString implements Processor {

		private String quote;

		TokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state) {

			boolean escaped = false;
			String next;
			boolean end = false;
			while ((next = stream.next()).length() != 0) {
				if (next.equals(quote) && !escaped) {
					end = true;
					break;
				}
				escaped = !escaped && next.equals("\\");
			}
			if (end || !(escaped || multiLineStrings())) {
				state.tokenize = null;
			}
			return "string";
		}
	}

	class TokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals("/") && maybeEnd) {
					state.tokenize = null;
					break;
				}
				maybeEnd = (ch.equals("*"));
			}
			return "comment";
		}
	}

	class TokenNestedComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals("/") && maybeEnd) {
					state.tokenize = null;
					break;
				}
				maybeEnd = (ch.equals("+"));
			}
			return "comment";
		}
	}

	@Override
	public boolean accept(String fileName) {

		return acceptExtensions(fileName, "d");
	}

	@Override
	public State startState() {

		return new State(null, new Context(-indentUnit(), 0, "top", false, null), 0, true);
	}

	@Override
	public String token(StringStream stream, State state) {

		Context ctx = state.context;
		if (stream.sol()) {
			if (ctx.align == null)
				ctx.align = false;
			state.indented = stream.indentation();
			state.startOfLine = true;
		}
		if (stream.eatSpace()) {
			return "";
		}
		state.curPunc = "";
		String style;
		if (state.tokenize != null) {
			style = state.tokenize.process(stream, state);
		} else {
			style = tokenBase(stream, state);
		}
		if (style.equals("comment") || style.equals("meta")) {
			return style;
		}
		if (ctx.align == false) {
			ctx.align = true;
		}
		if ((state.curPunc.equals(";") || state.curPunc.equals(":") || state.curPunc.equals(","))
				&& ctx.type.equals("statement")) {
			popContext(state);
		} else if (state.curPunc.equals("{")) {
			pushContext(state, stream.column(), "}");
		} else if (state.curPunc.equals("[")) {
			pushContext(state, stream.column(), "]");
		} else if (state.curPunc.equals("(")) {
			pushContext(state, stream.column(), ")");
		} else if (state.curPunc.equals("}")) {
			while (ctx.type.equals("statement")) {
				ctx = popContext(state);
			}
			if (ctx.type.equals("}")) {
				ctx = popContext(state);
			}
			while (ctx.type.equals("statement")) {
				ctx = popContext(state);
			}
		} else if (state.curPunc.equals(ctx.type)) {
			popContext(state);
		} else if (((ctx.type.equals("}") || ctx.type.equals("top")) && !state.curPunc.equals(";"))
				|| (ctx.type.equals("statement") && state.curPunc.equals("newstatement"))) {
			pushContext(state, stream.column(), "statement");
		}
		state.startOfLine = false;
		return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-d");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("d");
	}
}
