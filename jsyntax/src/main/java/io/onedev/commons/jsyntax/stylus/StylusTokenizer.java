package io.onedev.commons.jsyntax.stylus;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.TokenizerUtils;

public class StylusTokenizer extends AbstractTokenizer<StylusTokenizer.State> {

	static Pattern[] tokenBasePattern = new Pattern[19];
	static {
		tokenBasePattern[0] = Pattern.compile(
				"(^[\\w-]+\\s*=\\s*$)|(^\\s*[\\w-]+\\s*=\\s*[\\w-])|(^\\s*(\\.|#|@|\\$|\\&|\\[|\\d|\\+|::?|\\{|\\>|~|\\/)?\\s*[\\w-]*([a-z0-9-]|\\*|\\/\\*)(\\(|,)?)");
		tokenBasePattern[1] = Pattern.compile("^\\s*");
		tokenBasePattern[2] = Pattern.compile("[\\w\\-]");
		tokenBasePattern[3] = Pattern.compile("^[0-9a-f]{6}|[0-9a-f]{3}", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[4] = Pattern.compile("^[a-z][\\w-]*", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[5] = Pattern.compile("^-?[0-9]?\\.?[0-9]");
		tokenBasePattern[6] = Pattern.compile("[a-z%]", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[7] = Pattern.compile("^(important|optional)", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[8] = Pattern.compile("^\\.[a-z][\\w-]*", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[9] = Pattern.compile("^[a-z][\\w-]*\\(", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[10] = Pattern.compile("^(\\+|-)[a-z][\\w-]*\\(", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[11] = Pattern.compile("^\\s*&", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[12] = Pattern.compile("^[-_]+[a-z][\\w-]*");
		tokenBasePattern[13] = Pattern.compile("^(\\/|&)(-|_|:|\\.|#|[a-z])");
		tokenBasePattern[14] = Pattern.compile("^&{1}\\s*$");
		tokenBasePattern[15] = Pattern.compile("^\\$?[-_]*[a-z0-9]+[\\w-]*", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[16] = Pattern.compile("^(\\.|\\[)[\\w-\'\"\\]]+", Pattern.CASE_INSENSITIVE);
		tokenBasePattern[17] = Pattern.compile("\\.");
		tokenBasePattern[18] = Pattern.compile("[:;,{}\\[\\]\\(\\)]");
	}

	String tokenBase(StringStream stream, State state) {

		int posCopy = stream.pos();
		int startCopy = stream.start();
		int linestartCopy = stream.lineStart();

		List<String> firstWordMatch = stream.match(tokenBasePattern[0]);
		state.context.line.firstWord = !firstWordMatch.isEmpty() ? firstWordMatch.get(0).replaceAll("^\\s*", "") : "";
		state.context.line.indent = stream.indentation();

		stream.pos(posCopy);
		stream.start(startCopy);
		stream.lineStart(linestartCopy);
		String ch = stream.peek();
		// Line comment
		if (stream.match("//")) {
			stream.skipToEnd();
			return "comment comment";
		}
		// Block comment
		if (stream.match("/*")) {
			state.tokenize = new TokenCComment();
			return new TokenCComment().process(stream, state);
		}
		// String
		if (ch.equals("\"") || ch.equals("'")) {
			stream.next();
			state.tokenize = new TokenString(ch);
			return state.tokenize.process(stream, state);
		}
		// Def
		if (ch.equals("@")) {
			stream.next();
			stream.eatWhile(tokenBasePattern[2]);
			return "def" + " " + stream.current();
		}
		// ID selector or Hex color
		if (ch.equals("#")) {
			stream.next();
			// Hex color
			if (!stream.match(tokenBasePattern[3]).isEmpty()) {
				return "atom atom";
			}
			// ID selector
			if (!stream.match(tokenBasePattern[4]).isEmpty()) {
				return "builtin hash";
			}
		}
		// Vendor prefixes
		if (!stream.match(vendorPrefixesRegexp).isEmpty()) {
			return "meta vendor-prefixes";
		}
		// Numbers
		if (!stream.match(tokenBasePattern[5]).isEmpty()) {
			stream.eatWhile(tokenBasePattern[6]);
			return "number unit";
		}
		// !important|optional
		if (ch.equals("!")) {
			stream.next();
			if (!stream.match(tokenBasePattern[7]).isEmpty()) {
				return "keyword important";
			} else {
				return "operator important";
			}
		}
		// Class
		if (ch.equals(".") && !stream.match(tokenBasePattern[8]).isEmpty()) {
			return "qualifier qualifier";
		}
		// url url-prefix domain regexp
		if (!stream.match(documentTypesRegexp).isEmpty()) {
			if (stream.peek().equals("("))
				state.tokenize = new TokenParenthesized();
			return "property word";
		}
		// Mixins / Functions
		if (!stream.match(tokenBasePattern[9]).isEmpty()) {
			stream.backUp(1);
			return "keyword mixin";
		}
		// Block mixins
		if (!stream.match(tokenBasePattern[10]).isEmpty()) {
			stream.backUp(1);
			return "keyword block-mixin";
		}
		// Parent Reference BEM naming
		if (tokenBasePattern[11].matcher(stream.string()).find() && !stream.match(tokenBasePattern[12]).isEmpty()) {
			return "qualifier qualifier";
		}
		// / Root Reference & Parent Reference
		if (!stream.match(tokenBasePattern[13]).isEmpty()) {
			stream.backUp(1);
			return "variable-3 reference";
		}
		if (!stream.match(tokenBasePattern[14]).isEmpty()) {
			return "variable-3 reference";
		}
		// Word operator
		if (!stream.match(wordOperatorKeywordsRegexp).isEmpty()) {
			return "operator operator";
		}
		// Word
		if (!stream.match(tokenBasePattern[15]).isEmpty()) {
			// Variable
			if (!stream.match(tokenBasePattern[16], false).isEmpty()) {
				if (!wordIsTag(stream.current())) {
					stream.match(tokenBasePattern[17]);
					return "variable-2 variable-name";
				}
			}
			return "variable-2 word";
		}
		// Operators
		if (!stream.match(operatorsRegexp).isEmpty()) {
			return "operator" + " " + stream.current();
		}
		// Delimiters
		if (tokenBasePattern[18].matcher(ch).find()) {
			stream.next();
			return "" + " " + ch;
		}
		// Non-detected items
		stream.next();
		return "" + " " + "";
	}

	/**
	 * Token comment
	 */
	static class TokenCComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (maybeEnd && ch.equals("/")) {
					state.tokenize = null;
					break;
				}
				maybeEnd = (ch.equals("*"));
			}
			return "comment comment";
		}
	}

	/**
	 * Token string
	 */
	class TokenString implements Processor {

		private final String quote;

		public TokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state) {
			boolean escaped = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals(quote) && !escaped) {
					if (quote.equals(")"))
						stream.backUp(1);
					break;
				}
				escaped = !escaped && ch.equals("\\");
			}
			if (ch.equals(quote) || !escaped && !quote.equals(")"))
				state.tokenize = null;
			return "string string";
		}

	}

	/**
	 * Token parenthesized
	 */
	private static Pattern TOKEN_PARENTHESIZED_PATTERN = Pattern.compile("\\s*[\\\"\\')]");

	class TokenParenthesized implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			stream.next(); // Must be '('
			if (stream.match(TOKEN_PARENTHESIZED_PATTERN, false).isEmpty())
				state.tokenize = new TokenString(")");
			else
				state.tokenize = null;
			return "" + " " + "(";
		}
	}

	static class Line {
		String firstWord;
		int indent;

		Line(String firstWord, int indent) {
			this.firstWord = firstWord;
			this.indent = indent;
		}
	}

	/**
	 * Context management
	 */
	static class Context {
		StateTransition type;
		int indent;
		Context prev;
		Line line;

		Context(StateTransition type, int indent, Context prev, Line line) {
			this.type = type;
			this.indent = indent;
			this.prev = prev;
			this.line = new Line("", 0);
		}
	}

	private StateTransition pushContext(State state, StringStream stream, StateTransition type, boolean indent) {
		state.context = new Context(type, stream.indentation() + (indent ? indentUnit() : 0), state.context, null);
		return type;
	}

	private StateTransition pushContext(State state, StringStream stream, StateTransition type) {
		return pushContext(state, stream, type, true);
	}

	private StateTransition popContext(State state, Boolean currentIndent) {
		int contextIndent = state.context.indent - indentUnit();
		currentIndent = currentIndent || false;
		state.context = state.context.prev;
		if (currentIndent) {
			state.context.indent = contextIndent;
		}
		return state.context.type;
	}

	private StateTransition popContext(State state) {
		return popContext(state, true);
	}

	static StateTransition pass(String type, StringStream stream, State state) {
		return state.context.type.transition(type, stream, state);
	}

	static StateTransition popAndPass(String type, StringStream stream, State state) {
		return popAndPass(type, stream, state, 1);
	}

	static StateTransition popAndPass(String type, StringStream stream, State state, int n) {
		for (int i = n; i > 0; i--)
			state.context = state.context.prev;
		return pass(type, stream, state);
	}

	/**
	 * Parser
	 */
	private Boolean wordIsTag(String word) {
		return tagKeywords.contains(word);
	}

	private Boolean wordIsProperty(String word) {
		word = word.toLowerCase();
		return propertyKeywords.contains(word) || fontProperties.contains(word);
	}

	private Boolean wordIsBlock(String word) {
		return blockKeywords.contains(word.toLowerCase());
	}

	private String wordAsValue(String word) {
		String wordLC = word.toLowerCase();
		String override = "variable-2";
		if (wordIsTag(word)) {
			override = "tag";
		} else if (wordIsBlock(word)) {
			override = "block-keyword";
		} else if (wordIsProperty(word)) {
			override = "property";
		} else if (valueKeywords.contains(wordLC) || commonAtoms.contains(wordLC)) {
			override = "atom";
		} else if (wordLC.equals("return") || colorKeywords.contains(wordLC)) {
			override = "keyword";
		}
		// Font family
		else if (Pattern.compile("^[A-Z]").matcher(word).find()) {
			override = "string";
		}
		return override;
	}

	private Boolean typeIsBlock(String type, StringStream stream, State state) {

		return ((endOfLine(stream)
				&& (type.equals("{") || type.equals("]") || type.equals("hash") || type.equals("qualifier")))
				|| type.equals("block-mixin"));
	}

	private Boolean typeIsInterpolation(String type, StringStream stream) {

		return type.equals("{")
				&& !stream.match(Pattern.compile("^\\s*\\$?[\\w-]+", Pattern.CASE_INSENSITIVE), false).isEmpty();
	}

	private Boolean typeIsPseudo(String type, StringStream stream) {

		return type.equals(":") && !stream.match(Pattern.compile("^[a-z-]+"), false).isEmpty();
	}

	private Boolean startOfLine(StringStream stream) {

		String text = escapeRegExp(stream.current());
		Pattern p = Pattern.compile("^\\s*" + text);
		return stream.sol() || p.matcher(stream.string()).find();
	}

	private Boolean endOfLine(StringStream stream) {
		return stream.eol() || !stream.match(Pattern.compile("^\\s*$"), false).isEmpty();
	}

	private String firstWordOfLine1(StringStream line) {
		int posCopy = line.pos();
		int startCopy = line.start();
		int linestartCopy = line.lineStart();
		Pattern re = Pattern.compile("^\\s*[-_]*[a-z0-9]+[\\w-]*", Pattern.CASE_INSENSITIVE);
		List<String> result = line.match(re);
		line.pos(posCopy);
		line.start(startCopy);
		line.lineStart(linestartCopy);
		return !result.isEmpty() ? result.get(0).replace("^\\s*", "") : "";
	}

	private String firstWordOfLine2(StringStream line) {
		Pattern re = Pattern.compile("^\\s*[-_]*[a-z0-9]+[\\w-]*", Pattern.CASE_INSENSITIVE);
		List<String> result = line.match(re);
		return !result.isEmpty() ? result.get(0).replace("^\\s*", "") : "";
	}

	/**
	 * Block
	 */
	private static final Pattern States_Block_Pattern1 = Pattern.compile("^\\s*(\\.|#|:|\\[|\\*|&)");
	private static final Pattern States_Block_Pattern2 = Pattern.compile("^\\s?\\$[\\w-\\.\\[\\]\'\"]+$");
	private static final Pattern States_Block_Pattern3 = Pattern.compile("\\s*(,|\\.|#|\\[|:|\\{)");
	private static final Pattern States_Block_Pattern4 = Pattern
			.compile("@(font-face|media|supports|(-moz-)?document)");
	private static final Pattern States_Block_Pattern5 = Pattern.compile("@(-(moz|ms|o|webkit)-)?keyframes$");
	private static final Pattern States_Block_Pattern6 = Pattern.compile("@extends?");
	private static final Pattern States_Block_Pattern7 = Pattern.compile("(@import|@require|@charset)");
	private static final Pattern States_Block_Pattern8 = Pattern.compile("embed|menu|pre|progress|sub|table");
	private static final Pattern States_Block_Pattern9 = Pattern.compile("^(\\s*\\.|#|\\&|\\[|\\/|>|\\*)");
	private static final Pattern States_Block_Pattern10 = Pattern.compile("=");
	private static final Pattern States_Block_Pattern11 = Pattern.compile("(if|unless)");
	private static final Pattern States_Block_Pattern12 = Pattern.compile("^\\s?\\$[\\w-\\.\\[\\]\'\"]+$");

	class StatesBlock implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {

			if ((type.equals("comment") && startOfLine(stream)) || (type.equals(",") && endOfLine(stream))
					|| type.equals("mixin")) {
				return pushContext(state, stream, new StatesBlock(), false);
			}
			if (typeIsInterpolation(type, stream)) {
				return pushContext(state, stream, new StatesInterpolation());
			}
			if (endOfLine(stream) && type.equals("]")) {
				if (!States_Block_Pattern1.matcher(stream.string()).find() && !wordIsTag(firstWordOfLine1(stream))) {
					return pushContext(state, stream, new StatesBlock(), false);
				}
			}
			if (typeIsBlock(type, stream, state)) {
				return pushContext(state, stream, new StatesBlock());
			}
			if (type.equals("}") && endOfLine(stream)) {
				return pushContext(state, stream, new StatesBlock(), false);
			}
			if (type.equals("variable-name")) {
				if (States_Block_Pattern2.matcher(stream.string()).find() || wordIsBlock(firstWordOfLine1(stream))) {
					return pushContext(state, stream, new StatesVariableName());
				} else {
					return pushContext(state, stream, new StatesVariableName(), false);
				}
			}
			if (type.equals("=")) {
				if (!endOfLine(stream) && !wordIsBlock(firstWordOfLine1(stream))) {
					return pushContext(state, stream, new StatesBlock(), false);
				}
				return pushContext(state, stream, new StatesBlock());
			}
			if (type.equals("*")) {
				if (endOfLine(stream) || !stream.match(States_Block_Pattern3, false).isEmpty()) {
					state.override = "tag";
					return pushContext(state, stream, new StatesBlock());
				}
			}
			if (typeIsPseudo(type, stream)) {
				return pushContext(state, stream, new StatesPseudo());
			}
			if (States_Block_Pattern4.matcher(type).find()) {
				return pushContext(state, stream, endOfLine(stream) ? new StatesBlock() : new StatesAtBlock());
			}
			if (States_Block_Pattern5.matcher(type).find()) {
				return pushContext(state, stream, new StatesKeyframes());
			}
			if (States_Block_Pattern6.matcher(type).find()) {
				return pushContext(state, stream, new StatesExtend(), false);
			}
			if (type.length() != 0 && type.charAt(0) == '@') {
				// Property Lookup
				if (stream.indentation() > 0 && wordIsProperty(TokenizerUtils.slice(stream.current(), 1))) {
					state.override = "variable-2";
					return new StatesBlock();
				}
				if (States_Block_Pattern7.matcher(type).find()) {
					return pushContext(state, stream, new StatesBlock(), false);
				}
				return pushContext(state, stream, new StatesBlock());
			}
			if (type.equals("reference") && endOfLine(stream)) {
				return pushContext(state, stream, new StatesBlock());
			}
			if (type.equals("(")) {
				return pushContext(state, stream, new StatesParens());
			}
			if (type.equals("vendor-prefixes")) {
				return pushContext(state, stream, new StatesVvendorPrefixes());
			}
			if (type.equals("word")) {
				String word = stream.current();
				state.override = wordAsValue(word);
				if (state.override.equals("property")) {
					if (startOfLine(stream)) {
						return pushContext(state, stream, new StatesBlock(), false);
					} else {
						state.override = "atom";
						return new StatesBlock();
					}
				}
				if (state.override.equals("tag")) {

					// tag is a css value
					if (States_Block_Pattern8.matcher(word).find()) {
						if (wordIsProperty(firstWordOfLine1(stream))) {
							state.override = "atom";
							return new StatesBlock();
						}
					}

					// tag is an attribute
					if ((Pattern.compile("\\[\\s*" + word + "|" + word + "\\s*\\]").matcher(stream.string()).find())) {
						state.override = "atom";
						return new StatesBlock();
					}

					// tag is a variable
					if (tagVariablesRegexp.matcher(word).find()) {
						if ((startOfLine(stream) && States_Block_Pattern10.matcher(stream.string()).find())
								|| (!startOfLine(stream) && !States_Block_Pattern9.matcher(stream.string()).find()
										&& !wordIsTag(firstWordOfLine1(stream)))) {
							state.override = "variable-2";
							if (wordIsBlock(firstWordOfLine1(stream)))
								return new StatesBlock();
							return pushContext(state, stream, new StatesBlock(), false);
						}
					}
					if (endOfLine(stream))
						return pushContext(state, stream, new StatesBlock());
				}
				if (state.override.equals("block-keyword")) {
					state.override = "keyword";

					// Postfix conditionals
					if (States_Block_Pattern11.matcher(stream.current()).find() && !startOfLine(stream)) {
						return new StatesBlock();
					}
					return pushContext(state, stream, new StatesBlock());
				}
				if (word.equals("return"))
					return pushContext(state, stream, new StatesBlock(), false);

				// Placeholder selector
				if (state.override.equals("variable-2") && States_Block_Pattern12.matcher(stream.string()).find()) {
					return pushContext(state, stream, new StatesBlock());
				}
			}
			return state.context.type;
		}
	}

	/**
	 * Parens
	 */
	private static final Pattern States_Parens_Pattern1 = Pattern.compile("^[a-z][\\w-]*\\(", Pattern.CASE_INSENSITIVE);
	private static final Pattern States_Parens_Pattern2 = Pattern.compile("(\\.|#|:|\\[|\\*|&|>|~|\\+|\\/)");
	private static final Pattern States_Parens_Pattern3 = Pattern.compile("^-?[a-z][\\w-\\.\\[\\]\\'\"]*\\s*=");
	private static final Pattern States_Parens_Pattern4 = Pattern.compile("^[\\$-]?[a-z][\\w-\\.\\[\\]\\'\"]*\\s*=");
	private static final Pattern States_Parens_Pattern5 = Pattern.compile("^\\s*(\\(|\\)|[0-9])");
	private static final Pattern States_Parens_Pattern6 = Pattern.compile("^\\s+[a-z][\\w-]*\\(",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern States_Parens_Pattern7 = Pattern.compile("^\\s+[\\$-]?[a-z]",
			Pattern.CASE_INSENSITIVE);

	class StatesParens implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {

			if (type.equals("(")) {
				return pushContext(state, stream, new StatesParens());
			}
			if (type.equals(")")) {
				if (state.context.prev.type instanceof StateTransition) {
					return popContext(state);
				}
				if ((States_Parens_Pattern1.matcher(stream.string()).find() && endOfLine(stream))
						|| wordIsBlock(firstWordOfLine1(stream))
						|| States_Parens_Pattern2.matcher(firstWordOfLine1(stream)).find()
						|| (!States_Parens_Pattern3.matcher(stream.string()).find()
								&& wordIsTag(firstWordOfLine1(stream)))) {
					return pushContext(state, stream, new StatesBlock());
				}
				if (States_Parens_Pattern4.matcher(stream.string()).find()
						|| States_Parens_Pattern5.matcher(stream.string()).find()
						|| States_Parens_Pattern6.matcher(stream.string()).find()
						|| States_Parens_Pattern7.matcher(stream.string()).find()) {
					return pushContext(state, stream, new StatesBlock(), false);
				}
				if (endOfLine(stream))
					return pushContext(state, stream, new StatesBlock());
				else
					return pushContext(state, stream, new StatesBlock(), false);
			}
			if (type.length() != 0 && type.charAt(0) == '@'
					&& wordIsProperty(TokenizerUtils.slice(stream.current(), 1))) {
				state.override = "variable-2";
			}
			if (type.equals("word")) {
				String word = stream.current();
				state.override = wordAsValue(word);
				if (state.override.equals("tag") && tagVariablesRegexp.matcher(word).find()) {
					state.override = "variable-2";
				}
				if (state.override.equals("property") || word.equals("to"))
					state.override = "atom";
			}
			if (type.equals("variable-name")) {
				return pushContext(state, stream, new StatesVariableName());
			}
			if (typeIsPseudo(type, stream)) {
				return pushContext(state, stream, new StatesPseudo());
			}
			return state.context.type;
		}
	}

	/**
	 * Vendor prefixes
	 */
	class StatesVvendorPrefixes implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {
			if (type.equals("word")) {
				state.override = "property";
				return pushContext(state, stream, new StatesBlock(), false);
			}
			return popContext(state);
		}
	}

	/**
	 * Pseudo
	 */
	class StatesPseudo implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {
			if (!wordIsProperty(firstWordOfLine2(stream))) {
				stream.match(Pattern.compile("^[a-z-]+"));
				state.override = "variable-3";
				if (endOfLine(stream))
					return pushContext(state, stream, new StatesBlock());
				return popContext(state);
			}
			return popAndPass(type, stream, state);
		}
	}

	/**
	 * atBlock
	 */
	class StatesAtBlock implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {

			if (type.equals("("))
				return pushContext(state, stream, new StatesAtBlockParens());
			if (typeIsBlock(type, stream, state)) {
				return pushContext(state, stream, new StatesBlock());
			}
			if (typeIsInterpolation(type, stream)) {
				return pushContext(state, stream, new StatesInterpolation());
			}
			if (type.equals("word")) {
				String word = stream.current().toLowerCase();
				if (Pattern.compile("^(only|not|and|or)$").matcher(word).find())
					state.override = "keyword";
				else if (documentTypes.contains(word))
					state.override = "tag";
				else if (mediaTypes.contains(word))
					state.override = "attribute";
				else if (mediaFeatures.contains(word))
					state.override = "property";
				else if (nonStandardPropertyKeywords.contains(word))
					state.override = "string-2";
				else
					state.override = wordAsValue(stream.current());
				if (state.override == "tag" && endOfLine(stream)) {
					return pushContext(state, stream, new StatesBlock());
				}
			}
			if (type.equals("operator") && Pattern.compile("^(not|and|or)$").matcher(stream.current()).find()) {
				state.override = "keyword";
			}
			return state.context.type;
		}
	}

	/**
	 * atBlock_parens
	 */
	class StatesAtBlockParens implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {

			if (type.equals("{") || type.equals("}")) {
				return state.context.type;
			}
			if (type.equals(")")) {
				if (endOfLine(stream)) {
					return pushContext(state, stream, new StatesBlock());
				} else {
					return pushContext(state, stream, new StatesAtBlock());
				}
			}
			if (type.equals("word")) {
				String word = stream.current().toLowerCase();
				state.override = wordAsValue(word);
				if (Pattern.compile("^(max|min)").matcher(word).find()) {
					state.override = "property";
				}
				if (state.override.equals("tag")) {
					if (tagVariablesRegexp.matcher(word).find()) {
						state.override = "variable-2";
					} else {
						state.override = "atom";
					}
				}
				return state.context.type;
			}
			return new StatesAtBlock().transition(type, stream, state);
		}
	}

	/**
	 * Keyframes
	 */
	class StatesKeyframes implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {
			if (stream.indentation() == 0 && ((type.equals("}") && startOfLine(stream)) || type.equals("]")
					|| type.equals("hash") || type.equals("qualifier") || wordIsTag(stream.current()))) {
				return popAndPass(type, stream, state);
			}
			if (type.equals("{"))
				return pushContext(state, stream, new StatesKeyframes());
			if (type.equals("}")) {
				if (startOfLine(stream))
					return popContext(state, true);
				else
					return pushContext(state, stream, new StatesKeyframes());
			}
			if (type.equals("unit") && Pattern.compile("^[0-9]+\\%$").matcher(stream.current()).find()) {
				return pushContext(state, stream, new StatesKeyframes());
			}

			if (type.equals("word")) {
				state.override = wordAsValue(stream.current());
				if (state.override.equals("block-keyword")) {
					state.override = "keyword";
					return pushContext(state, stream, new StatesKeyframes());
				}
			}
			if (Pattern.compile("@(font-face|media|supports|(-moz-)?document)").matcher(type).find()) {
				return pushContext(state, stream, endOfLine(stream) ? new StatesBlock() : new StatesAtBlock());
			}
			if (type.equals("mixin")) {
				return pushContext(state, stream, new StatesBlock(), false);
			}
			return state.context.type;
		}
	}

	/**
	 * Interpolation
	 */
	private static final Pattern States_Interpolation_Pattern1 = Pattern.compile("^\\s*(\\.|#|:|\\[|\\*|&|>|~|\\+|\\/)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern States_Interpolation_Pattern2 = Pattern.compile("^\\s*[a-z]",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern States_Interpolation_Pattern3 = Pattern.compile("^(\\{|\\s*\\&)");
	private static final Pattern States_Interpolation_Pattern4 = Pattern.compile("\\s*[\\w-]");

	class StatesInterpolation implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {

			if (type.equals("{")) {
				StateTransition result = popContext(state);
				if (result != null) {
					return pushContext(state, stream, new StatesBlock());
				} else {
					return null;
				}
			}
			if (type.equals("}")) {
				if (States_Interpolation_Pattern1.matcher(stream.string()).find()
						|| States_Interpolation_Pattern2.matcher(stream.string()).find()
								&& wordIsTag(firstWordOfLine1(stream))) {
					return pushContext(state, stream, new StatesBlock());
				}
				if (States_Interpolation_Pattern3.matcher(stream.string()).find()
						|| !stream.match(States_Interpolation_Pattern4, false).isEmpty()) {
					return pushContext(state, stream, new StatesBlock(), false);
				}
				return pushContext(state, stream, new StatesBlock());
			}
			if (type.equals("variable-name")) {
				return pushContext(state, stream, new StatesVariableName(), false);
			}
			if (type.equals("word")) {
				state.override = wordAsValue(stream.current());
				if (state.override.equals("tag")) {
					state.override = "atom";
				}
			}
			return state.context.type;
		}
	}

	/**
	 * Extend/s
	 */
	class StatesExtend implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {
			if (type.equals("[") || type.equals("=")) {
				return new StatesExtend();
			}
			if (type.equals("]")) {
				return popContext(state);
			}
			if (type.equals("word")) {
				state.override = wordAsValue(stream.current());
				return new StatesExtend();
			}
			return popContext(state);
		}
	}

	/**
	 * Variable name
	 */
	private static final Pattern States_VariableName_Pattern1 = Pattern.compile("^(\\.|\\$)");
	private static final Pattern States_VariableName_Pattern2 = Pattern.compile("^\\.[\\w-]+",
			Pattern.CASE_INSENSITIVE);

	class StatesVariableName implements StateTransition {

		@Override
		public StateTransition transition(String type, StringStream stream, State state) {
			if (type.equals("string") || type.equals("[") || type.equals("]")
					|| States_VariableName_Pattern1.matcher(stream.current()).find()) {
				if (States_VariableName_Pattern2.matcher(stream.current()).find()) {
					state.override = "variable-2";
				}
				return new StatesVariableName();
			}
			return popAndPass(type, stream, state);
		}
	}

	static class State {
		Processor tokenize;
		StateTransition state;
		Context context;
		String override;
		String type;

		State(Processor tokenize, StateTransition state, Context context) {
			this.tokenize = tokenize;
			this.state = state;
			this.context = context;
		}
	}

	@Override
	public boolean accept(String fileName) {

		return acceptExtensions(fileName, "styl");
	}

	@Override
	public State startState() {

		return new State(null, new StatesBlock(), new Context(new StatesBlock(), 0, null, null));
	}

	@Override
	public String token(StringStream stream, State state) {
		if (state.tokenize == null && stream.eatSpace())
			return "";
		String style;
		if (state.tokenize != null)
			style = state.tokenize.process(stream, state);
		else
			style = tokenBase(stream, state);

		if (style.contains(" ")) {
			state.type = StringUtils.substringAfter(style, " ");
			style = StringUtils.substringBefore(style, " ");
		}
		state.override = style;
		state.state = state.state.transition(state.type, stream, state);
		return state.override;
	}

	static final Set<String> tagKeywords = Sets.newHashSet("a", "abbr", "address", "area", "article", "aside", "audio",
			"b", "base", "bdi", "bdo", "bgsound", "blockquote", "body", "br", "button", "canvas", "caption", "cite",
			"code", "col", "colgroup", "data", "datalist", "dd", "del", "details", "dfn", "div", "dl", "dt", "em",
			"embed", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "head",
			"header", "hgroup", "hr", "html", "i", "iframe", "img", "input", "ins", "kbd", "keygen", "label", "legend",
			"li", "link", "main", "map", "mark", "marquee", "menu", "menuitem", "meta", "meter", "nav", "nobr",
			"noframes", "noscript", "object", "ol", "optgroup", "option", "output", "p", "param", "pre", "progress",
			"q", "rp", "rt", "ruby", "s", "samp", "script", "section", "select", "small", "source", "span", "strong",
			"style", "sub", "summary", "sup", "table", "tbody", "td", "textarea", "tfoot", "th", "thead", "time", "tr",
			"track", "u", "ul", "var", "video");

	static final Pattern tagVariablesRegexp = Pattern.compile("^(a|b|i|s|col|em)$", Pattern.CASE_INSENSITIVE);

	static final Set<String> propertyKeywords = Sets.newHashSet("align-content", "align-items", "align-self",
			"alignment-adjust", "alignment-baseline", "anchor-point", "animation", "animation-delay",
			"animation-direction", "animation-duration", "animation-fill-mode", "animation-iteration-count",
			"animation-name", "animation-play-state", "animation-timing-function", "appearance", "azimuth",
			"backface-visibility", "background", "background-attachment", "background-clip", "background-color",
			"background-image", "background-origin", "background-position", "background-repeat", "background-size",
			"baseline-shift", "binding", "bleed", "bookmark-label", "bookmark-level", "bookmark-state",
			"bookmark-target", "border", "border-bottom", "border-bottom-color", "border-bottom-left-radius",
			"border-bottom-right-radius", "border-bottom-style", "border-bottom-width", "border-collapse",
			"border-color", "border-image", "border-image-outset", "border-image-repeat", "border-image-slice",
			"border-image-source", "border-image-width", "border-left", "border-left-color", "border-left-style",
			"border-left-width", "border-radius", "border-right", "border-right-color", "border-right-style",
			"border-right-width", "border-spacing", "border-style", "border-top", "border-top-color",
			"border-top-left-radius", "border-top-right-radius", "border-top-style", "border-top-width", "border-width",
			"bottom", "box-decoration-break", "box-shadow", "box-sizing", "break-after", "break-before", "break-inside",
			"caption-side", "clear", "clip", "color", "color-profile", "column-count", "column-fill", "column-gap",
			"column-rule", "column-rule-color", "column-rule-style", "column-rule-width", "column-span", "column-width",
			"columns", "content", "counter-increment", "counter-reset", "crop", "cue", "cue-after", "cue-before",
			"cursor", "direction", "display", "dominant-baseline", "drop-initial-after-adjust",
			"drop-initial-after-align", "drop-initial-before-adjust", "drop-initial-before-align", "drop-initial-size",
			"drop-initial-value", "elevation", "empty-cells", "fit", "fit-position", "flex", "flex-basis",
			"flex-direction", "flex-flow", "flex-grow", "flex-shrink", "flex-wrap", "float", "float-offset",
			"flow-from", "flow-into", "font", "font-feature-settings", "font-family", "font-kerning",
			"font-language-override", "font-size", "font-size-adjust", "font-stretch", "font-style", "font-synthesis",
			"font-variant", "font-variant-alternates", "font-variant-caps", "font-variant-east-asian",
			"font-variant-ligatures", "font-variant-numeric", "font-variant-position", "font-weight", "grid",
			"grid-area", "grid-auto-columns", "grid-auto-flow", "grid-auto-position", "grid-auto-rows", "grid-column",
			"grid-column-end", "grid-column-start", "grid-row", "grid-row-end", "grid-row-start", "grid-template",
			"grid-template-areas", "grid-template-columns", "grid-template-rows", "hanging-punctuation", "height",
			"hyphens", "icon", "image-orientation", "image-rendering", "image-resolution", "inline-box-align",
			"justify-content", "left", "letter-spacing", "line-break", "line-height", "line-stacking",
			"line-stacking-ruby", "line-stacking-shift", "line-stacking-strategy", "list-style", "list-style-image",
			"list-style-position", "list-style-type", "margin", "margin-bottom", "margin-left", "margin-right",
			"margin-top", "marker-offset", "marks", "marquee-direction", "marquee-loop", "marquee-play-count",
			"marquee-speed", "marquee-style", "max-height", "max-width", "min-height", "min-width", "move-to",
			"nav-down", "nav-index", "nav-left", "nav-right", "nav-up", "object-fit", "object-position", "opacity",
			"order", "orphans", "outline", "outline-color", "outline-offset", "outline-style", "outline-width",
			"overflow", "overflow-style", "overflow-wrap", "overflow-x", "overflow-y", "padding", "padding-bottom",
			"padding-left", "padding-right", "padding-top", "page", "page-break-after", "page-break-before",
			"page-break-inside", "page-policy", "pause", "pause-after", "pause-before", "perspective",
			"perspective-origin", "pitch", "pitch-range", "play-during", "position", "presentation-level",
			"punctuation-trim", "quotes", "region-break-after", "region-break-before", "region-break-inside",
			"region-fragment", "rendering-intent", "resize", "rest", "rest-after", "rest-before", "richness", "right",
			"rotation", "rotation-point", "ruby-align", "ruby-overhang", "ruby-position", "ruby-span",
			"shape-image-threshold", "shape-inside", "shape-margin", "shape-outside", "size", "speak", "speak-as",
			"speak-header", "speak-numeral", "speak-punctuation", "speech-rate", "stress", "string-set", "tab-size",
			"table-layout", "target", "target-name", "target-new", "target-position", "text-align", "text-align-last",
			"text-decoration", "text-decoration-color", "text-decoration-line", "text-decoration-skip",
			"text-decoration-style", "text-emphasis", "text-emphasis-color", "text-emphasis-position",
			"text-emphasis-style", "text-height", "text-indent", "text-justify", "text-outline", "text-overflow",
			"text-shadow", "text-size-adjust", "text-space-collapse", "text-transform", "text-underline-position",
			"text-wrap", "top", "transform", "transform-origin", "transform-style", "transition", "transition-delay",
			"transition-duration", "transition-property", "transition-timing-function", "unicode-bidi",
			"vertical-align", "visibility", "voice-balance", "voice-duration", "voice-family", "voice-pitch",
			"voice-range", "voice-rate", "voice-stress", "voice-volume", "volume", "white-space", "widows", "width",
			"word-break", "word-spacing", "word-wrap", "z-index", "clip-path", "clip-rule", "mask", "enable-background",
			"filter", "flood-color", "flood-opacity", "lighting-color", "stop-color", "stop-opacity", "pointer-events",
			"color-interpolation", "color-interpolation-filters", "color-rendering", "fill", "fill-opacity",
			"fill-rule", "image-rendering", "marker", "marker-end", "marker-mid", "marker-start", "shape-rendering",
			"stroke", "stroke-dasharray", "stroke-dashoffset", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit",
			"stroke-opacity", "stroke-width", "text-rendering", "baseline-shift", "dominant-baseline",
			"glyph-orientation-horizontal", "glyph-orientation-vertical", "text-anchor", "writing-mode",
			"font-smoothing", "osx-font-smoothing");

	static final Set<String> nonStandardPropertyKeywords = Sets.newHashSet("scrollbar-arrow-color",
			"scrollbar-base-color", "scrollbar-dark-shadow-color", "scrollbar-face-color", "scrollbar-highlight-color",
			"scrollbar-shadow-color", "scrollbar-3d-light-color", "scrollbar-track-color", "shape-inside",
			"searchfield-cancel-button", "searchfield-decoration", "searchfield-results-button",
			"searchfield-results-decoration", "zoom");

	static final Set<String> valueKeywords = Sets.newHashSet("above", "absolute", "activeborder", "additive",
			"activecaption", "afar", "after-white-space", "ahead", "alias", "all", "all-scroll", "alphabetic",
			"alternate", "always", "amharic", "amharic-abegede", "antialiased", "appworkspace", "arabic-indic",
			"armenian", "asterisks", "attr", "auto", "avoid", "avoid-column", "avoid-page", "avoid-region",
			"background", "backwards", "baseline", "below", "bidi-override", "binary", "bengali", "blink", "block",
			"block-axis", "bold", "bolder", "border", "border-box", "both", "bottom", "break", "break-all",
			"break-word", "bullets", "button", "button-bevel", "buttonface", "buttonhighlight", "buttonshadow",
			"buttontext", "calc", "cambodian", "capitalize", "caps-lock-indicator", "caption", "captiontext", "caret",
			"cell", "center", "checkbox", "circle", "cjk-decimal", "cjk-earthly-branch", "cjk-heavenly-stem",
			"cjk-ideographic", "clear", "clip", "close-quote", "col-resize", "collapse", "column", "compact",
			"condensed", "contain", "content", "content-box", "context-menu", "continuous", "copy", "counter",
			"counters", "cover", "crop", "cross", "crosshair", "currentcolor", "cursive", "cyclic", "dashed", "decimal",
			"decimal-leading-zero", "default", "default-button", "destination-atop", "destination-in",
			"destination-out", "destination-over", "devanagari", "disc", "discard", "disclosure-closed",
			"disclosure-open", "document", "dot-dash", "dot-dot-dash", "dotted", "double", "down", "e-resize", "ease",
			"ease-in", "ease-in-out", "ease-out", "element", "ellipse", "ellipsis", "embed", "end", "ethiopic",
			"ethiopic-abegede", "ethiopic-abegede-am-et", "ethiopic-abegede-gez", "ethiopic-abegede-ti-er",
			"ethiopic-abegede-ti-et", "ethiopic-halehame-aa-er", "ethiopic-halehame-aa-et", "ethiopic-halehame-am-et",
			"ethiopic-halehame-gez", "ethiopic-halehame-om-et", "ethiopic-halehame-sid-et", "ethiopic-halehame-so-et",
			"ethiopic-halehame-ti-er", "ethiopic-halehame-ti-et", "ethiopic-halehame-tig", "ethiopic-numeric",
			"ew-resize", "expanded", "extends", "extra-condensed", "extra-expanded", "fantasy", "fast", "fill", "fixed",
			"flat", "flex", "footnotes", "forwards", "from", "geometricPrecision", "georgian", "graytext", "groove",
			"gujarati", "gurmukhi", "hand", "hangul", "hangul-consonant", "hebrew", "help", "hidden", "hide", "higher",
			"highlight", "highlighttext", "hiragana", "hiragana-iroha", "horizontal", "hsl", "hsla", "icon", "ignore",
			"inactiveborder", "inactivecaption", "inactivecaptiontext", "infinite", "infobackground", "infotext",
			"inherit", "initial", "inline", "inline-axis", "inline-block", "inline-flex", "inline-table", "inset",
			"inside", "intrinsic", "invert", "italic", "japanese-formal", "japanese-informal", "justify", "kannada",
			"katakana", "katakana-iroha", "keep-all", "khmer", "korean-hangul-formal", "korean-hanja-formal",
			"korean-hanja-informal", "landscape", "lao", "large", "larger", "left", "level", "lighter", "line-through",
			"linear", "linear-gradient", "lines", "list-item", "listbox", "listitem", "local", "logical", "loud",
			"lower", "lower-alpha", "lower-armenian", "lower-greek", "lower-hexadecimal", "lower-latin",
			"lower-norwegian", "lower-roman", "lowercase", "ltr", "malayalam", "match", "matrix", "matrix3d",
			"media-controls-background", "media-current-time-display", "media-fullscreen-button", "media-mute-button",
			"media-play-button", "media-return-to-realtime-button", "media-rewind-button", "media-seek-back-button",
			"media-seek-forward-button", "media-slider", "media-sliderthumb", "media-time-remaining-display",
			"media-volume-slider", "media-volume-slider-container", "media-volume-sliderthumb", "medium", "menu",
			"menulist", "menulist-button", "menulist-text", "menulist-textfield", "menutext", "message-box", "middle",
			"min-intrinsic", "mix", "mongolian", "monospace", "move", "multiple", "myanmar", "n-resize", "narrower",
			"ne-resize", "nesw-resize", "no-close-quote", "no-drop", "no-open-quote", "no-repeat", "none", "normal",
			"not-allowed", "nowrap", "ns-resize", "numbers", "numeric", "nw-resize", "nwse-resize", "oblique", "octal",
			"open-quote", "optimizeLegibility", "optimizeSpeed", "oriya", "oromo", "outset", "outside", "outside-shape",
			"overlay", "overline", "padding", "padding-box", "painted", "page", "paused", "persian", "perspective",
			"plus-darker", "plus-lighter", "pointer", "polygon", "portrait", "pre", "pre-line", "pre-wrap",
			"preserve-3d", "progress", "push-button", "radial-gradient", "radio", "read-only", "read-write",
			"read-write-plaintext-only", "rectangle", "region", "relative", "repeat", "repeating-linear-gradient",
			"repeating-radial-gradient", "repeat-x", "repeat-y", "reset", "reverse", "rgb", "rgba", "ridge", "right",
			"rotate", "rotate3d", "rotateX", "rotateY", "rotateZ", "round", "row-resize", "rtl", "run-in", "running",
			"s-resize", "sans-serif", "scale", "scale3d", "scaleX", "scaleY", "scaleZ", "scroll", "scrollbar",
			"se-resize", "searchfield", "searchfield-cancel-button", "searchfield-decoration",
			"searchfield-results-button", "searchfield-results-decoration", "semi-condensed", "semi-expanded",
			"separate", "serif", "show", "sidama", "simp-chinese-formal", "simp-chinese-informal", "single", "skew",
			"skewX", "skewY", "skip-white-space", "slide", "slider-horizontal", "slider-vertical",
			"sliderthumb-horizontal", "sliderthumb-vertical", "slow", "small", "small-caps", "small-caption", "smaller",
			"solid", "somali", "source-atop", "source-in", "source-out", "source-over", "space", "spell-out", "square",
			"square-button", "start", "static", "status-bar", "stretch", "stroke", "sub", "subpixel-antialiased",
			"super", "sw-resize", "symbolic", "symbols", "table", "table-caption", "table-cell", "table-column",
			"table-column-group", "table-footer-group", "table-header-group", "table-row", "table-row-group", "tamil",
			"telugu", "text", "text-bottom", "text-top", "textarea", "textfield", "thai", "thick", "thin",
			"threeddarkshadow", "threedface", "threedhighlight", "threedlightshadow", "threedshadow", "tibetan",
			"tigre", "tigrinya-er", "tigrinya-er-abegede", "tigrinya-et", "tigrinya-et-abegede", "to", "top",
			"trad-chinese-formal", "trad-chinese-informal", "translate", "translate3d", "translateX", "translateY",
			"translateZ", "transparent", "ultra-condensed", "ultra-expanded", "underline", "up", "upper-alpha",
			"upper-armenian", "upper-greek", "upper-hexadecimal", "upper-latin", "upper-norwegian", "upper-roman",
			"uppercase", "urdu", "url", "var", "vertical", "vertical-text", "visible", "visibleFill", "visiblePainted",
			"visibleStroke", "visual", "w-resize", "wait", "wave", "wider", "window", "windowframe", "windowtext",
			"words", "x-large", "x-small", "xor", "xx-large", "xx-small", "bicubic", "optimizespeed", "grayscale",
			"row", "row-reverse", "wrap", "wrap-reverse", "column-reverse", "flex-start", "flex-end", "space-between",
			"space-around");

	static final Set<String> colorKeywords = Sets.newHashSet("aliceblue", "antiquewhite", "aqua", "aquamarine", "azure",
			"beige", "bisque", "black", "blanchedalmond", "blue", "blueviolet", "brown", "burlywood", "cadetblue",
			"chartreuse", "chocolate", "coral", "cornflowerblue", "cornsilk", "crimson", "cyan", "darkblue", "darkcyan",
			"darkgoldenrod", "darkgray", "darkgreen", "darkkhaki", "darkmagenta", "darkolivegreen", "darkorange",
			"darkorchid", "darkred", "darksalmon", "darkseagreen", "darkslateblue", "darkslategray", "darkturquoise",
			"darkviolet", "deeppink", "deepskyblue", "dimgray", "dodgerblue", "firebrick", "floralwhite", "forestgreen",
			"fuchsia", "gainsboro", "ghostwhite", "gold", "goldenrod", "gray", "grey", "green", "greenyellow",
			"honeydew", "hotpink", "indianred", "indigo", "ivory", "khaki", "lavender", "lavenderblush", "lawngreen",
			"lemonchiffon", "lightblue", "lightcoral", "lightcyan", "lightgoldenrodyellow", "lightgray", "lightgreen",
			"lightpink", "lightsalmon", "lightseagreen", "lightskyblue", "lightslategray", "lightsteelblue",
			"lightyellow", "lime", "limegreen", "linen", "magenta", "maroon", "mediumaquamarine", "mediumblue",
			"mediumorchid", "mediumpurple", "mediumseagreen", "mediumslateblue", "mediumspringgreen", "mediumturquoise",
			"mediumvioletred", "midnightblue", "mintcream", "mistyrose", "moccasin", "navajowhite", "navy", "oldlace",
			"olive", "olivedrab", "orange", "orangered", "orchid", "palegoldenrod", "palegreen", "paleturquoise",
			"palevioletred", "papayawhip", "peachpuff", "peru", "pink", "plum", "powderblue", "purple", "rebeccapurple",
			"red", "rosybrown", "royalblue", "saddlebrown", "salmon", "sandybrown", "seagreen", "seashell", "sienna",
			"silver", "skyblue", "slateblue", "slategray", "snow", "springgreen", "steelblue", "tan", "teal", "thistle",
			"tomato", "turquoise", "violet", "wheat", "white", "whitesmoke", "yellow", "yellowgreen");

	static final Set<String> documentTypes = Sets.newHashSet("domain", "regexp", "url", "url-prefix");

	static final Pattern documentTypesRegexp = wordRegexp(documentTypes);

	static final Set<String> mediaFeatures = Sets.newHashSet("width", "min-width", "max-width", "height", "min-height",
			"max-height", "device-width", "min-device-width", "max-device-width", "device-height", "min-device-height",
			"max-device-height", "aspect-ratio", "min-aspect-ratio", "max-aspect-ratio", "device-aspect-ratio",
			"min-device-aspect-ratio", "max-device-aspect-ratio", "color", "min-color", "max-color", "color-index",
			"min-color-index", "max-color-index", "monochrome", "min-monochrome", "max-monochrome", "resolution",
			"min-resolution", "max-resolution", "scan", "grid");

	static final Set<String> mediaTypes = Sets.newHashSet("all", "aural", "braille", "handheld", "print", "projection",
			"screen", "tty", "tv", "embossed");

	static final Set<String> fontProperties = Sets.newHashSet("font-family", "src", "unicode-range", "font-variant",
			"font-feature-settings", "font-stretch", "font-weight", "font-style");

	static final Pattern operatorsRegexp = Pattern
			.compile("^\\s*([.]{2,3}|&&|\\|\\||\\*\\*|[?!=:]?=|[-+*\\/%<>]=?|\\?:|\\~)");

	static final Set<String> wordOperatorKeywords = Sets.newHashSet("in", "and", "or", "not", "is not", "is a", "is",
			"isnt", "defined", "if unless");

	static final Pattern wordOperatorKeywordsRegexp = wordRegexp(wordOperatorKeywords);

	static final Set<String> blockKeywords = Sets.newHashSet("for", "if", "else", "unless", "from", "to");

	static final Set<String> commonAtoms = Sets.newHashSet("null", "true", "false", "href", "title", "type",
			"not-allowed", "readonly", "disabled");

	static final Set<String> commonDef = Sets.newHashSet("@font-face", "@keyframes", "@media", "@viewport", "@page",
			"@host", "@supports", "@block", "@css");

	static final Pattern vendorPrefixesRegexp = Pattern.compile("^\\-(moz|ms|o|webkit)-", Pattern.CASE_INSENSITIVE);

	static Pattern wordRegexp(Set<String> words) {
		return Pattern.compile("^((" + StringUtils.join(words, ")|(") + "))\\b");
	}

	static String escapeRegExp(String text) {
		return Pattern.quote(text);
	}

	static interface StateTransition {
		StateTransition transition(String type, StringStream stream, State state);
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-styl");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("stylus");
	}
}
