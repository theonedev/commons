package io.onedev.commons.jsyntax.pegjs;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.javascript.JavaScriptTokenizer;

public class PegjsTokenizer extends AbstractTokenizer<PegjsTokenizer.State> {
	
	static final Pattern identifier = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*");

	static class State {
		JavaScriptTokenizer jsMode;
		JavaScriptTokenizer.State localState;
		String stringType;
		int braced;
		boolean inString;
		boolean inComment;
		boolean inCharacterClass;
		boolean lhs;
		
		public State(String stringType,
				int braced, boolean inString, boolean inComment, boolean inCharacterClass, boolean lhs) {
			this.jsMode = new JavaScriptTokenizer();
			this.localState = jsMode.startState();
			this.stringType = stringType;
			this.braced = braced;
			this.inString = inString;
			this.inComment = inComment;
			this.inCharacterClass = inCharacterClass;
			this.lhs = lhs;
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "jsonld");
	}

	@Override
	public State startState() {
		return new State("", 0, false, false, false, true);
	}
	
	static final Pattern pattern[] = new Pattern[7];
	static {
	    pattern[0] = Pattern.compile("^\\/\\*");
	    pattern[1] = Pattern.compile("^.[^\\\\\\\"\\']*");
	    pattern[2] = Pattern.compile("\\*\\/");
	    pattern[3] = Pattern.compile("^.[^\\*]*");
	    pattern[4] = Pattern.compile("^[^\\]\\\\]+");
	    pattern[5] = Pattern.compile("^\\\\.");
	    pattern[6] = Pattern.compile("^\\/\\/");
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (!state.inString && !state.inComment && ((stream.peek().equals("\"")) || (stream.peek().equals("'")))) {
	        state.stringType = stream.peek();
	        stream.next();
	        state.inString = true;
	    }
	    if (!state.inString && !state.inComment && !stream.match(pattern[0]).isEmpty()) {
	        state.inComment = true;
	    }
	    if (state.inString) {
	        while (state.inString && !stream.eol()) {
	            if (stream.peek().equals(state.stringType)) {
	                stream.next();
	                state.inString = false;
	            }
	            else if (stream.peek().equals("\\")) {
	                stream.next();
	                stream.next();
	            }
	            else {
	                stream.match(pattern[1]);
	            }
	        }
	        return state.lhs ? "property string" : "string";
	    }
	    else if (state.inComment) {
	        while (state.inComment && !stream.eol()) {
	            if (!stream.match(pattern[2]).isEmpty()) {
	                state.inComment = false;
	            }
	            else {
	                stream.match(pattern[3]);
	            }
	        }
	        return "comment";
	    }
	    else if (state.inCharacterClass) {
	        while (state.inCharacterClass && !stream.eol()) {
	            if (!(!stream.match(pattern[4]).isEmpty() || !stream.match(pattern[5]).isEmpty())) {
	                state.inCharacterClass = false;
	            }
	        }
	    }
	    else if (stream.peek().equals("[")) {
	        stream.next();
	        state.inCharacterClass = true;
	        return "bracket";
	    }
	    else if (!stream.match(pattern[6]).isEmpty()) {
	        stream.skipToEnd();
	        return "comment";
	    }
	    else if (state.braced != 0 || stream.peek().equals("{")) {
	        String token = state.jsMode.token(stream, state.localState);
	        String text = stream.current();
	        if (token.isEmpty()) {
	            for (int i = 0; i < text.length(); i++) {
	                if (text.charAt(i) == '{') {
	                    state.braced++;
	                }
	                else if (text.charAt(i) == '}') {
	                    state.braced--;
	                }
	            }
	        }
	        return token;
	    }
	    else if (!stream.match(identifier).isEmpty()) {
	        if (stream.peek().equals(":")) {
	            return "variable";
	        }
	        return "variable-2";
	    }
	    else if ("[]()".indexOf(stream.peek()) != -1) {
	        stream.next();
	        return "bracket";
	    }
	    else if (!stream.eatSpace()) {
	        stream.next();
	    }
	    return "";
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("null");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("pegjs");
	}
}
