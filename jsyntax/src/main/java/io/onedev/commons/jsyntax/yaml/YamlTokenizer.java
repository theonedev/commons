package io.onedev.commons.jsyntax.yaml;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class YamlTokenizer extends AbstractTokenizer<YamlTokenizer.State> {
	static final ArrayList<String> cons  = Lists.newArrayList("true", "false", "on", "off", "yes", "no");
	static final Pattern keywordRegex  = Pattern.compile("\\b(("+StringUtils.join(cons, ")|(")+"))$",Pattern.CASE_INSENSITIVE);
	static class State {
		boolean escaped;
		boolean literal ;
		boolean pair ;
		boolean pairStart ;
		int inlinePairs;
		int inlineList;
	    int keyCol;
		public State(boolean escaped,boolean literal ,boolean pair ,boolean pairStart ,int inlinePairs,int inlineList,int keyCol) {
			this.escaped = escaped;
			this.literal = literal;
			this.pair  = pair;
			this.pairStart = pairStart;
			this.inlinePairs = inlinePairs;
			this.inlineList = inlineList;
			this.keyCol = keyCol;
		}
	}
	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "yaml","yml");
	}

	@Override
	public boolean acceptMime(String mime) {
		// TODO Auto-generated method stub
		return mime != null && mime.equals("text/x-yaml");
	}

	@Override
	public boolean acceptMode(String mode) {
		// TODO Auto-generated method stub
		return mode != null && mode.equals("yaml");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(false, false, false, false, 0, 0,0);
	}
	static final Pattern pattern[] = new Pattern[12];
	static {
	    pattern[0] = Pattern.compile("\\s");
	    pattern[1] = Pattern.compile("^('([^']|\\\\.)*'?|\"([^\"]|\\\\.)*\"?)");
	    pattern[2] = Pattern.compile("---");
	    pattern[3] = Pattern.compile("\\.\\.\\.");
	    pattern[4] = Pattern.compile("\\s*-\\s+");
	    pattern[5] = Pattern.compile("^(\\{|\\}|\\[|\\])");
	    pattern[6] = Pattern.compile("^\\s*(\\||\\>)\\s*");
	    pattern[7] = Pattern.compile("^\\s*(\\&|\\*)[a-z0-9\\._-]+\\b", Pattern.CASE_INSENSITIVE);
	    pattern[8] = Pattern.compile("^\\s*-?[0-9\\.\\,]+\\s?$");
	    pattern[9] = Pattern.compile("^\\s*-?[0-9\\.\\,]+\\s?(?=(,|}))");
	    pattern[10] = Pattern.compile("^\\s*(?:[,\\[\\]{}&*!|>'\"%@`][^\\s'\":]|[^,\\[\\]{}#&*!|>'\"%@`])[^#]*?(?=\\s*:($|\\s))");
	    pattern[11] = Pattern.compile("^:\\s*");
	}
	@Override
	public String token(StringStream stream, State state) {
		  String ch = stream.peek();
		    boolean esc = state.escaped;
		    state.escaped = false;
		    if (ch.equals("#") && (stream.pos() == 0 || pattern[0].matcher(String.valueOf(stream.string().charAt(stream.pos()-1))).matches())) {
		        stream.skipToEnd();
		        return "comment";
		    }
		    if (!stream.match(pattern[1]).isEmpty()) return "string";
		    if (state.literal && stream.indentation() > state.keyCol) {
		        stream.skipToEnd();
		        return "string";
		    }
		    else if (state.literal) {
		        state.literal = false;
		    }
		    if (stream.sol()) {
		        state.keyCol = 0;
		        state.pair = false;
		        state.pairStart = false;
		        if (!stream.match(pattern[2]).isEmpty()) {
		            return "def";
		        }
		        if (!stream.match(pattern[3]).isEmpty()) {
		            return "def";
		        }
		        if (!stream.match(pattern[4]).isEmpty()) {
		            return "meta";
		        }
		    }
		    if (!stream.match(pattern[5]).isEmpty()) {
		        if (ch.equals("{")) state.inlinePairs ++;
		        else if (ch.equals("}")) state.inlinePairs --;
		        else if (ch.equals("[")) state.inlineList ++;
		        else state.inlineList --;
		        return "meta";
		    }
		    if (state.inlineList > 0 && !esc && ch.equals(",")) {
		        stream.next();
		        return "meta";
		    }
		    if (state.inlinePairs > 0 && !esc && ch.equals(",")) {
		        state.keyCol = 0;
		        state.pair = false;
		        state.pairStart = false;
		        stream.next();
		        return "meta";
		    }
		    if (state.pairStart) {
		        if (!stream.match(pattern[6]).isEmpty()) {
		            state.literal = true;
		            return "meta";
		        }
		        ;
		        if (!stream.match(pattern[7]).isEmpty()) {
		            return "variable-2";
		        }
		        if (state.inlinePairs == 0 && !stream.match(pattern[8]).isEmpty()) {
		            return "number";
		        }
		        if (state.inlinePairs > 0 && !stream.match(pattern[9]).isEmpty()) {
		            return "number";
		        }
		        if (!stream.match(keywordRegex).isEmpty()) {
		            return "keyword";
		        }
		    }
		    if (!state.pair && !stream.match(pattern[10]).isEmpty()) {
		        state.pair = true;
		        state.keyCol = stream.indentation();
		        return "atom";
		    }
		    if (state.pair && !stream.match(pattern[11]).isEmpty()) {
		        state.pairStart = true;
		        return "meta";
		    }
		    state.pairStart = false;
		    state.escaped = (ch.equals("\\"));
		    stream.next();
		    return "";
	}

}
