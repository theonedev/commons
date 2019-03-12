package io.onedev.commons.jsyntax.diff;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class DiffTokenizer extends AbstractTokenizer<DiffTokenizer.State> {

	static final Map<String, String> TOKEN_NAMES = new HashMap<String, String>();
	static {
		TOKEN_NAMES.put("+", "positive");
		TOKEN_NAMES.put("-", "negative");
		TOKEN_NAMES.put("@", "meta");
	}
	
	static class State {
		
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "diff", "patch");
	}

	@Override
	public State startState() {
		return new State();
	}
	
	static final Pattern pattern[] = new Pattern[2];
	static {
		pattern[0] = Pattern.compile("[\\t ]+?$");
		pattern[1] = Pattern.compile(" $");
	}

	@Override
	public String token(StringStream stream, State state) {
		int tw_pos = -1;
		Matcher matcher = pattern[0].matcher(stream.string());
		if (matcher.find()) {
			tw_pos = matcher.start();
		}
	    if (!stream.sol() || tw_pos == 0) {
	        stream.skipToEnd();
	        String s = "error " + (TOKEN_NAMES.containsKey(stream.string().charAt(0) + "") ? TOKEN_NAMES.get(stream.string().charAt(0) + "") : "") ;
	        Matcher mat = pattern[1].matcher(s);
	        return mat.replaceAll("");
	    }
	    String token_name = "";
	    if (TOKEN_NAMES.containsKey(stream.peek())) {
	    	token_name = TOKEN_NAMES.get(stream.peek());
	    } else {
	    	stream.skipToEnd();
	    }
	    if (tw_pos == -1) {
	        stream.skipToEnd();
	    } else {
	        stream.pos(tw_pos);
	    }
	    return token_name;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-diff");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("diff");
	}
}
