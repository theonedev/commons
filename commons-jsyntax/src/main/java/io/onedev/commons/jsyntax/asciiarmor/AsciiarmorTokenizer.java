package io.onedev.commons.jsyntax.asciiarmor;

import java.util.List;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class AsciiarmorTokenizer extends AbstractTokenizer<AsciiarmorTokenizer.State> {

	public static class State {
		String state;
		String type;

		State(String state, String type) {
			this.state = state;
			this.type = type;
		}
	}

	static Pattern pattern[] = new Pattern[5];

	static {
		pattern[0] = Pattern.compile("^\\s*\\S");
		pattern[1] = Pattern.compile("^-----BEGIN (.*)?-----\\s*$");
		pattern[2] = Pattern.compile("^\\w+:");
		pattern[3] = Pattern.compile("^-----END (.*)?-----\\s*$");
		pattern[4] = Pattern.compile("[A-Za-z0-9+\\/=]");
	}

	private String errorIfNotEmpty(StringStream stream) {
		List<String> nonWS = stream.match(pattern[0]);
		stream.skipToEnd();
		return !nonWS.isEmpty() ? "error" : "";
	}

	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "pgp");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State("top", "");
	}

	public void blankLine(State state) {
		if (state.state.equals("headers"))
			state.state = "body";
	}

	@Override
	public String token(StringStream stream, State state) {
		// TODO Auto-generated method stub
		List<String> m;
		if (state.state.equals("top")) {

			if (stream.sol() && (!(m = stream.match(pattern[1])).isEmpty())) {
				state.state = "headers";
				state.type = m.get(1).toString();
				return "tag";
			}
			return errorIfNotEmpty(stream);
		} else if (state.state.equals("headers")) {
			if (stream.sol() && !(stream.match(pattern[2]).isEmpty())) {
				state.state = "header";
				return "atom";
			} else {
				String result = errorIfNotEmpty(stream);
				if (!result.isEmpty())
					state.state = "body";
				return result;
			}
		} else if (state.state.equals("header")) {
			stream.skipToEnd();
			state.state = "headers";
			return "string";
		} else if (state.state == "body") {

			if (stream.sol() && !(m = stream.match(pattern[3])).isEmpty()) {
				if (!m.get(1).equals(state.type))
					return "error";
				state.state = "end";
				return "tag";
			} else {
				if (stream.eatWhile(pattern[4])) {
					return "";
				} else {
					stream.next();
					return "error";
				}
			}
		} else if (state.state.equals("end")) {
			return errorIfNotEmpty(stream);
		}
		return "";
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null 
				&& (mime.equals("application/pgp") 
				|| mime.equals("application/pgp-keys") 
				|| mime.equals("application/pgp-signature"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("asciiarmor");
	}
}
