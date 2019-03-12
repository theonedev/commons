package io.onedev.commons.jsyntax.solr;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class SolrTokenzier extends AbstractTokenizer<SolrTokenzier.State> {
	static final Pattern isStringChar = Pattern
			.compile("[^\\s\\|\\!\\+\\-\\*\\?\\~\\^\\&\\:\\(\\)\\[\\]\\{\\}\\\"\\\\]");
	static final Pattern isOperatorChar = Pattern.compile("[\\|\\!\\+\\-\\*\\?\\~\\^\\&]");
	static final Pattern isOperatorString = Pattern.compile("^(OR|AND|NOT|TO)$", Pattern.CASE_INSENSITIVE);

	boolean isNumber(String word) {
		for (int i = 0; i < word.length(); i++) {
			if (!Character.isDigit(word.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	static class State {
		Processor tokenize;

		public State(Processor tokenize) {
			this.tokenize = tokenize;
		}

	}

	static interface Processor {
		String processor(StringStream stream, State state);
	}

	class tokenString implements Processor {
		String quote;

		public tokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String processor(StringStream stream, State state) {
			boolean escaped = false;
			String next;
			while (!(next = stream.next()).isEmpty()) {
				if (next.equals(quote) && !escaped)
					break;
				escaped = !escaped && next.equals("\\");
			}
			if (!escaped)
				state.tokenize = new tokenBase();
			return "string";
		}
	}

	static final Pattern pattern[] = new Pattern[2];
	static {
		pattern[0] = Pattern.compile("\\|");
		pattern[1] = Pattern.compile("\\&");
	}

	class tokenOperator implements Processor {
		String operator;

		public tokenOperator(String operator) {
			this.operator = operator;
		}

		@Override
		public String processor(StringStream stream, State state) {
			String style = "operator";
			if (operator.equals("+"))
				style += " positive";
			else if (operator.equals("-"))
				style += " negative";
			else if (operator.equals("|"))
				stream.eat(pattern[0]);
			else if (operator.equals("&"))
				stream.eat(pattern[1]);
			else if (operator.equals("^"))
				style += " boost";
			state.tokenize = new tokenBase();
			return style;
		}
	}

	class tokenWord implements Processor {
		String ch;

		public tokenWord(String ch) {
			this.ch = ch;
		}

		@Override
		public String processor(StringStream stream, State state) {
			String word = ch;
			while (!(ch = stream.peek()).isEmpty() && isStringChar.matcher(ch).matches()) {
				word += stream.next();
			}
			state.tokenize = new tokenBase();
			if (isOperatorString.matcher(word).matches())
				return "operator";
			else if (isNumber(word))
				return "number";
			else if (stream.peek().equals(":"))
				return "field";
			else
				return "string";
		}

	}

	class tokenBase implements Processor {

		@Override
		public String processor(StringStream stream, State state) {
			String ch = stream.next();
			if (ch.equals("\""))
				state.tokenize = new tokenString(ch);
			else if (isOperatorChar.matcher(ch).matches())
				state.tokenize = new tokenOperator(ch);
			else if (isStringChar.matcher(ch).matches())
				state.tokenize = new tokenWord(ch);
			return !(state.tokenize instanceof tokenBase) ? state.tokenize.processor(stream, state) : "";
		}

	}

	@Override
	public boolean accept(String fileName) {

		return acceptExtensions(fileName, "solr");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(new tokenBase());
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.eatSpace())
			return "";
		return state.tokenize.processor(stream, state);
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-solr");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("solr");
	}
}
