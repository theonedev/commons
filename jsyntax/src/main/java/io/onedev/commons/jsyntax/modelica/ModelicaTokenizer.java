package io.onedev.commons.jsyntax.modelica;

import java.util.Set;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class ModelicaTokenizer extends AbstractTokenizer<ModelicaTokenizer.State> {
	static class State {
		Processor tokenize;
		boolean sol;
		int level;

		public State(Processor tokenize, boolean sol, int level) {
			super();
			this.tokenize = tokenize;
			this.sol = sol;
			this.level = level;
		}

	}

	private static Pattern isSingleOperatorChar = Pattern.compile("[;=\\(:\\),{}.*<>+\\-\\/^\\[\\]]");
	private static Pattern isDoubleOperatorChar = Pattern
			.compile("(:=|<=|>=|==|<>|\\.\\+|\\.\\-|\\.\\*|\\.\\/|\\.\\^)");
	private static Pattern isDigit = Pattern.compile("[0-9]");
	private static Pattern isNonDigit = Pattern.compile("[_a-zA-Z]");

	static interface Processor {
		String process(StringStream stream, State state);
	}

	private static Set<String> keywords = wordsOf(
			"algorithm and annotation assert block break class connect connector constant constrainedby der discrete each else elseif elsewhen encapsulated end enumeration equation expandable extends external false final flow for function if import impure in initial inner input loop model not operator or outer output package parameter partial protected public pure record redeclare replaceable return stream then true type when while within");
	private static Set<String> builtin = wordsOf(
			"abs acos actualStream asin atan atan2 cardinality ceil cos cosh delay div edge exp floor getInstanceName homotopy inStream integer log log10 mod pre reinit rem semiLinear sign sin sinh spatialDistribution sqrt tan tanh");
	private static Set<String> atoms = wordsOf("Real Boolean Integer String");

	class tokenLineComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.skipToEnd();
			state.tokenize = null;
			return "comment";
		}
	}

	class tokenBlockComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (maybeEnd && ch.equals("/")) {
					state.tokenize = null;
					break;
				}
				maybeEnd = ch.equals("*");
			}
			return "comment";
		}
	}

	class tokenString implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean escaped = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals("\"") && !escaped) {
					state.tokenize = null;
					state.sol = false;
					break;
				}
				escaped = !escaped && ch.equals("\\");
			}

			return "string";
		}

	}

	class tokenIdent implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.eatWhile(isDigit);
			while (stream.eat(isDigit).length() != 0 || stream.eat(isNonDigit).length() != 0) {
			}

			String cur = stream.current();

			if (state.sol
					&& (cur.equals("package") || cur.equals("model") || cur.equals("when") || cur.equals("connector")))
				state.level++;
			else if (state.sol && cur.equals("end") && state.level > 0)
				state.level--;

			state.tokenize = null;
			state.sol = false;

			if (keywords.contains(cur))
				return "keyword";
			else if (builtin.contains(cur))
				return "builtin";
			else if (atoms.contains(cur))
				return "atom";
			else
				return "variable";
		}

	}

	class tokenQIdent implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			while (stream.eat(Pattern.compile("[^']")).length() != 0) {
			}

			state.tokenize = null;
			state.sol = false;

			if (stream.eat("'").length() != 0)
				return "variable";
			else
				return "error";
		}

	}

	class tokenUnsignedNuber implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.eatWhile(isDigit);
			if (stream.eat(".").length() != 0)
				stream.eatWhile(isDigit);
			if (stream.eat("e").length() != 0 || stream.eat("E").length() != 0) {
				if (stream.eat("-").length() == 0)
					stream.eat("+");
				stream.eatWhile(isDigit);
			}

			state.tokenize = null;
			state.sol = false;
			return "number";
		}

	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "mo");
	}

	@Override
	public State startState() {
		return new State(null, true, 0);
	}

	@Override
	public String token(StringStream stream, State state) {
		if (state.tokenize != null)
			return state.tokenize.process(stream, state);

		if (stream.sol())
			state.sol = true;

		if (stream.eatSpace()) {
			state.tokenize = null;
			return "";
		}

		String ch = stream.next();

		if (ch.equals("/") && stream.eat("/").length() != 0)
			state.tokenize = new tokenBlockComment();
		else if (ch.equals("/") && stream.eat("*").length() != 0)
			state.tokenize = new tokenBlockComment();
		else if (isDoubleOperatorChar.matcher(ch + stream.peek()).matches()) {
			stream.next();
			state.tokenize = null;
			return "operator";
		} else if (isSingleOperatorChar.matcher(ch).matches()) {
			state.tokenize = null;
			return "operator";
		} else if (isNonDigit.matcher(ch).matches())
			state.tokenize = new tokenIdent();
		else if (ch.equals("'") && stream.peek().length() != 0 && !stream.peek().equals("'"))
			state.tokenize = new tokenQIdent();
		else if (ch.equals("\""))
			state.tokenize = new tokenString();
		else if (isDigit.matcher(ch).matches())
			state.tokenize = new tokenUnsignedNuber();
		else {
			state.tokenize = null;
			return "error";
		}

		return state.tokenize.process(stream, state);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-modelica");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("modelica");
	}
}
