package io.onedev.commons.jsyntax.go;

import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class GoTokenizer extends AbstractTokenizer<GoTokenizer.State> {
	
	static Set<String> keywords = Sets.newHashSet("break", "case", "chan", "const", "continue",
		    "default", "defer", "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
		    "interface", "map", "package", "range", "return", "select", "struct", "switch", "type", "var",
		    "bool", "byte", "complex64", "complex128", "float32", "float64", "int8", "int16", "int32",
		    "int64", "string", "uint8", "uint16", "uint32", "uint64", "int", "uint", "uintptr", "error");
	static Set<String> atoms = Sets.newHashSet("true", "false", "iota", "nil", "append",
		    "cap", "close", "complex", "copy", "imag", "len", "make", "new", "panic", "print",
		    "println", "real", "recover");
	
	static Pattern isOperatorChar = Pattern.compile("[+\\-*&^%:=<>!|\\/]");
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class Context {
		Context prev;
		String type;
		int indented;
		int column;
		boolean align;
		
		Context(Context prev, String type, int indented, int column, boolean align) {
			this.prev = prev;
			this.type = type;
			this.indented = indented;
			this.column = column;
			this.align = align;
		}
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
			this.curPunc = "";
			this.indented = indented;
			this.startOfLine = startOfLine;
		}
	}
	
	static Context pushContext(State state, int col, String type) {
		return state.context = new Context(state.context, type, state.indented, col, false);
	}
	
	static Context popContext(State state) {
		if (state.context.prev == null)
			return null;
		String t = state.context.type;
		if (")]}".indexOf(t) != -1)
			state.indented = state.context.indented;
		return state.context = state.context.prev;
	}
	
	static Pattern number = Pattern.compile("[\\d\\.]");
	static Pattern scientific = Pattern.compile("^[0-9]+([eE][\\-+]?[0-9]+)?");
	static Pattern hex = Pattern.compile("^[xX][0-9a-fA-F]+");
	static Pattern oct = Pattern.compile("^0[0-7]+");
	static Pattern otherNum = Pattern.compile("^[0-9]*\\.?[0-9]*([eE][\\-+]?[0-9]+)?");
	static Pattern brace = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.]");
	static Pattern allWord = Pattern.compile("[\\w\\$_\\xa1-\\uffff]");
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			String ch = stream.next();
			if ("\"'`".indexOf(ch) != -1) {
				state.tokenize = new TokenString(ch);
				return state.tokenize.process(stream, state);
			}
			if (number.matcher(ch).matches()) {
				if (ch.equals(".")) {
					stream.match(scientific);
				} else if (ch.equals("0")) {
					if (stream.match(hex).isEmpty())
						stream.match(oct);
				}
				else {
					stream.match(otherNum);
				}
				return "number";
			}
			if (brace.matcher(ch).matches()) {
				state.curPunc = ch;
				return "";
			}
			if (ch.equals("/")) {
				if (!stream.eat("*").isEmpty()) {
					state.tokenize = new TokenComment();
					return state.tokenize.process(stream, state);
				}
				if (!stream.eat("/").isEmpty()) {
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
				if (cur.equals("case") || cur.equals("default"))
					state.curPunc = "case";
				return "keyword";
			}
			if (atoms.contains(cur))
				return "atom";
			return "variable";
		}
	}
	
	class TokenString implements Processor {
		String quote;
		
		TokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state) {
			boolean escaped = false, end = false;
			String next;
			while (!(next = stream.next()).isEmpty()) {
				if (next.equals(quote) && !escaped) {
					end = true;
					break;
				}
				escaped = !escaped && !quote.equals("`") && next.equals("\\");
			}
			if (end || !(escaped || quote.equals("`")))
				state.tokenize = new TokenBase();
			return "string";
		}
	}
	
	class TokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while (!(ch = stream.next()).equals("")) {
				if (ch.equals("/") && maybeEnd) {
					state.tokenize = new TokenBase();
					break;
				}
				maybeEnd = ch.equals("*");
			}
			return "comment";
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "go");
	}

	@Override
	public State startState() {
		return new State(null, new Context(null, "top", -indentUnit(), 0, false), 0, true);
	}

	@Override
	public String token(StringStream stream, State state) {
		Context ctx = state.context;
		if (stream.sol()) {
			state.indented = stream.indentation();
			state.startOfLine = true;
			if (ctx.type.equals("case"))
				ctx.type = "}";
		}
		if (stream.eatSpace())
			return "";
		state.curPunc = "";
		String style = (state.tokenize == null ? new TokenBase() : state.tokenize).process(stream, state);
		if (style.equals("comment"))
			return style;
		if (ctx.align == false)
			ctx.align = true;
		
		if (state.curPunc.equals("{"))
			pushContext(state, stream.column(), "}");
		else if (state.curPunc.equals("["))
			pushContext(state, stream.column(), "]");
		else if (state.curPunc.equals("("))
			pushContext(state, stream.column(), ")");
		else if (state.curPunc.equals("case"))
			ctx.type = "case";
		else if (state.curPunc.equals("}") && ctx.type.equals("}"))
			ctx = popContext(state);
		else if (state.curPunc.equals(ctx.type))
			popContext(state);
		state.startOfLine = false;
		return style;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-go");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("go");
	}
}
