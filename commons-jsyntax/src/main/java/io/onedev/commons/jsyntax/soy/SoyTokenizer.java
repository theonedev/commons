package io.onedev.commons.jsyntax.soy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.css.CssTokenizer;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;
import io.onedev.commons.jsyntax.javascript.JavaScriptTokenizer;
import io.onedev.commons.jsyntax.textplain.TextplainTokenizer;

public class SoyTokenizer extends AbstractTokenizer<SoyTokenizer.State> {

	static final Set<String> indentingTags = Sets.newHashSet("template", "literal", "msg", "fallbackmsg", "let", "if",
			"elseif", "else", "switch", "case", "default", "foreach", "ifempty", "for", "call", "param", "deltemplate",
			"delcall", "log");
	@SuppressWarnings("rawtypes")
	static final Map<String, AbstractTokenizer> modes = Maps.newHashMap();

	static {
		modes.put("html", new HtmlMixedTokenizer());
		modes.put("attributes", new TextplainTokenizer());
		modes.put("text", new TextplainTokenizer());
		modes.put("uri", new TextplainTokenizer());
		modes.put("css", new CssTokenizer());
		modes.put("js", new JavaScriptTokenizer());
	}

	static class State {
		Stack<String> kind;
		Stack<String> kindTag;
		String tag;
		Stack<String> soyState;
		@SuppressWarnings("rawtypes")
		AbstractTokenizer localMode;
		Object localState;

		@SuppressWarnings("rawtypes")
		public State(Stack<String> kind, Stack<String> kindTag, String tag, Stack<String> soyState,
				AbstractTokenizer localMode, Object localState) {
			this.kind = kind;
			this.kindTag = kindTag;
			this.tag = tag;
			this.soyState = soyState;
			this.localMode = localMode;
			this.localState = localState;
		}
	}

	@SuppressWarnings("unchecked")
	String tokenUntil(StringStream stream, State state, Pattern untilRegExp) {
		String oldString = stream.string();
		Matcher match = untilRegExp.matcher(StringUtils.substring(oldString, stream.pos()));
		if (match.find()) {
			stream.string(StringUtils.substring(oldString, 0, stream.pos() + match.start()));
		}
		String result = stream.hideFirstChars(0, () -> {
			return state.localMode.token(stream, state.localState);
		}).toString();
		stream.string(oldString);
		return result;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "soy");
	}

	@Override
	public State startState() {
		return new State(new Stack<String>(), new Stack<String>(), "", new Stack<String>(),
				modes.get("html"), modes.get("html").startState());
	}

	static final Pattern pattern[] = new Pattern[17];
	static {
	    pattern[0] = Pattern.compile("^.*?\\*\\/");
	    pattern[1] = Pattern.compile("^}");
	    pattern[2] = Pattern.compile("^\\/?}");
	    pattern[3] = Pattern.compile("^([\\w?]+)(?==)");
	    pattern[4] = Pattern.compile("^=\"([^\"]+)");
	    pattern[5] = Pattern.compile("^\"");
	    pattern[6] = Pattern.compile("^(?=\\{\\/literal})");
	    pattern[7] = Pattern.compile("\\{\\/literal}");
	    pattern[8] = Pattern.compile("^.*?(\"|\\\\[\\s\\S])");
	    pattern[9] = Pattern.compile("^\\/\\*");
	    pattern[10] = Pattern.compile("^\\s*\\/\\/.*");
	    pattern[11] = Pattern.compile("^\\s+\\/\\/.*");
	    pattern[12] = Pattern.compile("^\\{\\$[\\w?]*");
	    pattern[13] = Pattern.compile("^\\{literal}");
	    pattern[14] = Pattern.compile("^\\{([\\/@\\\\]?[\\w?]*)");
	    pattern[15] = Pattern.compile("^(\\/|(else|elseif|case|default)$)");
	    pattern[16] = Pattern.compile("\\{|\\s+\\/\\/|\\/\\*");
	}

	@Override
	public String token(StringStream stream, State state) {
	    List<String> match;
	    switch(state.soyState.isEmpty() ? "" : state.soyState.peek()) {
	    case "comment" : 
	    	if (!stream.match(pattern[0]).isEmpty()) {
	            state.soyState.pop();
	        }
	        else {
	            stream.skipToEnd();
	        }
	        return "comment";
	    case "variable" :
	    	if (!stream.match(pattern[1]).isEmpty()) {
	            state.soyState.pop();
	            return "variable-2";
	        }
	        stream.next();
	        return "";
	    case "tag" :
	    	if (!stream.match(pattern[2]).isEmpty()) {
	            state.soyState.pop();
	            return "keyword";
	        }
	        else if (!stream.match(pattern[3]).isEmpty()) {
	            if (stream.current().equals("kind") && !(match = stream.match(pattern[4], false)).isEmpty()) {
	                String kind = match.get(1);
	                state.kind.push(kind);
	                state.kindTag.push(state.tag);
	                state.localMode = modes.containsKey(kind) ? modes.get(kind) : modes.get("html");
	                state.localState = state.localMode.startState();
	            }
	            return "attribute";
	        }
	        else if (!stream.match(pattern[5]).isEmpty()) {
	            state.soyState.push("string");
	            return "string";
	        }
	        stream.next();
	        return "";
	    case "literal" :
	    	if (!stream.match(pattern[6]).isEmpty()) {
	            state.soyState.pop();
	            return this.token(stream, state);
	        }
	        return tokenUntil(stream, state, pattern[7]);
	    case "string" :
	    	match = stream.match(pattern[8]);
	        if (match.isEmpty()) {
	            stream.skipToEnd();
	        }
	        else if (match.get(1).equals("\"")) {
	            state.soyState.pop();
	        }
	        return "string";
	    }
	    if (!stream.match(pattern[9]).isEmpty()) {
	        state.soyState.push("comment");
	        return "comment";
	    }
	    else if (!stream.match(stream.sol()?pattern[10]:pattern[11]).isEmpty()) {
	        return "comment";
	    }
	    else if (!stream.match(pattern[12]).isEmpty()) {
	        state.soyState.push("variable");
	        return "variable-2";
	    }
	    else if (!stream.match(pattern[13]).isEmpty()) {
	        state.soyState.push("literal");
	        return "keyword";
	    }
	    else if (!(match = stream.match(pattern[14])).isEmpty()) {
	        state.tag = match.get(1);
	        if (state.tag.equals("/" + (state.kindTag.isEmpty() ? "" : state.kindTag.peek()))) {
	            state.kind.pop();
	            state.kindTag.pop();
	            if (state.kind.isEmpty() || !modes.containsKey(state.kind.peek())) {
	            	state.localMode = modes.get("html");
	            } else {
	            	state.localMode = modes.get(state.kind.peek());
	            }
	            state.localState = state.localMode.startState();
	        }
	        state.soyState.push("tag");
	        return "keyword";
	    }
	    return tokenUntil(stream, state, pattern[16]);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-soy");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("soy");
	}
}
