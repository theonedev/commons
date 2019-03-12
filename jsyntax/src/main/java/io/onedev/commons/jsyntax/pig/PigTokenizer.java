package io.onedev.commons.jsyntax.pig;

import java.util.Set;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class PigTokenizer extends AbstractTokenizer<PigTokenizer.State> {
	static class State {
		Processor tokenize;
		boolean startOfLine;

		public State(Processor tokenize, boolean startOfLine) {
			this.tokenize = tokenize;
			this.startOfLine = startOfLine;
		}

	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	private static Set<String> keywords = wordsOf(
			"VOID IMPORT RETURNS DEFINE LOAD FILTER FOREACH ORDER CUBE DISTINCT COGROUP "
					+ "JOIN CROSS UNION SPLIT INTO IF OTHERWISE ALL AS BY USING INNER OUTER ONSCHEMA PARALLEL "
					+ "PARTITION GROUP AND OR NOT GENERATE FLATTEN ASC DESC IS STREAM THROUGH STORE MAPREDUCE "
					+ "SHIP CACHE INPUT OUTPUT STDERROR STDIN STDOUT LIMIT SAMPLE LEFT RIGHT FULL EQ GT LT GTE LTE "
					+ "NEQ MATCHES TRUE FALSE DUMP ");
	private static Set<String> Builtins = wordsOf(
			"ABS ACOS ARITY ASIN ATAN AVG BAGSIZE BINSTORAGE BLOOM BUILDBLOOM CBRT CEIL "
					+ "CONCAT COR COS COSH COUNT COUNT_STAR COV CONSTANTSIZE CUBEDIMENSIONS DIFF DISTINCT DOUBLEABS "
					+ "DOUBLEAVG DOUBLEBASE DOUBLEMAX DOUBLEMIN DOUBLEROUND DOUBLESUM EXP FLOOR FLOATABS FLOATAVG "
					+ "FLOATMAX FLOATMIN FLOATROUND FLOATSUM GENERICINVOKER INDEXOF INTABS INTAVG INTMAX INTMIN "
					+ "INTSUM INVOKEFORDOUBLE INVOKEFORFLOAT INVOKEFORINT INVOKEFORLONG INVOKEFORSTRING INVOKER "
					+ "ISEMPTY JSONLOADER JSONMETADATA JSONSTORAGE LAST_INDEX_OF LCFIRST LOG LOG10 LOWER LONGABS "
					+ "LONGAVG LONGMAX LONGMIN LONGSUM MAX MIN MAPSIZE MONITOREDUDF NONDETERMINISTIC OUTPUTSCHEMA  "
					+ "PIGSTORAGE PIGSTREAMING RANDOM REGEX_EXTRACT REGEX_EXTRACT_ALL REPLACE ROUND SIN SINH SIZE "
					+ "SQRT STRSPLIT SUBSTRING SUM STRINGCONCAT STRINGMAX STRINGMIN STRINGSIZE TAN TANH TOBAG "
					+ "TOKENIZE TOMAP TOP TOTUPLE TRIM TEXTLOADER TUPLESIZE UCFIRST UPPER UTF8STORAGECONVERTER ");
	private static Set<String> Types = wordsOf("BOOLEAN INT LONG FLOAT DOUBLE CHARARRAY BYTEARRAY BAG TUPLE MAP ");
	static Pattern[] pattern = new Pattern[4];

	static {
		pattern[0] = Pattern.compile("[\\[\\]{}\\(\\),;\\.]");
		pattern[1] = Pattern.compile("\\d");
		pattern[2] = Pattern.compile("[\\w\\.]");
		pattern[3] = Pattern.compile("[\\w\\$_]");
	}

	static Pattern isOperatorChar = Pattern.compile("[*+\\-%<>=&?:\\/!|]");

	class tokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean isEnd = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals("/") && isEnd) {
					state.tokenize = new tokenBase();
					break;
				}
				isEnd = ch.equals("*");
			}
			return "comment";
		}

	}

	class tokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			String ch = stream.next();

			if (ch.equals("\"") || ch.equals("'")) {
				state.tokenize = new tokenString(ch);
				return state.tokenize.process(stream, state);
			} else if (pattern[0].matcher(ch).matches()) {
				return "";
			} else if (pattern[1].matcher(ch).matches()) {
				stream.eatWhile(pattern[2]);
				return "number";
			} else if (ch.equals("/")) {
				if (stream.eat("*").length() != 0) {
					state.tokenize = new tokenComment();
					return state.tokenize.process(stream, state);
				} else {
					stream.eatWhile(isOperatorChar);
					return "operator";
				}
			} else if (ch.equals("-")) {
				if (stream.eat("-").length() != 0) {
					stream.skipToEnd();
					return "comment";
				} else {
					stream.eatWhile(isOperatorChar);
					return "operator";
				}
			} else if (isOperatorChar.matcher(ch).matches()) {
				stream.eatWhile(isOperatorChar);
				return "operator";
			} else {
				stream.eatWhile(pattern[3]);
				if (!keywords.isEmpty() && keywords.contains(stream.current().toUpperCase())) {
					if (stream.eat(")").length() == 0 && stream.eat(".").length() == 0) {
						return "keyword";
					}
				}
				if (!Builtins.isEmpty() && Builtins.contains(stream.current().toUpperCase())) {
					return "variable-2";
				}
				if (!Types.isEmpty() && Types.contains(stream.current().toUpperCase())) {
					return "variable-3";
				}
				return "variable";
			}
		}

	}

	class tokenString implements Processor {
		private String quote;

		public tokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state) {
			boolean escaped = false;
			String next;
			boolean end = false;
			while ((next = stream.next()).length() != 0) {
				if (next.equals(quote) && !escaped) {
					end = true;
					break;
				}
				escaped = !escaped && next.equals("\\");
			}
			if (end || !escaped)
				state.tokenize = new tokenBase();
			return "error";
		}

	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "pig");
	}

	@Override
	public State startState() {
		return new State(new tokenBase(), true);
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.eatSpace()) {
			return "";
		}
		String style = state.tokenize.process(stream, state);
		return style;

	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-pig");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("pig");
	}
}
