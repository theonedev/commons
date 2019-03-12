package io.onedev.commons.jsyntax.vb;

import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class VbTokenizer extends AbstractTokenizer<VbTokenizer.State> {
	
	static String ERRORCLASS = "error";
	static String stringPrefixes = "\"";
	
	static Pattern singleOperators = Pattern.compile("^[\\+\\-\\*/%&\\\\|\\^~<>!]");
	static Pattern singleDelimiters = Pattern.compile("^[\\(\\)\\[\\]\\{\\}@,:`=;\\.]");
	static Pattern doubleOperators = Pattern.compile("^((==)|(<>)|(<=)|(>=)|(<>)|(<<)|(>>)|(//)|(\\*\\*))");
	static Pattern doubleDelimiters = Pattern.compile("^((\\+=)|(\\-=)|(\\*=)|(%=)|(/=)|(&=)|(\\|=)|(\\^=))");
	static Pattern tripleDelimiters = Pattern.compile("^((//=)|(>>=)|(<<=)|(\\*\\*=))");
	static Pattern identifiers = Pattern.compile("^[_A-Za-z][_A-Za-z0-9]*");
	
	static Set<String> openingKeywords = Sets.newHashSet("class", "module", "sub", "enum", "select", "while", "if", "function", "get", "set", "property", "try");
	static Set<String> middleKeywords = Sets.newHashSet("else", "elseif", "case", "catch");
	static Set<String> endKeywords = Sets.newHashSet("next", "loop");
	
	static Set<String> operatorKeywords = Sets.newHashSet("and", "or", "not", "xor", "in");
	static Set<String> commonKeywords = Sets.newHashSet("as", "dim", "break", "continue", "optional", "then", "until",
            						"goto", "byval", "byref", "new", "handles", "property", "return",
            						"const", "private", "protected", "friend", "public", "shared", "static", "true", "false");
	static Set<String> commonTypes = Sets.newHashSet("integer", "string", "double", "decimal", "boolean", "short", "char", "float", "single");
	
	static Pattern keywords;
	static Pattern types;
	static Pattern wordOperators;
	static Pattern opening;
	static Pattern middle;
	static Pattern closing;
	static Pattern doubleClosing;
	static Pattern doOpening;
	
	static {
		keywords = wordRegexp(commonKeywords);
		types = wordRegexp(commonTypes);
		wordOperators = wordRegexp(operatorKeywords);
		opening = wordRegexp(openingKeywords);
		middle = wordRegexp(middleKeywords);
		closing = wordRegexp(endKeywords);
		doubleClosing = wordRegexp(Sets.newHashSet("end"));
		doOpening = wordRegexp(Sets.newHashSet("do"));
	}
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class Token {
		String style;
		String content;
		
		Token(String style, String content) {
			this.style = style;
			this.content = content;
		}
	}

	static class State {
		Processor tokenize;
		Token lastToken;
		String indentInfo;
		int currentIndent;
		int nextLineIndent;
		boolean doInCurrentLine;
		
		State(Processor tokenize, Token lastToken, int currentIndent, int nextLineIndent, boolean doInCurrentLine) {
			this.tokenize = tokenize;
			this.lastToken = lastToken;
			this.indentInfo = "";
			this.currentIndent = currentIndent;
			this.nextLineIndent = nextLineIndent;
			this.doInCurrentLine = doInCurrentLine;
		}
	}
	
	static Pattern wordRegexp(Set<String> words) {
		return Pattern.compile("^((" + StringUtils.join(words, ")|(") + "))\\b", Pattern.CASE_INSENSITIVE);
	}
	
	static void indent(State state) {
		state.currentIndent++;
	}
	
	static boolean dedent(State state) {
		state.currentIndent--;
		return false;
	}
	
	boolean singleLineStringErrors() {
		return false;
	}
	
	static Pattern number = Pattern.compile("^((&H)|(&O))?[0-9\\.a-f]", Pattern.CASE_INSENSITIVE);
	static Pattern float1 = Pattern.compile("^\\d*\\.\\d+F?", Pattern.CASE_INSENSITIVE);
	static Pattern float2 = Pattern.compile("^\\d+\\.\\d*F?");
	static Pattern float3 = Pattern.compile("^\\.\\d+F?");
	static Pattern hex = Pattern.compile("^&H[0-9a-f]+", Pattern.CASE_INSENSITIVE);
	static Pattern oct = Pattern.compile("^&O[0-7]+", Pattern.CASE_INSENSITIVE);
	static Pattern dec = Pattern.compile("^[1-9]\\d*F?");
	static Pattern other = Pattern.compile("^0(?![\\dx])", Pattern.CASE_INSENSITIVE);
	static Pattern patternJ = Pattern.compile("J", Pattern.CASE_INSENSITIVE);
	static Pattern patternL = Pattern.compile("L", Pattern.CASE_INSENSITIVE);
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
	        if (stream.eatSpace()) {
	            return "";
	        }

	        String ch = stream.peek();

	        // Handle Comments
	        if (ch.equals("'")) {
	            stream.skipToEnd();
	            return "comment";
	        }

	        // Handle Number Literals
	        if (!stream.match(number, false).isEmpty()) {
	            boolean floatLiteral = false;
	            // Floats
	            if (!stream.match(float1).isEmpty())
	            	floatLiteral = true;
	            else if (!stream.match(float2).isEmpty())
	            	floatLiteral = true;
	            else if (!stream.match(float3).isEmpty())
	            	floatLiteral = true;

	            if (floatLiteral) {
	                // Float literals may be "imaginary"
	                stream.eat(patternJ);
	                return "number";
	            }
	            // Integers
	            boolean intLiteral = false;
	            // Hex
	            if (!stream.match(hex).isEmpty())
	            	intLiteral = true;
	            // Octal
	            else if (!stream.match(oct).isEmpty())
	            	intLiteral = true;
	            // Decimal
	            else if (!stream.match(dec).isEmpty()) {
	                // Decimal literals may be "imaginary"
	                stream.eat(patternJ);
	                intLiteral = true;
	            }
	            // Zero by itself with no other piece of number.
	            else if (!stream.match(other).isEmpty())
	            	intLiteral = true;
	            if (intLiteral) {
	                // Integer literals may be "long"
	                stream.eat(patternL);
	                return "number";
	            }
	        }

	        // Handle Strings
	        if (stream.match(stringPrefixes)) {
	            state.tokenize = tokenStringFactory(stream.current());
	            return state.tokenize.process(stream, state);
	        }

	        // Handle operators and Delimiters
	        if (!stream.match(tripleDelimiters).isEmpty() || !stream.match(doubleDelimiters).isEmpty()) {
	            return "";
	        }
	        if (!stream.match(doubleOperators).isEmpty()
	            || !stream.match(singleOperators).isEmpty()
	            || !stream.match(wordOperators).isEmpty()) {
	            return "operator";
	        }
	        if (!stream.match(singleDelimiters).isEmpty()) {
	            return "";
	        }
	        if (!stream.match(doOpening).isEmpty()) {
	        	indent(state);
	        	state.doInCurrentLine = true;
	            return "keyword";
	        }
	        if (!stream.match(opening).isEmpty()) {
	            if (!state.doInCurrentLine)
	            	indent(state);
	            else
	            	state.doInCurrentLine = false;
	            return "keyword";
	        }
	        if (!stream.match(middle).isEmpty())
	            return "keyword";

	        if (!stream.match(doubleClosing).isEmpty()) {
	            dedent(state);
	            dedent(state);
	            return "keyword";
	        }
	        if (!stream.match(closing).isEmpty()) {
	        	dedent(state);
	            return "keyword";
	        }

	        if (!stream.match(types).isEmpty())
	            return "keyword";

	        if (!stream.match(keywords).isEmpty())
	            return "keyword";

	        if (!stream.match(identifiers).isEmpty())
	            return "variable";

	        // Handle non-detected items
	        stream.next();
	        return ERRORCLASS;
		}
		
	}
	
	Processor tokenStringFactory(String delimiter) {
		return new TokenString(delimiter.length() == 1, delimiter);
	}
	
	static Pattern notquotation = Pattern.compile("[^\'\"]");
	static Pattern quotation = Pattern.compile("[\'\"]");
	
	class TokenString implements Processor {
		boolean singleline;
		String delimiter;
		
		TokenString(boolean singleline, String delimiter) {
			this.singleline = singleline;
			this.delimiter = delimiter;
		}
		
		@Override
		public String process(StringStream stream, State state) {
			while (!stream.eol()) {
				stream.eatWhile(notquotation);
				if (stream.match(delimiter)) {
					state.tokenize = new TokenBase();
					return "string";
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
			return "string";
		}
	}
	
	String tokenLexer(StringStream stream, State state) {
		String style = state.tokenize.process(stream, state);
		String current = stream.current();
		
		// Handle '.' connected identifiers
		if (current.equals(".")) {
			style = state.tokenize.process(stream, state);
			current = stream.current();
			if (style.equals("variable"))
				return "variable";
			else
				return ERRORCLASS;
		}
		
		int delimiter_index = "[({".indexOf(current);
		if (delimiter_index != -1)
			indent(state);
		if (state.indentInfo.equals("dedent"))
			if (dedent(state))
				return ERRORCLASS;
		delimiter_index = "])}".indexOf(current);
		if (delimiter_index != -1)
			if (dedent(state))
				return ERRORCLASS;
		return style;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "frm", "bas", "cls", "vb");
	}

	@Override
	public State startState() {
		return new State(new TokenBase(), null, 0, 0, false);
	}

	@Override
	public String token(StringStream stream, State state) {
		if (stream.sol()) {
			state.currentIndent += state.nextLineIndent;
			state.nextLineIndent = 0;
			state.doInCurrentLine = false;
		}
		String style = tokenLexer(stream, state);
		state.lastToken = new Token(style, stream.current());
		return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-vb");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("vb");
	}
}
