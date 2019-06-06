package io.onedev.commons.jsyntax.swift;

import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class SwiftTokenizer extends AbstractTokenizer<SwiftTokenizer.State> {
	
	static Set<String> keywords = Sets.newHashSet("_","var","let","class","enum","extension","import","protocol","struct","func","typealias","associatedtype",
            "open","public","internal","fileprivate","private","deinit","init","new","override","self","subscript","super",
            "convenience","dynamic","final","indirect","lazy","required","static","unowned","unowned(safe)","unowned(unsafe)","weak","as","is",
            "break","case","continue","default","else","fallthrough","for","guard","if","in","repeat","switch","where","while",
            "defer","return","inout","mutating","nonmutating","catch","do","rethrows","throw","throws","try","didSet","get","set","willSet",
            "assignment","associativity","infix","left","none","operator","postfix","precedence","precedencegroup","prefix","right",
            "Any","AnyObject","Type","dynamicType","Self","Protocol","__COLUMN__","__FILE__","__FUNCTION__","__LINE__");
	static Set<String> definingKeywords = Sets.newHashSet("var","let","class","enum","extension","import","protocol","struct","func","typealias","associatedtype","for");
	static Set<String> atoms = Sets.newHashSet("true","false","nil","self","super","_");
	static Set<String> types = Sets.newHashSet("Array","Bool","Character","Dictionary","Double","Float","Int","Int8","Int16","Int32","Int64","Never","Optional","Set","String",
			"UInt8","UInt16","UInt32","UInt64","Void");
	
	static String operators = "+-/*%=|&<>~^?!";
	static String punc = ":;,.(){}[]";
	
	static Pattern binary = Pattern.compile("^\\-?0b[01][01_]*");
	static Pattern octal = Pattern.compile("^\\-?0o[0-7][0-7_]*");
	static Pattern hexadecimal = Pattern.compile("^\\-?0x[\\dA-Fa-f][\\dA-Fa-f_]*(?:(?:\\.[\\dA-Fa-f][\\dA-Fa-f_]*)?[Pp]\\-?\\d[\\d_]*)?");
	static Pattern decimal = Pattern.compile("^\\-?\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:[Ee]\\-?\\d[\\d_]*)?");
	static Pattern identifier = Pattern.compile("^\\$\\d+|(`?)[_A-Za-z][_A-Za-z$0-9]*\\1");
	static Pattern property = Pattern.compile("^\\.(?:\\$\\d+|(`?)[_A-Za-z][_A-Za-z$0-9]*\\1)");
	static Pattern instruction = Pattern.compile("^\\#[A-Za-z]+");
	static Pattern attribute = Pattern.compile("^@(?:\\$\\d+|(`?)[_A-Za-z][_A-Za-z$0-9]*\\1)");

	static interface Processor {
		String process(StringStream stream, State state, String prev);
	}
	
	static class Context {
		Context prev;
		int align;
		int indented;
		
		Context(Context prev, int align, int indented) {
			this.prev = prev;
			this.align = align;
			this.indented = indented;
		}
	}
	
	static class State {
		Stack<Processor> tokenize;
		Context context;
		String prev;
		int indented;
		
		State(Stack<Processor> tokenize, Context context, String prev, int indented) {
			this.tokenize = tokenize;
			this.context = context;
			this.prev = prev;
			this.indented = indented;
		}
	}
	
	static void popContext(State state) {
		if (state.context != null) {
			state.indented = state.context.indented;
			state.context = state.context.prev;
		}
	}
	
	static Pattern patternForPush = Pattern.compile("^\\s*($|\\/[\\/\\*])");
	
	static void pushContext(State state, StringStream stream) {
		int align = !stream.match(patternForPush, false).isEmpty() ? 0 : stream.column() + 1;
		state.context = new Context(state.context, align, state.indented);
	}
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state, String prev) {
			if (stream.sol())
				state.indented = stream.indentation();
			if (stream.eatSpace())
				return "";
			
			String ch = stream.peek();
			if (ch.equals("/")) {
				if (stream.match("//")) {
					stream.skipToEnd();
					return "comment";
				}
				if (stream.match("/*")) {
					state.tokenize.push(new TokenComment());
					return state.tokenize.peek().process(stream, state, prev);
				}
			}
			if (!stream.match(instruction).isEmpty())
				return "builtin";
			if (!stream.match(attribute).isEmpty())
				return "attribute";
			if (!stream.match(binary).isEmpty())
				return "number";
			if (!stream.match(octal).isEmpty())
				return "number";
			if (!stream.match(hexadecimal).isEmpty())
				return "number";
			if (!stream.match(decimal).isEmpty())
				return "number";
			if (!stream.match(property).isEmpty())
				return "property";
			if (operators.indexOf(ch) != -1) {
				stream.next();
				return "operator";
			}
			if (punc.indexOf(ch) != -1) {
				stream.next();
				stream.match("..");
				return "punctuation";
			}
			if (ch.equals("\"") || ch.equals("'")) {
				stream.next();
				Processor tokenize = new TokenString(ch);
				state.tokenize.push(tokenize);
				return tokenize.process(stream, state, prev);
			}
			
			if (!stream.match(identifier).isEmpty()) {
				String ident = stream.current();
				if (types.contains(ident))
					return "variable-2";
				if (atoms.contains(ident))
					return "atom";
				if (keywords.contains(ident)) {
					if (definingKeywords.contains(ident))
						state.prev = "define";
					return "keyword";
				}
				if (prev.equals("define"))
					return "def";
				return "variable";
			}
			
			stream.next();
			return "";
		}
	}
	
	class TokenUntilClosingParen implements Processor {
		int depth = 0;

		@Override
		public String process(StringStream stream, State state, String prev) {
			String inner = new TokenBase().process(stream, state, prev);
			if (inner.equals("punctuation")) {
				if (stream.current().equals("("))
					++depth;
				else if (stream.current().equals(")")) {
					if (depth == 0) {
						stream.backUp(1);
						state.tokenize.pop();
						return state.tokenize.peek().process(stream, state, prev);
					} else {
						--depth;
					}
				}
			}
			return inner;
		}
	}
	
	class TokenString implements Processor {
		String quote;
		
		TokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state, String prev) {
			String ch;
			boolean escaped = false;
			while (!(ch = stream.next()).equals("")) {
				if (escaped) {
					if (ch.equals("(")) {
						state.tokenize.push(new TokenUntilClosingParen());
						return "string";
					}
					escaped = false;
				} else if (ch.equals(quote)) {
					break;
				} else {
					escaped = ch.equals("\\");
				}
			}
			state.tokenize.pop();
			return "string";
		}
	}
	
	static Pattern patternForTokenComment = Pattern.compile("^(?:[^*]|\\*(?!\\/))*");
	
	class TokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state, String prev) {
			stream.match(patternForTokenComment);
			if (stream.match("*/"))
				state.tokenize.pop();
			return "comment";
		}
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "swift");
	}

	@Override
	public State startState() {
		return new State(new Stack<Processor>(), null, "", 0);
	}

	@Override
	public String token(StringStream stream, State state) {
		String prev = state.prev;
		state.prev = "";
		Processor tokenize = state.tokenize.empty() ? new TokenBase() : state.tokenize.peek();
		String style = tokenize.process(stream, state, prev);
		if (style.equals("") || style.equals("comment"))
			state.prev = prev;
		else if (state.prev.equals(""))
			state.prev = style;
		
		if (style.equals("punctuation")) {
			if ("([{".indexOf(stream.current()) != -1)
				pushContext(state, stream);
			else if (")]}".indexOf(stream.current()) != -1)
				popContext(state);
		}
		return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-swift");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("swift");
	}
}
