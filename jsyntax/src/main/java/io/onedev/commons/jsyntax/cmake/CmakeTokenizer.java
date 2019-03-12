package io.onedev.commons.jsyntax.cmake;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class CmakeTokenizer extends AbstractTokenizer<CmakeTokenizer.State>{

	static final Pattern variable_regex  = Pattern.compile("(\\{)?[a-zA-Z0-9_]+(\\})?");
	
	static class State{
		String pending;
		boolean continueString;
		boolean inDefinition;
		boolean inInclude ;
		public State(String pending,boolean continueString,boolean inDefinition,boolean inInclude ) {
			this.pending = pending;
			this.continueString = continueString;
			this.inDefinition = inDefinition;
			this.inInclude = inInclude;
		}
	}

	String tokenString(StringStream stream, State state) {
	    String current="", prev="";
	    boolean found_var = false;
	    while (!stream.eol() && !(current = stream.next()).equals(state.pending)) {
	        if (current.equals("$") &&! prev.equals("\\") && state.pending.equals("\"")) {
	            found_var = true;
	            break;
	        }
	        prev = current;
	    }
	    if (found_var) {
	        stream.backUp(1);
	    }
	    if (current.equals(state.pending)) {
	        state.continueString = false;
	    }
	    else {
	        state.continueString = true;
	    }
	    return "string";
	}



	static final Pattern pattern[] = new Pattern[4];
	static {
	    pattern[0] = Pattern.compile("(\\s+)?\\w+\\(");
	    pattern[1] = Pattern.compile("(\\s+)?\\w+\\ \\(");
	    pattern[2] = Pattern.compile("[0-9]");
	    pattern[3] = Pattern.compile("[\\w-]");
	   
	}

	String tokenize(StringStream stream, State state) {
	    String ch = stream.next();
	    if (ch.equals("$")) {
	        if (!stream.match(variable_regex).isEmpty()) {
	            return "variable-2";
	        }
	        return "variable";
	    }
	    if (state.continueString) {
	        stream.backUp(1);
	        return tokenString(stream, state);
	    }
	    if (!stream.match(pattern[0]).isEmpty() || !stream.match(pattern[1]).isEmpty()) {
	        stream.backUp(1);
	        return "def";
	    }
	    if (ch.equals("#")) {
	        stream.skipToEnd();
	        return "comment";
	    }
	    if (ch.equals("'") || ch.equals("\"")) {
	        state.pending = ch;
	        return tokenString(stream, state);
	    }
	    if (ch.equals("(") || ch.equals(")")) {
	        return "bracket";
	    }
	    if (pattern[2].matcher(ch).find()) {
	        return "number";
	    }
	    stream.eatWhile(pattern[3]);
	    return "";
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "cmake");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State("", false, false, false);
	}

	@Override
	public String token(StringStream stream, State state) {
		 if (stream.eatSpace())
			 return "" ;
	  return  tokenize(stream, state);
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-cmake");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("cmake");
	}
}
