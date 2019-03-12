package io.onedev.commons.jsyntax.php;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.clike.PurePhpTokenizer;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;

public class PhpTokenizer extends AbstractTokenizer<PhpTokenizer.State> {
	
	static enum TokenizerMode {
		Html, Php, None
	}
	
	static class Pend {
		String style;
		int end;
		String value;
		
		public Pend(String style, int end, String value) {
			this.style = style;
			this.end = end;
			this.value = value;
		}
	}
	
	static class State {
		HtmlMixedTokenizer htmlTokenizer;
		HtmlMixedTokenizer.State htmlTokenizerState;
		
		PurePhpTokenizer phpTokenizer;
		PurePhpTokenizer.State phpTokenizerState;
		
		Pend pending;
		TokenizerMode mode;
		
		public State() {
			htmlTokenizer = new HtmlMixedTokenizer();
			htmlTokenizerState = htmlTokenizer.startState();
			pending = null;
			mode = TokenizerMode.None;
		}
	}
	
	private static Pattern[] pattern = new Pattern[4];
	
	static {
		pattern[0] = Pattern.compile("^<\\?\\w*");
		pattern[1] = Pattern.compile("<\\?");
		pattern[2] = Pattern.compile("[\'\"]$");
		pattern[3] = Pattern.compile("\\?>");
	}
	
	String dispath(StringStream stream, State state) {
		boolean isPhp = state.mode == TokenizerMode.Php;
		if (stream.sol() && state.pending != null && 
				!"\"".equals(state.pending.value) && !"'".equals(state.pending.value)) {
			state.pending = null;
		}
		if (!isPhp) {
			if (!stream.match(pattern[0]).isEmpty()) {
				if (state.phpTokenizer == null) {
					state.phpTokenizer = new PurePhpTokenizer() {};
					state.phpTokenizerState = state.phpTokenizer.startState();
				}
				state.mode = TokenizerMode.Php;
				return "meta";
			}
			String style;
			if (state.pending != null && ("\"".equals(state.pending.value) || "'".equals(state.pending.value))) {
				while (!stream.eol() && !stream.next().equals(state.pending.value));
				style = "string";
			} else if (state.pending != null && stream.pos() < state.pending.end) {
				stream.pos(state.pending.end);
				style = state.pending.style;
			} else {
				style = state.htmlTokenizer.token(stream, state.htmlTokenizerState);
			}
			if (state.pending != null) {
				state.pending = null;
			}
			String cur = stream.current();
			Matcher match = pattern[1].matcher(cur);
			int openPhp = match.find() ? match.start() : -1;
			if (openPhp != -1) {
				if (style.equals("string")) {
					match = pattern[2].matcher(cur);
					if (match.find() && !pattern[3].matcher(cur).matches()) {
						state.pending = new Pend(null, -1, match.group(0));
					} else {
						state.pending = new Pend(style, stream.pos(), null);
					}
					stream.backUp(cur.length() - openPhp);
				}
			}
			return style;
		} else if (isPhp && state.phpTokenizer.checkTokenize(state.phpTokenizerState) && stream.match("?>")) {
			state.mode = TokenizerMode.Html;
			if (state.phpTokenizer.checkContext(state.phpTokenizerState))
				state.phpTokenizer = null;
			return "meta";
		} else {
			return state.phpTokenizer.token(stream, state.phpTokenizerState);
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "php", "php3", "php4", "php5", "phtml");
	}

	@Override
	public State startState() {
		return new State();
	}

	@Override
	public String token(StringStream stream, State state) {
		return dispath(stream, state);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/x-httpd-php");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("php");
	}
}
