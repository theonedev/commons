package io.onedev.commons.jsyntax.clike;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.TokenizerUtils;

public abstract class ClikeTokenizer extends AbstractTokenizer<ClikeTokenizer.State> {

	private static Pattern IS_OPERATOR_CHAR = Pattern.compile("[+\\-*&%=<>!?|\\/]");
	
	private static Pattern IS_PUNCTUATION_CHAR = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.]");
	
	private static Pattern NUMBER_START = Pattern.compile("[\\d\\.]");

	private static Pattern NUMBER = Pattern.compile("^(?:0x[a-f\\d]+|0b[01]+|(?:\\d+\\.?\\d*|\\.\\d+)(?:e[-+]?\\d+)?)(u|ll?|l|f)?", Pattern.CASE_INSENSITIVE);
	
	private static Pattern TYPE_BEFORE = Pattern.compile("\\S(?:[^- ]>|[*\\]])\\s*$|\\*$");
	
	static class Pair {
		String ch;
		int v;
		
		public Pair(String ch, int v) {
			this.ch = ch;
			this.v = v;
		}
	}

	public static class State {
		public Processor tokenize;
		public Context context;
		public int indented;
		public boolean startOfLine;
		public String curPunc;
		public boolean isDefKeyword;
		public String cpp11RawStringDelim;
		public String prevToken;
		public boolean typeAtEndOfLine;	
		public Processor stringTokenizer;
		public Stack<Pair> tokStack;
		public Stack<Processor> interpolationStack;

		State(Processor tokenize, Context context, int indented, boolean startOfLine, String prevToken) {
			this.tokenize = tokenize;
			this.context = context;
			this.indented = indented;
			this.startOfLine = startOfLine;
			this.prevToken = prevToken;
			this.tokStack = new Stack<Pair>();
			this.interpolationStack = new Stack<Processor>();
		}
	}

	static class Context {
		int indented;
		int column;
		String type;
		String info;
		Boolean align;
		Context prev;

		Context(int indented, int column, String type, String info, Boolean align, Context prev) {
			this.indented = indented;
			this.column = column;
			this.type = type;
			this.info = info;
			this.align = align;
			this.prev = prev;
		}

	}

	@FunctionalInterface
	public static interface Processor {
		String process(StringStream stream, State state, String style);
	}

	private Context pushContext(State state, int col, String type, String info) {
		int indent = state.indented;
		if (state.context != null && state.context.type.equals("statement") && !type.equals("statement"))
			indent = state.context.indented;
		return state.context = new Context(indent, col, type, info, null, state.context);
	}

	private Context popContext(State state) {
		String t = state.context.type;
		if (t.equals(")") || t.equals("]") || t.equals("}"))
			state.indented = state.context.indented;
		return state.context = state.context.prev;
	}

	private boolean typeBefore(StringStream stream, State state, int pos) {
		  if (state.prevToken.equals("variable") || state.prevToken.equals("variable-3")) 
			  return true;
		  if (TYPE_BEFORE.matcher(TokenizerUtils.slice(stream.string(), 0, pos)).matches()) 
			  return true;
		  if (state.typeAtEndOfLine && stream.column() == stream.indentation()) 
			  return true;
		  return false;
	}
	
	private boolean isTopScope(Context context) {
		for (;;) {
			if (context == null || context.type.equals("top")) 
				return true;
			if (context.type.equals("}") && !context.prev.info.equals("namespace")) 
				return false;
		    context = context.prev;
		}
	}

	protected int statementIndentUnit() {
		return indentUnit();
	}

	protected Set<String> keywords() {
		return new HashSet<>();
	}

	protected boolean typesContains(String word) {
		return false;
	}
	
	protected Set<String> defKeywords() {
		return new HashSet<>();
	}
	
	protected Set<String> builtin() {
		return new HashSet<>();
	}

	protected Set<String> blockKeywords() {
		return new HashSet<>();
	}

	protected Set<String> atoms() {
		return new HashSet<>();
	}

	protected Map<String, Processor> hooks() {
		return new HashMap<>();
	}

	protected boolean multiLineStrings() {
		return false;
	}

	protected boolean indentStatements() {
		return true;
	}

	protected boolean indentSwitch() {
		return true;
	}
	
	protected String namespaceSeparator() {
		return "";
	}
	
	protected boolean typeFirstDefinitions() {
		return false;
	}
	
	protected boolean styleDefs() {
		return true;
	}
	
	protected Pattern isPunctuationChar() {
		return IS_PUNCTUATION_CHAR;
	}
	
	protected Pattern isOperatorChar() {
		return IS_OPERATOR_CHAR;
	}
	
	protected Pattern numberStart() {
		return NUMBER_START;
	}
	
	protected Pattern number() {
		return NUMBER;
	}
	
	private static Pattern TOKEN_BASE_PATTERN1 = Pattern.compile("^\\/[\\/*]");

	private static Pattern TOKEN_BASE_PATTERN2 = Pattern.compile("[\\w\\$_\\xa1-\\uffff]");

	private String tokenBase(StringStream stream, State state) {
		String ch = stream.next();
		if (hooks().get(ch) != null) {
			String result = hooks().get(ch).process(stream, state, "");
			if (result.length() != 0)
				return result;
		}

		if (ch.equals("\"") || ch.equals(("'"))) {
			state.tokenize = new TokenString(ch);
			return state.tokenize.process(stream, state, "");
		}
		if (isPunctuationChar().matcher(ch).matches()) {
			state.curPunc = ch;
			return "";
		}
		if (numberStart().matcher(ch).matches()) {
			stream.backUp(1);
			if (!stream.match(number()).isEmpty())
				return "number";
			stream.next();
		}
		if (ch.equals("/")) {
			if (stream.eat("*").length() != 0) {
				state.tokenize = new TokenComment();
				return state.tokenize.process(stream, state, "");
			}
			if (stream.eat("/").length() != 0) {
				stream.skipToEnd();
				return "comment";
			}
		}
		if (isOperatorChar().matcher(ch).matches()) {
		    while (stream.match(TOKEN_BASE_PATTERN1, false).isEmpty() && stream.eat(isOperatorChar()).length() != 0) {
		    }
			return "operator";
		}
		stream.eatWhile(TOKEN_BASE_PATTERN2);
	    if (namespaceSeparator().length() != 0) {
	    	while (stream.match(namespaceSeparator()))
		        stream.eatWhile(TOKEN_BASE_PATTERN2);
	    }
		
		String cur = stream.current();
		if (keywords().contains(cur)) {
			if (blockKeywords().contains(cur))
				state.curPunc = "newstatement";
		    if (defKeywords().contains(cur)) 
		    	state.isDefKeyword = true;
			return "keyword";
		}
	    if (typesContains(cur)) 
	    	return "variable-3";
		if (builtin().contains(cur)) {
			if (blockKeywords().contains(cur))
				state.curPunc = "newstatement";
			return "builtin";
		}
		if (atoms().contains(cur))
			return "atom";
		return "variable";
	}

	class TokenString implements Processor {

		private String quote;

		TokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state, String style) {
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
			if (end || !(escaped || multiLineStrings()))
				state.tokenize = null;
			return "string";
		}

	}

	class TokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state, String style) {
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

	private void maybeEOL(StringStream stream, State state) {
	    if (typeFirstDefinitions() && stream.eol() && isTopScope(state.context))
	    	state.typeAtEndOfLine = typeBefore(stream, state, stream.pos());
	}
	 
	@Override
	public State startState() {
		return new State(null, new Context(-indentUnit(), 0, "top", "", false, null), 0, true, "");
	}

	private static final Pattern TOKEN_PATTERN1 = Pattern.compile("^\\s*(?:\\/\\/.*)?$");
	
	private static final Pattern TOKEN_PATTERN2 = Pattern.compile("^\\s*\\(");
	
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
			maybeEOL(stream, state);
			return "";
		}

		state.curPunc = "";
		state.isDefKeyword = false;

		String style;
		if (state.tokenize != null)
			style = state.tokenize.process(stream, state, "");
		else
			style = tokenBase(stream, state);

		if (style.equals("comment") || style.equals("meta"))
			return style;

		if (ctx.align == null)
			ctx.align = true;

	    if (state.curPunc.equals(";") || state.curPunc.equals(":") || (state.curPunc.equals(",") && !stream.match(TOKEN_PATTERN1, false).isEmpty()))
	        while (state.context.type.equals("statement")) popContext(state);
		else if (state.curPunc.equals("{"))
			pushContext(state, stream.column(), "}", "");
		else if (state.curPunc.equals("["))
			pushContext(state, stream.column(), "]", "");
		else if (state.curPunc.equals("("))
			pushContext(state, stream.column(), ")", "");
		else if (state.curPunc.equals("}")) {
			while (ctx.type.equals("statement"))
				ctx = popContext(state);
			if (ctx.type.equals("}"))
				ctx = popContext(state);
			while (ctx.type.equals("statement"))
				ctx = popContext(state);
		} else if (state.curPunc.equals(ctx.type))
			popContext(state);
		else if (indentStatements()
				&& (((ctx.type.equals("}") || ctx.type.equals("top")) && !state.curPunc
						.equals(';')) || (ctx.type.equals("statement") && state.curPunc
						.equals("newstatement")))) {
			pushContext(state, stream.column(), "statement", stream.current());
		}
	    if (style.equals("variable") 
	    		&& ((state.prevToken.equals("def") 
	    				|| (typeFirstDefinitions() 
	    						&& typeBefore(stream, state, stream.start()) 
	    						&& isTopScope(state.context) 
	    						&& !stream.match(TOKEN_PATTERN2, false).isEmpty()))))
	    	style = "def";

    	if (hooks().containsKey("token")) {
    		String result = hooks().get("token").process(stream, state, style);
    		if (result.length() != 0) style = result;
        }

        if (style.equals("def") && !styleDefs()) style = "variable";

        state.startOfLine = false;
        state.prevToken = state.isDefKeyword ? "def" : (style.length()!=0?style:state.curPunc);
        maybeEOL(stream, state);
        return style;
	}

	static Set<String> C_KEYWORDS = Sets.newHashSet("auto", "if", "break", "case", "register",
			"continue", "return", "default", "do", "sizeof", "static", "else", "struct", 
			"switch", "extern", "typedef", "union", "for", "goto", "while", "enum", "const", 
			"volatile");
	
	static Set<String> C_TYPES = Sets.newHashSet("int", "long", "char", "short", "double", "float", "unsigned", "signed", "void", "size_t", "ptrdiff_t");

	static class CppHook implements Processor {

		private static final Pattern PATTERN1 = Pattern.compile("^.$");
		
		private static final Pattern PATTERN2 = Pattern.compile("^\\/[\\/\\*]");
		
		@Override
		public String process(StringStream stream, State state, String style) {
		    if (!state.startOfLine) return "";
		    
		    Processor next = null;
		    String ch;
		    for (;(ch=stream.peek()).length()!=0;) {
		    	if (ch.equals("\\") && !stream.match(PATTERN1).isEmpty()) {
		    		next = new CppHook();
		    	    break;
		    	} else if (ch.equals("/") && !stream.match(PATTERN2, false).isEmpty()) {
		    		break;
		    	}
		    	stream.next();
		    }
		    state.tokenize = next;
		    return "meta";
		}

	}
	
	static class PointerHook implements Processor {

		@Override
		public String process(StringStream stream, State state, String style) {
		    if (state.prevToken.equals("variable-3")) return "variable-3";
		    return "";
		}
		
	}
	
	static class Cpp14Literal implements Processor {

		private static final Pattern PATTERN = Pattern.compile("[\\w\\.']");
		
		@Override
		public String process(StringStream stream, State state, String style) {
		    stream.eatWhile(PATTERN);
		    return "number";
		}
		
	}
	
	static class Cpp11StringHook implements Processor {

		private static Pattern PATTERN1 = Pattern.compile("(R|u8R|uR|UR|LR)");

		private static Pattern PATTERN2 = Pattern
				.compile("\"([^\\s\\\\()]{0,16})\\(");

		private static Pattern PATTERN3 = Pattern.compile("(u8|u|U|L)");

		private static Pattern PATTERN4 = Pattern.compile("[\"']");

		@Override
		public String process(StringStream stream, State state, String style) {
			stream.backUp(1);
			// Raw strings.
			if (!stream.match(PATTERN1).isEmpty()) {
				List<String> match = stream.match(PATTERN2);
				if (match.isEmpty()) {
					return "";
				}
				state.cpp11RawStringDelim = match.get(1);
				state.tokenize = new TokenRawString();
				return state.tokenize.process(stream, state, "");
			}
			// Unicode strings/chars.
			if (!stream.match(PATTERN3).isEmpty()) {
				if (!stream.match(PATTERN4, /* eat */false).isEmpty()) {
					return "string";
				}
				return "";
			}
			// Ignore this hook.
			stream.next();
			return "";
		}

	}
	
	private static final Pattern CPPLOOKSLIKECONSTRUCTOR_PATTERN = Pattern.compile("(\\w+)::(\\w+)$");
	
	static boolean cppLooksLikeConstructor(String word) {
		Matcher matcher = CPPLOOKSLIKECONSTRUCTOR_PATTERN.matcher(word);
		return matcher.find() && matcher.group(1).equals(matcher.group(2));
	}

	static class TokenAtString implements Processor {

		@Override
		public String process(StringStream stream, State state, String style) {
			String next;
			while ((next = stream.next()).length() != 0) {
				if (next.equals("\"") && stream.eat("\"").length() == 0) {
					state.tokenize = null;
					break;
				}
			}
			return "string";
		}

	}

	static class TokenRawString implements Processor {

		private static Pattern PATTERN1 = Pattern.compile("[^\\w\\s]");

		@Override
		public String process(StringStream stream, State state, String style) {
			// Escape characters that have special regex meanings.
			String delim = PATTERN1.matcher(state.cpp11RawStringDelim)
					.replaceAll("\\\\$0");
			if (!stream.match(Pattern.compile(".*?\\)" + delim + "\"")).isEmpty())
				state.tokenize = null;
			else
				stream.skipToEnd();
			return "string";
		}

	}

	static class TokenTripleString implements Processor {

		@Override
		public String process(StringStream stream, State state, String style) {
			boolean escaped = false;
			while (!stream.eol()) {
				if (!escaped && stream.match("\"\"\"")) {
					state.tokenize = null;
					break;
				}
				escaped = !stream.next().equals("\\") && !escaped;
			}
			return "string";
		}
	}
	
	static Processor tokenKotlinString(boolean tripleString) {
		
		return new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
				boolean escaped = false; 
				String next;
				boolean end = false;
			    while (!stream.eol()) {
			    	if (!tripleString && !escaped && stream.match("\"")) {end = true; break;}
			        if (tripleString && stream.match("\"\"\"")) {end = true; break;}
			        next = stream.next();
			        if(!escaped && next == "$" && stream.match("{"))
			        	stream.skipTo("}");
			        escaped = !escaped && next.equals("\\") && !tripleString;
			    }
			    if (end || !tripleString)
			    	state.tokenize = null;
			    return "string";
			}
			
		};
		
	}

	static Processor tokenCeylonString(String type) {
		
		return new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
				boolean escaped = false; 
				String next = "";
				boolean end = false;
				while (!stream.eol()) {
					if (!escaped && stream.match("\"") && (type.equals("single") || stream.match("\"\""))) {
						end = true;
						break;
			        }
			        if (!escaped && stream.match("``")) {
			        	state.stringTokenizer = tokenCeylonString(type);
			        	end = true;
			        	break;
			        }
			        next = stream.next();
			        escaped = type.equals("single") && !escaped && next.equals("\\");
				}
			    if (end)
			    	state.tokenize = null;
			    return "string";
			}
		};
	}
	
}