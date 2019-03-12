package io.onedev.commons.jsyntax.tornado;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class TornadoInnerTokenizer extends AbstractTokenizer<TornadoInnerTokenizer.State> {

	static final Set<String> keywords = Sets.newHashSet("and", "as", "assert", "autoescape", "block", "break", "class",
			"comment", "context", "continue", "datetime", "def", "del", "elif", "else", "end", "escape", "except",
			"exec", "extends", "false", "finally", "for", "from", "global", "if", "import", "in", "include", "is",
			"json_encode", "lambda", "length", "linkify", "load", "module", "none", "not", "or", "pass", "print", "put",
			"raise", "raw", "return", "self", "set", "squeeze", "super", "true", "try", "url_escape", "while", "with",
			"without", "xhtml_escape", "yield");

	static final Pattern Keywords = Pattern.compile("^((" + StringUtils.join(keywords, ")|(") + "))\\b");
	
	@FunctionalInterface
	static interface Processor {
		String process(StringStream stream, State state);
	}

	static class State {
		Processor tokenize;

		public State(Processor tokenize) {
			super();
			this.tokenize = tokenize;
		}
	}
	
	static final Pattern pattern[] = new Pattern[2];
	static {
	    pattern[0] = Pattern.compile("[^\\{]");
	    pattern[1] = Pattern.compile("\\{|%|#");
	}
	
	static final Processor tokenBase = (stream, state) -> {
	    stream.eatWhile(pattern[0]);
	    String ch = stream.next();
	    if (ch.equals("{")) {
	        if (!(ch = stream.eat(pattern[1])).isEmpty()) {
	            state.tokenize = inTag(ch);
	            return "tag";
	        }
	    }
	    return "";
	};
	
	static Processor inTag(String close) {
		if (close.equals("{")) {
			close = "}";
		}
		final String _close = close;
		return (stream, state) -> {
		    String ch = stream.next();
		    if ((ch.equals(_close)) && !stream.eat("}").isEmpty()) {
		        state.tokenize = tokenBase;
		        return "tag";
		    }
		    if (!stream.match(Keywords).isEmpty()) {
		        return "keyword";
		    }
		    return _close.equals("#") ? "comment" : "string";
		};
	}

	@Override
	public boolean accept(String fileName) {
		return false;
	}

	@Override
	public State startState() {
		return new State(tokenBase);
	}

	@Override
	public String token(StringStream stream, State state) {
		return state.tokenize.process(stream, state);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return false;
	}

	@Override
	public boolean acceptMode(String mode) {
		return false;
	}
}
