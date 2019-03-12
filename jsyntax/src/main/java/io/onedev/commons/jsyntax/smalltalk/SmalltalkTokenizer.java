package io.onedev.commons.jsyntax.smalltalk;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class SmalltalkTokenizer extends AbstractTokenizer<SmalltalkTokenizer.State> {

	static final Pattern specialChars = Pattern.compile("[+\\-\\/\\\\*~<>=@%|&?!.,:;^]");
	static final Pattern keywords = Pattern.compile("true|false|nil|self|super|thisContext");
    
	@FunctionalInterface
	static interface Processor {
	    Token process(StringStream stream, Context context, State state);
	}
	
	static class Context {
	    Processor next;
	    Context parent;

	    public Context(Processor next, Context parent) {
	        this.next = next;
	        this.parent = parent;
	    }
	}
	
	static class Token {
	    String name;
	    Context context;
	    boolean eos;

	    public Token(String name, Context context, boolean eos) {
	        this.name = name;
	        this.context = context;
	        this.eos = eos;
	    }
	}
	
	static class State {
	    Context context;
	    boolean expectVariable;

	    public State(Context context, boolean expectVariable) {
	        this.context = context;
	        this.expectVariable = expectVariable;
	    }
	}
	
	static final Pattern patternNextComment = Pattern.compile("[^\"]");
	
	static final Processor nextComment = (stream, context, state) -> {
	    stream.eatWhile(patternNextComment);
	    return new Token("comment", !stream.eat("\"").isEmpty() ? context.parent : context, true);
	};
	
	static final Pattern patternNextString = Pattern.compile("[^\']");
	
	static final Processor nextString = (stream, context, state) -> {
	    stream.eatWhile(patternNextString);
	    return new Token("string", !stream.eat("'").isEmpty() ? context.parent : context, false);
	};
	
	static final Pattern patternNextSymbol = Pattern.compile("[^\']");
	
	static final Processor nextSymbol = (stream, context, state) -> {
	    stream.eatWhile(patternNextSymbol);
	    return new Token("string-2", !stream.eat("'").isEmpty() ? context.parent : context, false);
	};
	
	static final Pattern patternTemporaries = Pattern.compile("[^|]");
	
	static final Processor nextTemporaries = (stream, context, state) -> {
	    Token token = new Token("", context, false);
	    String aChar = stream.next();
	    if (aChar.equals("|")) {
	        token.context = context.parent;
	        token.eos = true;
	    }
	    else {
	        stream.eatWhile(patternTemporaries);
	        token.name = "variable";
	    }
	    return token;
	};
	
	static final Pattern pattern[] = new Pattern[8];
	static {
	    pattern[0] = Pattern.compile("[^\\s.{}\\[\\]()]");
	    pattern[1] = Pattern.compile("[^\\s>]");
	    pattern[2] = Pattern.compile("[\\[\\]{}()]");
	    pattern[3] = Pattern.compile("[\\[{(]");
	    pattern[4] = Pattern.compile("\\d");
	    pattern[5] = Pattern.compile("[\\w\\d]");
	    pattern[6] = Pattern.compile("[\\w_]");
	    pattern[7] = Pattern.compile("[\\w\\d_]");
	}
	
	static final Processor next = (stream, context, state) -> {
	    Token token = new Token("", context, false);
	    String aChar = stream.next();
	    if (aChar.equals("\"")) {
	        token = nextComment.process(stream, new Context(nextComment, context), state);
	    }
	    else if (aChar.equals("\'")) {
	        token = nextString.process(stream, new Context(nextString, context), state);
	    }
	    else if (aChar.equals("#")) {
	        if (stream.peek().equals("\'")) {
	            stream.next();
	            token = nextSymbol.process(stream, new Context(nextSymbol, context), state);
	        }
	        else {
	            if (stream.eatWhile(pattern[0])) token.name = "string-2";
	            else token.name = "meta";
	        }
	    }
	    else if (aChar.equals("$")) {
	        if (stream.next().equals("<")) {
	            stream.eatWhile(pattern[1]);
	            stream.next();
	        }
	        token.name = "string-2";
	    }
	    else if (aChar.equals("|") && state.expectVariable) {
	        token.context = new Context(nextTemporaries, context);
	    }
	    else if (pattern[2].matcher(aChar).matches()) {
	        token.name = "bracket";
	        token.eos = pattern[3].matcher(aChar).matches();
	    }
	    else if (specialChars.matcher(aChar).matches()) {
	        stream.eatWhile(specialChars);
	        token.name = "operator";
	        token.eos = !aChar.equals(";");
	    }
	    else if (pattern[4].matcher(aChar).matches()) {
	        stream.eatWhile(pattern[5]);
	        token.name = "number";
	    }
	    else if (pattern[6].matcher(aChar).matches()) {
	        stream.eatWhile(pattern[7]);
	        token.name = state.expectVariable ? (keywords.matcher(stream.current()).matches() ? "keyword" : "variable") : "";
	    }
	    else {
	        token.eos = state.expectVariable;
	    }
	    return token;
	};

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "st");
	}

	@Override
	public State startState() {
		return new State(new Context(next, null), true);
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (stream.eatSpace()) {
	        return "";
	    }
	    Token token = state.context.next.process(stream, state.context, state);
	    state.context = token.context;
	    state.expectVariable = token.eos;
	    return token.name;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-stsrc");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("smalltalk");
	}
}
