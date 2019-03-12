package io.onedev.commons.jsyntax.http;

import java.util.List;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class HttpTokenizer extends AbstractTokenizer<HttpTokenizer.State> {

	static class State {
		Processor cur;

		public State(Processor cur) {
			this.cur = cur;
		}
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	String failFirstLine(StringStream stream, State state) {
		stream.skipToEnd();
		state.cur = new Header();
		return "error";
	}

	static final Pattern pattern[] = new Pattern[3];
	static {
		pattern[0] = Pattern.compile("^HTTP\\/\\d\\.\\d");
		pattern[1] = Pattern.compile("^[A-Z]+");
		pattern[2] = Pattern.compile("[ \\t]");
	}

	class Start implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		if (!stream.match(pattern[0]).isEmpty()) {
			state.cur = new ResponseStatusCode();
			return "keyword";
		} else if (!stream.match(pattern[1]).isEmpty() && pattern[2].matcher(stream.peek()).matches()) {
			state.cur = new RequestPath();
			return "keyword";
		} else {
			return failFirstLine(stream, state);
		}
	}
	}

	static final Pattern pattern1[] = new Pattern[1];
	static {
		pattern1[0] = Pattern.compile("^\\d+");
	}

	class ResponseStatusCode implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			List<String> code = stream.match(pattern1[0]);
			if (code.isEmpty())
				return failFirstLine(stream, state);
			state.cur = new ResponseStatusText();
			int status = Integer.valueOf(code.get(0));
			if (status >= 100 && status < 200) {
				return "positive informational";
			} else if (status >= 200 && status < 300) {
				return "positive success";
			} else if (status >= 300 && status < 400) {
				return "positive redirect";
			} else if (status >= 400 && status < 500) {
				return "negative client-error";
			} else if (status >= 500 && status < 600) {
				return "negative server-error";
			} else {
				return "error";
			}
		}
	}

	class ResponseStatusText implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.skipToEnd();
			state.cur = new Header();
			return "";
		}
	}

	static final Pattern pattern2[] = new Pattern[1];
	static {
		pattern2[0] = Pattern.compile("\\S");
	}

	class RequestPath implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.eatWhile(pattern2[0]);
			state.cur = new RequestProtocol();
			return "string-2";
		}
	}

	static final Pattern pattern3[] = new Pattern[1];
	static {
		pattern3[0] = Pattern.compile("^HTTP\\/\\d\\.\\d$");
	}

	class RequestProtocol implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (!stream.match(pattern3[0]).isEmpty()) {
				state.cur = new Header();
				return "keyword";
			} else {
				return failFirstLine(stream, state);
			}
		}
	}

	static final Pattern pattern4[] = new Pattern[2];
	static {
		pattern4[0] = Pattern.compile("[ \\t]");
		pattern4[1] = Pattern.compile("^.*?:");
	}

	class Header implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (stream.sol() && !!stream.eat(pattern4[0]).isEmpty()) {
				if (!stream.match(pattern4[1]).isEmpty()) {
					return "atom";
				} else {
					stream.skipToEnd();
					return "error";
				}
			} else {
				stream.skipToEnd();
				return "string";
			}
		}
	}

	class Body implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.skipToEnd();
			return "";
		}
	}

	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "http");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(new Start());
	}

	@Override
	public String token(StringStream stream, State state) {
		// TODO Auto-generated method stub
		Processor cur = state.cur;
		if (!(cur instanceof Header) && !(cur instanceof Body) && stream.eatSpace())
			return "";
		return cur.process(stream,state);
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("message/http");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("http");
	}
}