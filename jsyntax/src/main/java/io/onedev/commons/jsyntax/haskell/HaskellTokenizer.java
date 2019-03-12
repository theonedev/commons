package io.onedev.commons.jsyntax.haskell;

import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class HaskellTokenizer extends AbstractTokenizer<HaskellTokenizer.State> {

	static final Pattern smallRE = Pattern.compile("[a-z_]");
	static final Pattern largeRE = Pattern.compile("[A-Z]");
	static final Pattern digitRE = Pattern.compile("\\d");
	static final Pattern hexitRE = Pattern.compile("[0-9A-Fa-f]");
	static final Pattern octitRE = Pattern.compile("[0-7]");
	static final Pattern idRE = Pattern.compile("[a-z_A-Z0-9'\\xa1-\\uffff]");
	static final Pattern symbolRE = Pattern.compile("[-!#$%&*+.\\/<=>?@\\\\^|~:]");
	static final Pattern specialRE = Pattern.compile("[(),;\\[\\]`\\{\\}]");
	static final Pattern whiteCharRE = Pattern.compile("[ \\t\\v\\f]");

	static final Map<String, String> wellKnownWords = Maps.newHashMap();
	static {
		setType("keyword", "case", "class", "data", "default", "deriving", "do", "else", "foreign", "if", "import",
				"in", "infix", "infixl", "infixr", "instance", "let", "module", "newtype", "of", "then", "type",
				"where", "_");
		setType("keyword", "..", ":", "::", "=", "\\", "<-", "->", "@", "~", "=>");
		setType("builtin", "!!", "$!", "$", "&&", "+", "++", "-", ".", "/", "/=", "<", "<=", "=<<", "==", ">", ">=",
				">>", ">>=", "^", "^^", "||", "*", "**");
		setType("builtin", "Bool", "Bounded", "Char", "Double", "EQ", "Either", "Enum", "Eq", "False", "FilePath",
				"Float", "Floating", "Fractional", "Functor", "GT", "IO", "IOError", "Int", "Integer", "Integral",
				"Just", "LT", "Left", "Maybe", "Monad", "Nothing", "Num", "Ord", "Ordering", "Rational", "Read",
				"ReadS", "Real", "RealFloat", "RealFrac", "Right", "Show", "ShowS", "String", "True");
		setType("builtin", "abs", "acos", "acosh", "all", "and", "any", "appendFile", "asTypeOf", "asin", "asinh",
				"atan", "atan2", "atanh", "break", "catch", "ceiling", "compare", "concat", "concatMap", "const", "cos",
				"cosh", "curry", "cycle", "decodeFloat", "div", "divMod", "drop", "dropWhile", "either", "elem",
				"encodeFloat", "enumFrom", "enumFromThen", "enumFromThenTo", "enumFromTo", "error", "even", "exp",
				"exponent", "fail", "filter", "flip", "floatDigits", "floatRadix", "floatRange", "floor", "fmap",
				"foldl", "foldl1", "foldr", "foldr1", "fromEnum", "fromInteger", "fromIntegral", "fromRational", "fst",
				"gcd", "getChar", "getContents", "getLine", "head", "id", "init", "interact", "ioError",
				"isDenormalized", "isIEEE", "isInfinite", "isNaN", "isNegativeZero", "iterate", "last", "lcm", "length",
				"lex", "lines", "log", "logBase", "lookup", "map", "mapM", "mapM_", "max", "maxBound", "maximum",
				"maybe", "min", "minBound", "minimum", "mod", "negate", "not", "notElem", "null", "odd", "or",
				"otherwise", "pi", "pred", "print", "product", "properFraction", "putChar", "putStr", "putStrLn",
				"quot", "quotRem", "read", "readFile", "readIO", "readList", "readLn", "readParen", "reads",
				"readsPrec", "realToFrac", "recip", "rem", "repeat", "replicate", "return", "reverse", "round",
				"scaleFloat", "scanl", "scanl1", "scanr", "scanr1", "seq", "sequence", "sequence_", "show", "showChar",
				"showList", "showParen", "showString", "shows", "showsPrec", "significand", "signum", "sin", "sinh",
				"snd", "span", "splitAt", "sqrt", "subtract", "succ", "sum", "tail", "take", "takeWhile", "tan", "tanh",
				"toEnum", "toInteger", "toRational", "truncate", "uncurry", "undefined", "unlines", "until", "unwords",
				"unzip", "unzip3", "userError", "words", "writeFile", "zip", "zip3", "zipWith", "zipWith3");
	}

	@FunctionalInterface
	static interface Processor {
		String process(StringStream stream, SetState setState);
	}

	static interface SetState {
		void setState(Processor f);
	}

	static class State {
		Processor f;

		public State(Processor f) {
			super();
			this.f = f;
		}
	}

	static void setType(String type, String... args) {
		for (String s : args) {
			wellKnownWords.put(s, type);
		}
	}

	static String switchState(StringStream stream, SetState setState, Processor f) {
		setState.setState(f);
		return f.process(stream, setState);
	}

	static final Pattern pattern[] = new Pattern[7];
	static {
		pattern[0] = Pattern.compile("[xX]");
		pattern[1] = Pattern.compile("[oO]");
		pattern[2] = Pattern.compile("^\\.\\d+");
		pattern[3] = Pattern.compile("[eE]");
		pattern[4] = Pattern.compile("[-+]");
		pattern[5] = Pattern.compile("-");
		pattern[6] = Pattern.compile("-");
	}

	static final Processor normal = (stream, setState) -> {
		if (stream.eatWhile(whiteCharRE)) {
			return "";
		}
		String ch = stream.next();
		if (specialRE.matcher(ch).matches()) {
			if (ch.equals("{") && !stream.eat("-").isEmpty()) {
				String t = "comment";
				if (!stream.eat("#").isEmpty()) {
					t = "meta";
				}
				return switchState(stream, setState, ncomment(t, 1));
			}
			return "";
		}
		if (ch.equals("\'")) {
			if (!stream.eat("\\").isEmpty()) {
				stream.next();
			} else {
				stream.next();
			}
			if (!stream.eat("\'").isEmpty()) {
				return "string";
			}
			return "string error";
		}
		if (ch.equals("\"")) {
			return switchState(stream, setState, stringLiteral());
		}
		if (largeRE.matcher(ch).matches()) {
			stream.eatWhile(idRE);
			if (!stream.eat(".").isEmpty()) {
				return "qualifier";
			}
			return "variable-2";
		}
		if (smallRE.matcher(ch).matches()) {
			stream.eatWhile(idRE);
			return "variable";
		}
		if (digitRE.matcher(ch).matches()) {
			if (ch.equals("0")) {
				if (!stream.eat(pattern[0]).isEmpty()) {
					stream.eatWhile(hexitRE);
					return "integer";
				}
				if (!stream.eat(pattern[1]).isEmpty()) {
					stream.eatWhile(octitRE);
					return "number";
				}
			}
			stream.eatWhile(digitRE);
			String t = "number";
			if (!stream.match(pattern[2]).isEmpty()) {
				t = "number";
			}
			if (!stream.eat(pattern[3]).isEmpty()) {
				t = "number";
				stream.eat(pattern[4]);
				stream.eatWhile(digitRE);
			}
			return t;
		}
		if (ch.equals(".") && !stream.eat(".").isEmpty())
			return "keyword";
		if (symbolRE.matcher(ch).matches()) {
			if (ch.equals("-") && !stream.eat(pattern[5]).isEmpty()) {
				stream.eatWhile(pattern[6]);
				if (!!stream.eat(symbolRE).isEmpty()) {
					stream.skipToEnd();
					return "comment";
				}
			}
			String t = "variable";
			if (ch.equals(":")) {
				t = "variable-2";
			}
			stream.eatWhile(symbolRE);
			return t;
		}
		return "error";
	};

	static Processor ncomment(String type, int nest) {
		if (nest == 0) {
			return normal;
		}
		return (stream, setState) -> {
			int currNest = nest;
			while (!stream.eol()) {
				String ch = stream.next();
				if (ch.equals("{") && !stream.eat("-").isEmpty()) {
					++currNest;
				} else if (ch.equals("-") && !stream.eat("}").isEmpty()) {
					--currNest;
					if (currNest == 0) {
						setState.setState(normal);
						return type;
					}
				}
			}
			setState.setState(ncomment(type, currNest));
			return type;
		};
	}

	static Processor stringLiteral() {
		return (stream, setState) -> {
			while (!stream.eol()) {
				String ch = stream.next();
				if (ch.equals("\"")) {
					setState.setState(normal);
					return "string";
				}
				if (ch.equals("\\")) {
					if (stream.eol() || !stream.eat(whiteCharRE).isEmpty()) {
						setState.setState(stringGap());
						return "string";
					}
					if (!stream.eat("&").isEmpty()) {
					} else {
						stream.next();
					}
				}
			}
			setState.setState(normal);
			return "string error";
		};
	}

	static Processor stringGap() {
		return (stream, setState) -> {
			if (!stream.eat("\\").isEmpty()) {
				return switchState(stream, setState, stringLiteral());
			}
			stream.next();
			setState.setState(normal);
			return "error";
		};
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "hs");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-haskell");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("haskell");
	}

	@Override
	public State startState() {
		return new State(normal);
	}

	@Override
	public String token(StringStream stream, State state) {
		String t = state.f.process(stream, new SetState() {
			@Override
			public void setState(Processor f) {
				state.f = f;
			}
		});
		String w = stream.current();
		return wellKnownWords.containsKey(w) ? wellKnownWords.get(w) : t;
	}
}
