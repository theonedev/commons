package io.onedev.commons.jsyntax.mathematica;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class MathematicaTokenizer extends AbstractTokenizer<MathematicaTokenizer.State> {

	static String identifier = "[a-zA-Z\\$][a-zA-Z0-9\\$]*";
	static String pBase      = "(?:\\d+)";
	static String pFloat     = "(?:\\.\\d+|\\d+\\.\\d*|\\d+)";
	static String pFloatBase = "(?:\\.\\w+|\\w+\\.\\w*|\\w+)";
	static String pPrecision = "(?:`(?:`?" + pFloat + ")?)";
	
	static Pattern reBaseForm = Pattern.compile("(?:" + pBase + "(?:\\^\\^" + pFloatBase + pPrecision + "?(?:\\*\\^[+-]?\\d+)?))", Pattern.CASE_INSENSITIVE);
	static Pattern reFloatForm = Pattern.compile("(?:" + pFloat + pPrecision + "?(?:\\*\\^[+-]?\\d+)?)", Pattern.CASE_INSENSITIVE);
	static Pattern reIdInContext = Pattern.compile("(?:`?)(?:" + identifier + ")(?:`(?:" + identifier + "))*(?:`?)", Pattern.CASE_INSENSITIVE);

	static interface Processor {
	    String process(StringStream stream, State state);
	}
	
	static class State {
	    Processor tokenize;
	    int commentLevel;

	    public State(Processor tokenize, int commentLevel) {
	        this.tokenize = tokenize;
	        this.commentLevel = commentLevel;
	    }
	}
	
    static Pattern pattern[] = new Pattern[11];
    
    static {
	    pattern[0] = Pattern.compile("(?:In|Out)\\[[0-9]*\\]", Pattern.CASE_INSENSITIVE);
	    pattern[1] = Pattern.compile("([a-zA-Z\\$]+(?:`?[a-zA-Z0-9\\$])*::usage)", Pattern.CASE_INSENSITIVE);
	    pattern[2] = Pattern.compile("([a-zA-Z\\$]+(?:`?[a-zA-Z0-9\\$])*::[a-zA-Z\\$][a-zA-Z0-9\\$]*):?", Pattern.CASE_INSENSITIVE);
	    pattern[3] = Pattern.compile("([a-zA-Z\\$][a-zA-Z0-9\\$]*\\s*:)(?:(?:[a-zA-Z\\$][a-zA-Z0-9\\$]*)|(?:[^:=>~@\\^\\&\\*\\)\\[\\]'\\?,\\|])).*", Pattern.CASE_INSENSITIVE);
	    pattern[4] = Pattern.compile("[a-zA-Z\\$][a-zA-Z0-9\\$]*_+[a-zA-Z\\$][a-zA-Z0-9\\$]*", Pattern.CASE_INSENSITIVE);
	    pattern[5] = Pattern.compile("[a-zA-Z\\$][a-zA-Z0-9\\$]*_+", Pattern.CASE_INSENSITIVE);
	    pattern[6] = Pattern.compile("_+[a-zA-Z\\$][a-zA-Z0-9\\$]*", Pattern.CASE_INSENSITIVE);
	    pattern[7] = Pattern.compile("\\\\\\[[a-zA-Z\\$][a-zA-Z0-9\\$]*\\]", Pattern.CASE_INSENSITIVE);
	    pattern[8] = Pattern.compile("(?:\\[|\\]|\\{|\\}|\\(|\\))", Pattern.CASE_INSENSITIVE);
	    pattern[9] = Pattern.compile("(?:#[a-zA-Z\\$][a-zA-Z0-9\\$]*|#+[0-9]?)", Pattern.CASE_INSENSITIVE);
	    pattern[10] = Pattern.compile("(?:\\\\|\\+|\\-|\\*|\\/|,|;|\\.|:|@|~|=|>|<|&|\\||_|`|'|\\^|\\?|!|%)", Pattern.CASE_INSENSITIVE);
    }
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		    String ch;
		    ch = stream.next();
		    if (ch.equals("\"")) {
		        state.tokenize = new TokenString();
		        return state.tokenize.process(stream, state);
		    }
		    if (ch.equals("(")) {
		        if (!stream.eat("*").isEmpty()) {
		            state.commentLevel ++;
		            state.tokenize = new TokenComment();
		            return state.tokenize.process(stream, state);
		        }
		    }
		    stream.backUp(1);
		    if (!stream.match(reBaseForm).isEmpty()) {
		        return "number";
		    }
		    if (!stream.match(reFloatForm).isEmpty()) {
		        return "number";
		    }
		    if (!stream.match(pattern[0]).isEmpty()) {
		        return "atom";
		    }
		    if (!stream.match(pattern[1]).isEmpty()) {
		        return "meta";
		    }
		    if (!stream.match(pattern[2]).isEmpty()) {
		        return "string-2";
		    }
		    if (!stream.match(pattern[3]).isEmpty()) {
		        return "variable-2";
		    }
		    if (!stream.match(pattern[4]).isEmpty()) {
		        return "variable-2";
		    }
		    if (!stream.match(pattern[5]).isEmpty()) {
		        return "variable-2";
		    }
		    if (!stream.match(pattern[6]).isEmpty()) {
		        return "variable-2";
		    }
		    if (!stream.match(pattern[7]).isEmpty()) {
		        return "variable-3";
		    }
		    if (!stream.match(pattern[8]).isEmpty()) {
		        return "bracket";
		    }
		    if (!stream.match(pattern[9]).isEmpty()) {
		        return "variable-2";
		    }
		    if (!stream.match(reIdInContext).isEmpty()) {
		        return "keyword";
		    }
		    if (!stream.match(pattern[10]).isEmpty()) {
		        return "operator";
		    }
		    stream.next();
		    return "error";
		}
	}
	
	class TokenString implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		    String next;
		    boolean end = false, escaped = false;
		    while (!(next = stream.next()).isEmpty()) {
		        if (next.equals("\"") && !escaped) {
		            end = true;
		            break;
		        }
		        escaped = !escaped && next.equals("\\");
		    }
		    if (end && !escaped) {
		        state.tokenize = new TokenBase();
		    }
		    return "string";
		}
	}
	
	class TokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		    String prev = "", next;
		    while (state.commentLevel > 0 && !(next = stream.next()).isEmpty()) {
		        if (prev.equals("(") && next.equals("*")) state.commentLevel++;
		        if (prev.equals("*") && next.equals(")")) state.commentLevel--;
		        prev = next;
		    }
		    if (state.commentLevel <= 0) {
		        state.tokenize = new TokenBase();
		    }
		    return "comment";
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "m", "nb");
	}

	@Override
	public State startState() {
		return new State(new TokenBase(), 0);
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.eatSpace()) return "";
		return state.tokenize.process(stream, state);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-mathematica");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("mathematica");
	}
}
