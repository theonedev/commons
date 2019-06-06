package io.onedev.commons.jsyntax.pascal;

import java.util.Set;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class PascalTokenizer extends AbstractTokenizer<PascalTokenizer.State> {

	static Set<String> keywords;
	static Set<String> atoms;
	static Pattern isOperatorChar;
	
	static {
		keywords = wordsOf("and array begin case const div do downto else end file for forward integer " +
                "boolean char function goto if in label mod nil not of or packed procedure " +
                "program record repeat set string then to type until var while with");
		atoms = wordsOf("null");
		isOperatorChar = Pattern.compile("[+\\-*&%=<>!?|\\/]");
	}
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class State {
		Processor tokenize;
		boolean startOfLine;
		
		public State(Processor tokenize) {
			this.tokenize = tokenize;
			this.startOfLine = false;
		}
	}
	
	static Pattern pattern[] = new Pattern[4];
	
	static {
	    pattern[0] = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.]");
	    pattern[1] = Pattern.compile("\\d");
	    pattern[2] = Pattern.compile("[\\w\\.]");
	    pattern[3] = Pattern.compile("[\\w\\$_]");
	}
	
	String tokenBase(StringStream stream, State state) {
	    String ch = stream.next();
	    if (ch.equals("#") && state.startOfLine) {
	        stream.skipToEnd();
	        return "meta";
	    }
	    if (ch.equals("\"") || ch.equals("'")) {
	        state.tokenize = new TokenString(ch);
	        return state.tokenize.process(stream, state);
	    }
	    if (ch.equals("(") &&!stream.eat("*").isEmpty()) {
	        state.tokenize = new TokenComment();
	        return state.tokenize.process(stream, state);
	    }
	    if (pattern[0].matcher(ch).matches()) {
	        return "";
	    }
	    if (pattern[1].matcher(ch).matches()) {
	        stream.eatWhile(pattern[2]);
	        return "number";
	    }
	    if (ch.equals("/")) {
	        if (!stream.eat("/").isEmpty()) {
	            stream.skipToEnd();
	            return "comment";
	        }
	    }
	    if (isOperatorChar.matcher(ch).matches()) {
	        stream.eatWhile(isOperatorChar);
	        return "operator";
	    }
	    stream.eatWhile(pattern[3]);
	    Object cur = stream.current();
	    if (keywords.contains(cur)) return "keyword";
	    if (atoms.contains(cur)) return "atom";
	    return "variable";
	}

	class TokenString implements Processor {
		String quote;
		
		public TokenString(String quote) {
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
	            escaped = !escaped && next.equals("\\");
	        }
	        if (end || !escaped) state.tokenize = null;
	        return "string";
		}
	}
	
	class TokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		    boolean maybeEnd = false;
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (ch.equals(")") && maybeEnd) {
		            state.tokenize = null;
		            break;
		        }
		        maybeEnd = (ch.equals("*"));
		    }
		    return "comment";
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "p", "pas");
	}

	@Override
	public State startState() {
		return new State(null);
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (stream.eatSpace()) return "";
	    String style;
	    if (state.tokenize == null)
	    	style = tokenBase(stream, state);
	    else
	    	style = state.tokenize.process(stream, state);
	    if (style.equals("comment") || style.equals("meta")) return style;
	    return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-pascal");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("pascal");
	}
}
