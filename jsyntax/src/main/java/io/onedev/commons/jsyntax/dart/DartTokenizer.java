package io.onedev.commons.jsyntax.dart;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.clike.ClikeTokenizer;

public class DartTokenizer extends ClikeTokenizer {

	static final Set<String> keywords = wordsOf("this super static final const abstract class extends external factory " +
		    "implements get native operator set typedef with enum throw rethrow " +
		    "assert break case continue default in return new deferred async await " +
		    "try catch finally do else for if switch while import library export " +
		    "part of show hide is as");
	static final Set<String> blockKeywords = wordsOf("try catch finally do else for if switch while");
	static final Set<String> atoms = wordsOf("true false null");
	static final Set<String> builtins = wordsOf("void bool num int double dynamic var String");
	static final Pattern[] pattern = new Pattern[2];
	static final Map<String, Processor> hooks = Maps.newHashMap();
	
	static {
		pattern[0] = Pattern.compile("[\\w\\$_\\.]");
		pattern[1] = Pattern.compile("[\\w_]");
		hooks.put("@", (stream, state, style) -> {
			stream.eatWhile(pattern[0]);
			return "meta";
		});
		hooks.put("'", (stream, state, style) -> {
			return tokenString("'", stream, state, false);
		});
		hooks.put("\"", (stream, state, style) -> {
			return tokenString("\"", stream, state, false);
		});
		hooks.put("r", (stream, state, style) -> {
		    String peek = stream.peek();
		    if (peek.equals("'") || peek.equals("\"")) {
		        return tokenString(stream.next(), stream, state, true);
		    }
		    return "";
		});
		hooks.put("}", (stream, state, style) -> {
	        if (sizeInterpolationStack(state) > 0) {
	        	state.tokenize = popInterpolationStack(state);
	            return "";
	        }
	        return "";
		});
		hooks.put("/", (stream, state, style) -> {
		    if (!!stream.eat("*").isEmpty()) return "";
		    state.tokenize = tokenNestedComment(1);
		    return state.tokenize.process(stream, state, style);
		});
	}
	
	static void pushInterpolationStack(State state) {
		state.interpolationStack.push(state.tokenize);
	}

	static Processor popInterpolationStack(State state) {
		return state.interpolationStack.pop();
	}

	static int sizeInterpolationStack(State state) {
		return state.interpolationStack.size();
	}
	
	static String tokenString(String quote, StringStream stream, State state, boolean raw) {
	    boolean tripleQuoted = false;
	    if (!stream.eat(quote).isEmpty()) {
	        if (!stream.eat(quote).isEmpty()) tripleQuoted = true;
	        else return "string";
	    }
	    final boolean _tripleQuoted = tripleQuoted;
	    state.tokenize = (_stream, _state, style) -> {
		    boolean escaped = false;
		    while (!_stream.eol()) {
		        if (!raw && !escaped && _stream.peek().equals("$")) {
		            pushInterpolationStack(_state);
		            state.tokenize = tokenInterpolation();
		            return "string";
		        }
		        String next = _stream.next();
		        if (next.equals(quote) && !escaped && (!_tripleQuoted || _stream.match(quote + quote))) {
		            state.tokenize = null;
		            break;
		        }
		        escaped = !raw && !escaped && next.equals("\\");
		    }
		    return "string";
		};
	    return state.tokenize.process(stream, state, "");
	}
	
	static Processor tokenInterpolation() {
		return (stream, state, style) -> {
		    stream.eat("$");
		    if (!stream.eat("{").isEmpty()) {
		        state.tokenize = null;
		    }
		    else {
		        state.tokenize = tokenInterpolationIdentifier();
		    }
		    return "";
		};
	}
	
	static Processor tokenInterpolationIdentifier() {
		return (stream, state, style) -> {
		    stream.eatWhile(pattern[1]);
		    state.tokenize = popInterpolationStack(state);
		    return "variable";
		};
	}
	
	static Processor tokenNestedComment(int depth) {
		return (stream, state, style) -> {
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (ch.equals("*") && !stream.eat("/").isEmpty()) {
		            if (depth == 1) {
		                state.tokenize = null;
		                break;
		            }
		            else {
		                state.tokenize = tokenNestedComment(depth - 1);
		                return state.tokenize.process(stream, state, style);
		            }
		        }
		        else if (ch.equals("/") && !stream.eat("*").isEmpty()) {
		            state.tokenize = tokenNestedComment(depth + 1);
		            return state.tokenize.process(stream, state,style);
		        }
		    }
		    return "comment";
		};
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "dart");
	}

	@Override
	protected Set<String> keywords() {
		return keywords;
	}

	@Override
	protected Set<String> builtin() {
		return builtins;
	}

	@Override
	protected Set<String> blockKeywords() {
		return blockKeywords;
	}

	@Override
	protected Set<String> atoms() {
		return atoms;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return hooks;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null 
				&& (mime.equals("text/x-dart")
				|| mime.equals("application/dart"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("dart");
	}
}
