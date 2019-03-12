package io.onedev.commons.jsyntax.python;

import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public abstract class AbstractPythonTokenizer extends AbstractTokenizer<AbstractPythonTokenizer.State> {

	static Set<String> commonKeywords = Sets.newHashSet("as", "assert", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import",
            "lambda", "pass", "raise", "return", "try", "while", "with", "yield", "in");
	
	static Set<String> commonBuiltins = Sets.newHashSet("abs", "all", "any", "bin", "bool",
			"bytearray", "callable", "chr", "classmethod", "compile", "complex", "delattr",
			"dict", "dir", "divmod", "enumerate", "eval", "filter", "float", "format", "frozenset",
            "getattr", "globals", "hasattr", "hash", "help", "hex", "id", "input", "int", "isinstance",
            "issubclass", "iter", "len", "list", "locals", "map", "max", "memoryview", "min", "next",
            "object", "oct", "open", "ord", "pow", "property", "range", "repr", "reversed", "round",
            "set", "setattr", "slice", "sorted", "staticmethod", "str", "sum", "super", "tuple",
            "type", "vars", "zip", "__import__", "NotImplemented", "Ellipsis", "__debug__");
	
	static String ERRORCLASS = "error";
	
	static Pattern singleDelimiters = Pattern.compile("^[\\(\\)\\[\\]\\{\\}@,:`=;\\.]");
	static Pattern doubleDelimiters = Pattern.compile("^(\\+=|\\-=|\\*=|%=|\\/=|&=|\\|=|\\^=)");
	static Pattern tripleDelimiters = Pattern.compile("^(\\/\\/=|>>=|<<=|\\*\\*=)");
	
	static Pattern doubleOperators = Pattern.compile("^([!<>]==|<>|<<|>>|\\/\\/|\\*\\*)");
	static Pattern wordOperators = wordRegexp(Sets.newHashSet("and", "or", "not", "is"));
	
	static class Scope {
		int offset;
		String type;
		int align;
		
		Scope(int offset, String type, int align) {
			this.offset = offset;
			this.type = type;
			this.align = align;
		}
	}
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class State {
		Processor tokenize;
		Stack<Scope> scopes;
		String lastToken;
		int indent;
		int dedent;
		boolean errorToken;
		boolean beginningOfLine;
		boolean lambda;
		
		State(Processor tokenize, Stack<Scope> scopes, String lastToken, int indent, int dedent, boolean lambda) {
			this.tokenize = tokenize;
			this.scopes = scopes;
			this.lastToken = lastToken;
			this.indent = indent;
			this.dedent = dedent;
			this.lambda = lambda;
		}
	}
	
	abstract Pattern singleOperators();
	
	abstract Pattern identifiers();
	
	abstract Pattern keywords();
	
	abstract Pattern builtins();
	
	abstract Pattern stringPrefixes();
	
	abstract boolean py3();
	
	int hangingIndent() {
		return indentUnit();
	}
	
	boolean singleLineStringErrors() {
		return false;
	}
	
	static Pattern wordRegexp(Set<String> words) {
		return Pattern.compile("^((" + StringUtils.join(words, ")|(") + "))\\b");
	}
	
	Scope top(State state) {
		return state.scopes.peek();
	}
	
	void pushPyScope(State state) {
		while (!top(state).type.equals("py"))
			state.scopes.pop();
		state.scopes.push(new Scope(top(state).offset + indentUnit(), "py", 0));
	}
	
	static Pattern brackets = Pattern.compile("^([\\s\\[\\{\\(]|#.*)*$");
	
	void pushBracketScope(StringStream stream, State state, String type) {
		int align = !stream.match(brackets, false).isEmpty() ? 0 : stream.column() + 1;
		state.scopes.push(new Scope(state.indent + hangingIndent(), type, align));
	}
	
	boolean dedent(StringStream stream, State state) {
		int indented = stream.indentation();
		while (state.scopes.size() > 1 && top(state).offset > indented) {
			if (!top(state).type.equals("py"))
				return true;
			state.scopes.pop();
		}
		return top(state).offset != indented;
	}
	
	static Pattern numbers = Pattern.compile("^[0-9\\.]");
	static Pattern float1 = Pattern.compile("^\\d*\\.\\d+(e[\\+\\-]?\\d+)?", Pattern.CASE_INSENSITIVE);
	static Pattern float2 = Pattern.compile("^\\d+\\.\\d*");
	static Pattern float3 = Pattern.compile("^\\.\\d+");
	static Pattern number1 = Pattern.compile("J", Pattern.CASE_INSENSITIVE);
	static Pattern number2 = Pattern.compile("L", Pattern.CASE_INSENSITIVE);
	static Pattern hexInt = Pattern.compile("^0x[0-9a-f]+", Pattern.CASE_INSENSITIVE);
	static Pattern binInt = Pattern.compile("^0b[01]+", Pattern.CASE_INSENSITIVE);
	static Pattern octInt = Pattern.compile("^0o[0-7]+", Pattern.CASE_INSENSITIVE);
	static Pattern int1 = Pattern.compile("^[1-9]\\d*(e[\\+\\-]?\\d+)?");
	static Pattern int2 = Pattern.compile("^0(?![\\dx])", Pattern.CASE_INSENSITIVE);
	static Pattern selfcls = Pattern.compile("^(self|cls)\\b");
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (stream.sol())
				state.indent = stream.indentation();
			// handle scope changes
			if (stream.sol() && top(state).type.equals("py")) {
				int scopeOffset = top(state).offset;
				if (stream.eatSpace()) {
					int lineOffset = stream.indentation();
					if (lineOffset > scopeOffset)
						pushPyScope(state);
					else if (lineOffset < scopeOffset && dedent(stream, state) && !stream.peek().equals("#"))
						state.errorToken = true;
					return "";
				} else {
					String style = tokenBaseInner(stream, state);
					if (scopeOffset > 0 && dedent(stream, state))
						style += " " + ERRORCLASS;
					return style;
				}
			}
			return tokenBaseInner(stream, state);
		}
		
		String tokenBaseInner(StringStream stream, State state) {
			if (stream.eatSpace())
				return "";
			String ch = stream.peek();
			if (ch.equals("#")) {
				stream.skipToEnd();
				return "comment";
			}
			// numbers
			if (!stream.match(numbers, false).isEmpty()) {
				// floats
				boolean floatLiteral = false;
				if (!stream.match(float1).isEmpty())
					floatLiteral = true;
				if (!stream.match(float2).isEmpty())
					floatLiteral = true;
				if (!stream.match(float3).isEmpty())
					floatLiteral = true;
				if (floatLiteral) {
					stream.eat(number1);
					return "number";
				}
				// integers
				boolean intLiteral = false;
				if (!stream.match(hexInt).isEmpty())
					intLiteral = true;
				if (!stream.match(binInt).isEmpty())
					intLiteral = true;
				if (!stream.match(octInt).isEmpty())
					intLiteral = true;
				if (!stream.match(int1).isEmpty()) {
					stream.eat(number1);
					intLiteral = true;
				}
				if (!stream.match(int2).isEmpty())
					intLiteral = true;
				if (intLiteral) {
					stream.eat(number2);
					return "number";
				}
			}
			// strings
			if (!stream.match(stringPrefixes()).isEmpty()) {
				state.tokenize = tokenStringFactory(stream.current());
				return state.tokenize.process(stream, state);
			}
			// operators and delimiters
			if (!stream.match(tripleDelimiters).isEmpty() || !stream.match(doubleDelimiters).isEmpty())
				return "punctuation";
			if (!stream.match(doubleOperators).isEmpty() || !stream.match(singleOperators()).isEmpty())
				return "operator";
			if (!stream.match(singleDelimiters).isEmpty())
				return "punctuation";
			if (state.lastToken.equals(".") && !stream.match(identifiers()).isEmpty())
				return "property";
			if (!stream.match(keywords()).isEmpty() || !stream.match(wordOperators).isEmpty())
				return "keyword";
			if (!stream.match(builtins()).isEmpty())
				return "builtin";
			if (!stream.match(selfcls).isEmpty())
				return "variable-2";
			if (!stream.match(identifiers()).isEmpty()) {
				if (state.lastToken.equals("def") || state.lastToken.equals("class"))
					return "def";
				return "variable";
			}
			stream.next();
			return ERRORCLASS;
		}
	}

	Processor tokenStringFactory(String delimiter) {
		while ("rubf".indexOf(delimiter.substring(0, 1).toLowerCase()) >= 0)
			delimiter = delimiter.substring(1);
		boolean singleline = delimiter.length() == 1;
		return new TokenString(delimiter, singleline);
	}
	
	static Pattern strings = Pattern.compile("[^'\"\\\\]");
	static Pattern quotation = Pattern.compile("['\"]");
	
	class TokenString implements Processor {
		String delimiter;
		boolean singleline;
		
		TokenString(String delimiter, boolean singleline) {
			this.delimiter = delimiter;
			this.singleline = singleline;
		}
		
		@Override
		public String process(StringStream stream, State state) {
			String OUTCLASS = "string";
			while (!stream.eol()) {
				stream.eatWhile(strings);
				if (!stream.eat("\\").isEmpty()) {
					stream.next();
					if (singleline && stream.eol())
						return OUTCLASS;
				} else if (stream.match(delimiter)) {
					state.tokenize = new TokenBase();
					return OUTCLASS;
				} else {
					stream.eat(quotation);
				}
			}
			if (singleline) {
				if (singleLineStringErrors())
					return ERRORCLASS;
				else
					state.tokenize = new TokenBase();
			}
			return OUTCLASS;
		}
	}
	
	static Pattern notspace = Pattern.compile("\\S");
	
	String tokenLexer(StringStream stream, State state) {
		if (stream.sol())
			state.beginningOfLine = true;
		String style = state.tokenize.process(stream, state);
		String current = stream.current();
		
		// handle decorators
		if (state.beginningOfLine && current.equals("@"))
			return !stream.match(identifiers(), false).isEmpty() ? "meta" : py3() ? "operator" : ERRORCLASS;
		if (notspace.matcher(current).matches())
			state.beginningOfLine = false;
		if ((style.equals("variable") || style.equals("builtin")) && state.lastToken.equals("meta"))
			style = "meta";
		
		// handle scope changes
		if (current.equals("pass") || current.equals("return"))
			state.dedent++;
		if (current.equals("lambda"))
			state.lambda = true;
		if (current.equals(":") && !state.lambda && top(state).type.equals("py"))
			pushPyScope(state);
		int delimiter_index = current.length() == 1 ? "[({".indexOf(current) : -1;
		if (delimiter_index != -1)
			pushBracketScope(stream, state, "])}".substring(delimiter_index, delimiter_index + 1));
		delimiter_index = "])}".indexOf(current);
		if (delimiter_index  != -1) {
			if (top(state).type.equals(current))
				state.indent = state.scopes.pop().offset - hangingIndent();
			else
				return ERRORCLASS;
		}
		if (state.dedent > 0 && stream.eol() && top(state).type.equals("py")) {
			if (state.scopes.size() > 1)
				state.scopes.pop();
			state.dedent--;
		}
		return style;
	}
	
	@Override
	public State startState() {
		Stack<Scope> stack = new Stack<Scope>();
		stack.push(new Scope(0, "py", 0));
		return new State(new TokenBase(), stack, "", 0, 0, false);
	}
	
	@Override
	public String token(StringStream stream, State state) {
		boolean addErr = state.errorToken;
		if (addErr)
			state.errorToken = false;
		String style = tokenLexer(stream, state);
		
		if (!style.equals("") && !style.equals("comment"))
			state.lastToken = (style.equals("keyword") || style.equals("punctuation")) ? stream.current() : style;
		if (style.equals("punctuation"))
			style = "";
		
		if (stream.eol() && state.lambda)
			state.lambda = false;
		return addErr ? style + " " + ERRORCLASS : style;
	}
}
