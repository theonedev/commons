package io.onedev.commons.jsyntax.ruby;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class RubyTokenizer extends AbstractTokenizer<RubyTokenizer.State> {
	
	static Map<String, String> matching = new HashMap<String, String>();
	static Set<String> keywords = Sets.newHashSet(
			"alias", "and", "BEGIN", "begin", "break", "case", "class", "def", "defined?", "do", "else",
			"elsif", "END", "end", "ensure", "false", "for", "if", "in", "module", "next", "not", "or",
			"redo", "rescue", "retry", "return", "self", "super", "then", "true", "undef", "unless",
			"until", "when", "while", "yield", "nil", "raise", "throw", "catch", "fail", "loop", "callcc",
			"caller", "lambda", "proc", "public", "protected", "private", "require", "load",
			"require_relative", "extend", "autoload", "__END__", "__FILE__", "__LINE__", "__dir__");
	static Set<String> indentWords = Sets.newHashSet(
			"def", "class", "case", "for", "while", "until", "module", "then",
            "catch", "loop", "proc", "begin");
	static Set<String> dedentWords = Sets.newHashSet("end", "until");
	
	static {
		matching.put("{", "}");
		matching.put("[", "]");
		matching.put("(", ")");
	}
	
	static class Context {
		Context prev;
		String type;
		int indented;
		
		Context(Context prev, String type, int indented) {
			this.prev = prev;
			this.type = type;
			this.indented = indented;
		}
	}
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	public static class State {
		Stack<Processor> tokenize;
		Context context;
		String lastTok;
		String curPunc;
		int indented;
		boolean continuedLine;
		boolean varList;

		State(Stack<Processor> tokenize, Context context, String lastTok, int indented, boolean continuedLine, boolean varList) {
			this.tokenize = tokenize;
			this.context = context;
			this.lastTok = lastTok;
			this.curPunc = "";
			this.indented = indented;
			this.continuedLine = continuedLine;
			this.varList = varList;
		}
		
		public Stack<Processor> getTokenize() {
			return tokenize;
		}
		
		public boolean isContextPrevNull() {
			return context.prev == null;
		}
	}
	
	String chain(Processor newtok, StringStream stream, State state) {
		state.tokenize.push(newtok);
		return newtok.process(stream, state);
	}
	
	class ReadBlockComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (stream.sol() && stream.match("=end") && stream.eol())
				state.tokenize.pop();
			stream.skipToEnd();
			return "comment";
		}
	}
	
	class ReadHereDoc implements Processor {
		String phrase;
		
		public ReadHereDoc(String phrase) {
			this.phrase = phrase;
		}

		@Override
		public String process(StringStream stream, State state) {
			if (stream.match(phrase))
				state.tokenize.pop();
			else
				stream.skipToEnd();
			return "string";
		}
	}
	
	class TokenBaseOnce implements Processor {
		boolean alreadyCalled = false;

		@Override
		public String process(StringStream stream, State state) {
			if (alreadyCalled) {
				state.tokenize.pop();
				return state.tokenize.peek().process(stream, state);
			}
			alreadyCalled = true;
			return new TokenBase().process(stream, state);
		}
	}
	
	class TokenBaseUntilBrace implements Processor {
		int depth;
		
		public TokenBaseUntilBrace(int depth) {
			this.depth = depth;
		}

		@Override
		public String process(StringStream stream, State state) {
			if (stream.peek().equals("}")) {
				if (depth == 1) {
					state.tokenize.pop();
					return state.tokenize.peek().process(stream, state);
				} else {
					state.tokenize.pop();
					state.tokenize.push(new TokenBaseUntilBrace(depth - 1));
				}
			} else if (stream.peek().equals("{")) {
				state.tokenize.pop();
				state.tokenize.push(new TokenBaseUntilBrace(depth + 1));
			}
			return new TokenBase().process(stream, state);
		}
	}
	
	static Pattern patternForReadQuoted = Pattern.compile("[@\\$]");
	
	class ReadQuoted implements Processor {
		String quote, style;
		boolean embed, unescaped;
		
		public ReadQuoted(String quote, String style, boolean embed, boolean unescaped) {
			this.quote = quote;
			this.style = style;
			this.embed = embed;
			this.unescaped = unescaped;
		}

		@Override
		public String process(StringStream stream, State state) {
			boolean escaped = false;
			String ch;
			
			if (state.context.type.equals("read-quoted-paused")) {
				state.context = state.context.prev;
				stream.eat("}");
			}
			
			while (!(ch = stream.next()).isEmpty()) {
				if (ch.equals(quote) && (unescaped || !escaped)) {
					state.tokenize.pop();
					break;
				}
				if (embed && ch.equals("#") && !escaped) {
					if (!stream.eat("{").isEmpty()) {
						if (quote.equals("}"))
							state.context = new Context(state.context, "read-quoted-paused", 0);
						state.tokenize.push(new TokenBaseUntilBrace(1));
						break;
					} else if (patternForReadQuoted.matcher(stream.peek()).matches()) {
						state.tokenize.push(new TokenBaseOnce());
						break;
					}
				}
				escaped = !escaped && ch.equals("\\");
			}
			return style;
		}
	}
	
	static Pattern[] pattern = new Pattern[29];
	
	static {
		pattern[1] = Pattern.compile("[WQ]");
		pattern[2] = Pattern.compile("[r]");
		pattern[3] = Pattern.compile("[wxq]");
		pattern[4] = Pattern.compile("[^\\w\\s=]");
		pattern[5] = Pattern.compile("^<-?[\\`\\\"\\']?([a-zA-Z_?]\\w*)[\\`\\\"\\']?(?:;|$)");
		pattern[6] = Pattern.compile("[\\da-fA-F]");
		pattern[7] = Pattern.compile("[01]");
		pattern[8] = Pattern.compile("[0-7]");
		pattern[9] = Pattern.compile("\\d");
		pattern[10] = Pattern.compile("^[\\d_]*(?:\\.[\\d_]+)?(?:[eE][+\\-]?[\\d_]+)?");
		pattern[11] = Pattern.compile("^\\\\[CM]-");
		pattern[12] = Pattern.compile("\\w");
		pattern[13] = Pattern.compile("[\\<\\>]");
		pattern[14] = Pattern.compile("[\\+\\-\\*\\/\\&\\|\\:\\!]");
		pattern[15] = Pattern.compile("[a-zA-Z$@_\\xa1-\\uffff]");
		pattern[16] = Pattern.compile("[\\w$\\xa1-\\uffff]");
		pattern[17] = Pattern.compile("[\\?\\!\\=]");
		pattern[18] = Pattern.compile("^@?[a-zA-Z_\\xa1-\\uffff]");
		pattern[19] = Pattern.compile("[\\w\\xa1-\\uffff]");
		pattern[20] = Pattern.compile("[a-zA-Z_]");
		pattern[21] = Pattern.compile("[\\w]");
		pattern[22] = Pattern.compile("\\d");
		pattern[23] = Pattern.compile("[a-zA-Z_\\xa1-\\uffff]");
		pattern[24] = Pattern.compile("[\\w\\xa1-\\uffff]");
		pattern[25] = Pattern.compile("[\\?\\!]");
		pattern[26] = Pattern.compile("[\\(\\)\\[\\]{}\\\\;]");
		pattern[27] = Pattern.compile("[=+\\-\\/*:\\.^%<>~|]");
		pattern[28] = Pattern.compile("[=+\\-\\/*:\\.^%<>~|]");
	}
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (stream.sol() && stream.match("=begin") && stream.eol()) {
				state.tokenize.push(new ReadBlockComment());
				return "comment";
			}
			
			if (stream.eatSpace())
				return "";
			
			String ch = stream.next();
			List<String> m;
			if (ch.equals("`") || ch.equals("\'") || ch.equals("\"")) {
				return chain(new ReadQuoted(ch, "string", ch.equals("\"") || ch.equals("`"), false), stream, state);
			} else if (ch.equals("/")) {
				int currentIndex = stream.current().length();
				if (stream.skipTo("/")) {
					int search_till = stream.current().length();
					stream.backUp(stream.current().length() - currentIndex);
					int balance = 0;
					while (stream.current().length() < search_till) {
						String chchr = stream.next();
						if (chchr.equals("("))
							balance++;
						else if (chchr.equals(""))
							balance--;
						if (balance < 0)
							break;
					}
					stream.backUp(stream.current().length() - currentIndex);
					if (balance == 0)
						return chain(new ReadQuoted(ch, "string-2", true, false), stream, state);
				}
				return "operator";
			} else if (ch.equals("%")) {
				String style = "string";
				boolean embed = true;
				if (!stream.eat("s").isEmpty())
					style = "atom";
				else if (!stream.eat(pattern[1]).isEmpty())
					style = "string";
				else if (!stream.eat(pattern[2]).isEmpty())
					style = "string-2";
				else if (!stream.eat(pattern[3]).isEmpty()) {
					style = "string";
					embed = false;
				}
				String delim = stream.eat(pattern[4]);
				if (delim.isEmpty())
					return "operator";
				if (matching.containsKey(delim))
					delim = matching.get(delim);
				return chain(new ReadQuoted(delim, style, embed, true), stream, state);
			} else if (ch.equals("#")) {
				stream.skipToEnd();
				return "comment";
			} else if (ch.equals("<") && !(m = stream.match(pattern[5])).isEmpty()) {
				return chain(new ReadHereDoc(m.get(1)), stream, state);
			} else if (ch.equals("0")) {
				if (!stream.eat("x").isEmpty())
					stream.eatWhile(pattern[6]);
				else if (!stream.eat("b").isEmpty())
					stream.eatWhile(pattern[7]);
				else
					stream.eatWhile(pattern[8]);
				return "number";
			} else if (pattern[9].matcher(ch).matches()) {
				stream.match(pattern[10]);
				return "number";
			} else if (ch.equals("?")) {
				while (!stream.match(pattern[11]).isEmpty());
				if (!stream.eat("\\").isEmpty())
					stream.eatWhile(pattern[12]);
				else
					stream.next();
				return "string";
			} else if (ch.equals(":")) {
				if (!stream.eat("'").isEmpty())
					return chain(new ReadQuoted("'", "atom", false, false), stream, state);
				if (!stream.eat("\"").isEmpty())
					return chain(new ReadQuoted("\"", "atom", true, false), stream, state);

				// :> :>> :< :<< are valid symbols
				if (!stream.eat(pattern[13]).isEmpty()) {
					stream.eat(pattern[13]);
					return "atom";
				}

				// :+ :- :/ :* :| :& :! are valid symbols
				if (!stream.eat(pattern[14]).isEmpty())
					return "atom";

				// Symbols can't start by a digit
				if (!stream.eat(pattern[15]).isEmpty()) {
			        stream.eatWhile(pattern[16]);
			        // Only one ? ! = is allowed and only as the last character
			        stream.eat(pattern[17]);
			        return "atom";
				}
				return "operator";
			} else if (ch.equals("@") && !stream.match(pattern[18]).isEmpty()) {
				stream.eat("@");
				stream.eatWhile(pattern[19]);
				return "variable-2";
			} else if (ch.equals("$")) {
				if (!stream.eat(pattern[20]).isEmpty())
					stream.eatWhile(pattern[21]);
				else if (!stream.eat(pattern[22]).isEmpty())
					stream.eat(pattern[22]);
				else
					stream.next();
				return "variable-3";
			} else if (pattern[23].matcher(ch).matches()) {
				stream.eatWhile(pattern[24]);
				stream.eat(pattern[25]);
				if (!stream.eat(":").isEmpty())
					return "atom";
				return "ident";
			} else if (ch.equals("|") && (state.varList || state.lastTok.equals("{") || state.lastTok.equals("do"))) {
				state.curPunc = "|";
				return "";
			} else if (pattern[26].matcher(ch).matches()) {
				state.curPunc = ch;
				return "";
			} else if (ch.equals("-") && !stream.eat(">").isEmpty()) {
				return "arrow";
			} else if (pattern[27].matcher(ch).matches()) {
				boolean more = stream.eatWhile(pattern[28]);
				if (ch.equals(".") && !more)
					state.curPunc = ".";
				return "operator";
			} else {
				return "";
			}
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "rb");
	}

	@Override
	public State startState() {
		Stack<Processor> tokenize = new Stack<Processor>();
		tokenize.push(new TokenBase());
		return new State(tokenize, new Context(null, "top", -indentUnit()), "", 0, false, false);
	}
	
	static Pattern leftBrace = Pattern.compile("[\\(\\[\\{]");
	static Pattern rightBrace = Pattern.compile("[\\)\\]\\}]");

	@Override
	public String token(StringStream stream, State state) {
		state.curPunc = "";
		if (stream.sol())
			state.indented = stream.indentation();
		String style = state.tokenize.peek().process(stream, state), kwtype = "";
		String thisTok = state.curPunc;
		if (style.equals("ident")) {
			String word = stream.current();
			style = state.lastTok.equals(".") ? "property"
					: keywords.contains(stream.current()) ? "keyword"
					: Character.isUpperCase(word.charAt(0)) ? "tag"
					: (state.lastTok.equals("def") || state.lastTok.equals("class") || state.varList) ? "def"
					: "variable";
			if (style.equals("keyword")) {
				thisTok = word;
				if (indentWords.contains(word))
					kwtype = "indent";
				else if (dedentWords.contains(word))
					kwtype = "dedent";
				else if ((word.equals("if") || word.equals("unless")) && stream.column() == stream.indentation())
					kwtype = "indent";
				else if (word.equals("do") && state.context.indented < state.indented)
					kwtype = "indent";
			}
		}
		if (!state.curPunc.isEmpty() || (!style.isEmpty() && !style.equals("comment")))
			state.lastTok = thisTok;
		if (state.curPunc.equals("|"))
			state.varList = !state.varList;
		
		if (kwtype.equals("indent") || leftBrace.matcher(state.curPunc).matches())
			state.context = new Context(state.context, state.curPunc.isEmpty() ? style : state.curPunc, state.indented);
		else if ((kwtype.equals("dedent") || rightBrace.matcher(state.curPunc).matches()) && state.context.prev != null)
			state.context = state.context.prev;
		
		if (stream.eol())
			state.continuedLine = (state.curPunc.equals("\\") || style.equals("operator"));
		return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-ruby");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("ruby");
	}
}
