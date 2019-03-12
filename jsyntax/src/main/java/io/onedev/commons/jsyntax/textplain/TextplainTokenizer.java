package io.onedev.commons.jsyntax.textplain;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class TextplainTokenizer extends AbstractTokenizer<TextplainTokenizer.State> {

	static class State {
		
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "txt", "text", "conf", "def", "list", "log");
	}

	@Override
	public State startState() {
		return new State();
	}

	@Override
	public String token(StringStream stream, State state) {
		stream.skipToEnd();
		return "";
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/plain");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("null");
	}
}
