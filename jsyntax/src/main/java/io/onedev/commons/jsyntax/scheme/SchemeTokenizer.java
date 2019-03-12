package io.onedev.commons.jsyntax.scheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class SchemeTokenizer extends AbstractTokenizer<SchemeTokenizer.State> {

	private static final String BUILTIN = "builtin";
	private static final String COMMENT = "comment";
	private static final String STRING = "string";
	private static final String ATOM = "atom";
	private static final String NUMBER = "number";
	private static final String BRACKET = "bracket";
	private static final int INDENT_WORD_SKIP = 2;

	private static Set<String> keywords = wordsOf("λ case-lambda call/cc class"
			+ " define-class exit-handler field import inherit init-field interface let*-values let-values let/ec mixin opt-lambda override protect"
			+ " provide public rename require require-for-syntax syntax syntax-case syntax-error unit/sig unless when with-syntax and begin call-with-current-continuation"
			+ " call-with-input-file call-with-output-file case cond define define-syntax delay do dynamic-wind else for-each if lambda let let* let-syntax letrec letrec-syntax"
			+ " map or syntax-rules abs acos angle append apply asin assoc assq assv atan boolean? caar cadr call-with-input-file call-with-output-file call-with-values car cdddar"
			+ " cddddr cdr ceiling char->integer char-alphabetic? char-ci<=? char-ci<? char-ci=? char-ci>=? char-ci>? char-downcase char-lower-case? char-numeric? char-ready? char-upcase"
			+ " char-upper-case? char-whitespace? char<=? char<? char=? char>=? char>? char? close-input-port close-output-port complex? cons cos current-input-port current-output-port"
			+ " denominator display eof-object? eq? equal? eqv? eval even? exact->inexact exact? exp expt #f floor force gcd imag-part inexact->exact inexact? input-port? integer->char"
			+ " integer? interaction-environment lcm length list list->string list->vector list-ref list-tail list? load log magnitude make-polar make-rectangular make-string make-vector"
			+ " max member memq memv min modulo negative? newline not null-environment null? number->string number? numerator odd? open-input-file open-output-file output-port? pair? peek-char"
			+ " port? positive? procedure? quasiquote quote quotient rational? rationalize read read-char real-part real? remainder reverse round scheme-report-environment set! set-car! set-cdr!"
			+ " sin sqrt string string->list string->number string->symbol string-append string-ci<=? string-ci<? string-ci=? string-ci>=? string-ci>? string-copy string-fill! string-length string-ref"
			+ " string-set! string<=? string<? string=? string>=? string>? string? substring symbol->string symbol? #t tan transcript-off transcript-on truncate values vector vector->list vector-fill! "
			+ " vector-length vector-ref vector-set! with-input-from-file with-output-to-file write write-char zero?");

	private static Set<String> indentKeys = Sets.newHashSet("define", "let", "letrec", "let*", "lambda");

	static class State {
		stateStack indentStack;
		int indentation;
		String mode;
		int sExprComment;
		Boolean sExprCommentIsNum;

		State(stateStack indentStack, int indentation, String mode, int sExprComment, Boolean sExprCommentIsNum) {
			this.indentStack = indentStack;
			this.indentation = indentation;
			this.mode = mode;
			this.sExprComment = sExprComment;
			this.sExprCommentIsNum = sExprCommentIsNum;
		}
	}

	static class stateStack {
		int indent;
		String type;
		stateStack prev;

		stateStack(int indent, String type, stateStack prev) {
			this.indent = indent;
			this.type = type;
			this.prev = prev;
		}
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	private stateStack pushStack(State state, int indent, String type) {

		return state.indentStack = new stateStack(indent, type, state.indentStack);
	}

	private stateStack popStack(State state) {

		return state.indentStack = state.indentStack.prev;
	}

	private static Pattern binaryMatcher = Pattern.compile(
			"^(?:[-+]i|[-+][01]+#*(?:\\/[01]+#*)?i|[-+]?[01]+#*(?:\\/[01]+#*)?@[-+]?[01]+#*(?:\\/[01]+#*)?|[-+]?[01]+#*(?:\\/[01]+#*)?[-+](?:[01]+#*(?:\\/[01]+#*)?)?i|[-+]?[01]+#*(?:\\/[01]+#*)?)(?=[()\\s;\"]|$)",
			Pattern.CASE_INSENSITIVE);

	private static Pattern octalMatcher = Pattern.compile(
			"^(?:[-+]i|[-+][0-7]+#*(?:\\/[0-7]+#*)?i|[-+]?[0-7]+#*(?:\\/[0-7]+#*)?@[-+]?[0-7]+#*(?:\\/[0-7]+#*)?|[-+]?[0-7]+#*(?:\\/[0-7]+#*)?[-+](?:[0-7]+#*(?:\\/[0-7]+#*)?)?i|[-+]?[0-7]+#*(?:\\/[0-7]+#*)?)(?=[()\\s;\"]|$)",
			Pattern.CASE_INSENSITIVE);

	private static Pattern hexMatcher = Pattern.compile(
			"^(?:[-+]i|[-+][\\da-f]+#*(?:\\/[\\da-f]+#*)?i|[-+]?[\\da-f]+#*(?:\\/[\\da-f]+#*)?@[-+]?[\\da-f]+#*(?:\\/[\\da-f]+#*)?|[-+]?[\\da-f]+#*(?:\\/[\\da-f]+#*)?[-+](?:[\\da-f]+#*(?:\\/[\\da-f]+#*)?)?i|[-+]?[\\da-f]+#*(?:\\/[\\da-f]+#*)?)(?=[()\\s;\"]|$)",
			Pattern.CASE_INSENSITIVE);

	private static Pattern decimalMatcher = Pattern.compile(
			"^(?:[-+]i|[-+](?:(?:(?:\\d+#+\\.?#*|\\d+\\.\\d*#*|\\.\\d+#*|\\d+)(?:[esfdl][-+]?\\d+)?)|\\d+#*\\/\\d+#*)i|[-+]?(?:(?:(?:\\d+#+\\.?#*|\\d+\\.\\d*#*|\\.\\d+#*|\\d+)(?:[esfdl][-+]?\\d+)?)|\\d+#*\\/\\d+#*)@[-+]?(?:(?:(?:\\d+#+\\.?#*|\\d+\\.\\d*#*|\\.\\d+#*|\\d+)(?:[esfdl][-+]?\\d+)?)|\\d+#*\\/\\d+#*)|[-+]?(?:(?:(?:\\d+#+\\.?#*|\\d+\\.\\d*#*|\\.\\d+#*|\\d+)(?:[esfdl][-+]?\\d+)?)|\\d+#*\\/\\d+#*)[-+](?:(?:(?:\\d+#+\\.?#*|\\d+\\.\\d*#*|\\.\\d+#*|\\d+)(?:[esfdl][-+]?\\d+)?)|\\d+#*\\/\\d+#*)?i|(?:(?:(?:\\d+#+\\.?#*|\\d+\\.\\d*#*|\\.\\d+#*|\\d+)(?:[esfdl][-+]?\\d+)?)|\\d+#*\\/\\d+#*))(?=[()\\s;\"]|$)",
			Pattern.CASE_INSENSITIVE);

	private List<String> isBinaryNumber(StringStream stream) {
		return stream.match(binaryMatcher);
	}

	private List<String> isOctalNumber(StringStream stream) {
		return stream.match(octalMatcher);
	}

	private List<String> isDecimalNumber(StringStream stream, Boolean backup) {
		if (backup == true) {
			stream.backUp(1);
		}
		return stream.match(decimalMatcher);
	}

	private List<String> isHexNumber(StringStream stream) {
		return stream.match(hexMatcher);
	}

	static Pattern[] pattern = new Pattern[11];

	static {
		pattern[0] = Pattern.compile("[^/s]");
		pattern[1] = Pattern.compile("[tf]", Pattern.CASE_INSENSITIVE);
		pattern[2] = Pattern.compile("[ei]", Pattern.CASE_INSENSITIVE);
		pattern[3] = Pattern.compile("^#b", Pattern.CASE_INSENSITIVE);
		pattern[4] = Pattern.compile("^#o", Pattern.CASE_INSENSITIVE);
		pattern[5] = Pattern.compile("^#x", Pattern.CASE_INSENSITIVE);
		pattern[6] = Pattern.compile("^#d", Pattern.CASE_INSENSITIVE);
		pattern[7] = Pattern.compile("^[-+0-9.]");
		pattern[8] = Pattern.compile("^#[ei]", Pattern.CASE_INSENSITIVE);
		pattern[9] = Pattern.compile("[^\\s\\(\\[\\;\\)\\]]");
		pattern[10] = Pattern.compile("[\\w\\$_\\-!$%&*+\\.\\/:<=>?@\\^~]");
	}

	@Override
	public boolean accept(String fileName) {

		return acceptExtensions(fileName, "scm");
	}

	@Override
	public State startState() {

		return new State(new stateStack(0, "", null), 0, "", 0, false);
	}

	@Override
	public String token(StringStream stream, State state) {

		if (stream.sol()) {
			state.indentation = stream.indentation();
		}
		// skip spaces
		if (stream.eatSpace()) {
			return "";
		}
		String returnType = "";
		switch (state.mode) {
		case "string": // multi-line string parsing mode
			String next;
			Boolean escaped = false;
			while ((next = stream.next()).length() != 0) {
				if (next.equals("\"") && !escaped) {
					state.mode = "false";
					break;
				}
				escaped = !escaped && next == "\\";
			}
			returnType = STRING; // continue on in scheme-string mode
			break;
		case "comment": // comment parsing mode
			String n;
			Boolean maybeEnd = false;
			while ((n = stream.next()).length() != 0) {
				if (n.equals("#") && maybeEnd) {
					state.mode = "false";
					break;
				}
				maybeEnd = n.equals("|");
			}
			returnType = COMMENT;
			break;
		case "s-expr-comment": // s-expr commenting mode
			state.mode = "false";
			if (stream.peek().equals("(") || stream.peek().equals("[")) {
				// actually start scheme s-expr commenting mode
				state.sExprComment = 0;
				state.sExprCommentIsNum = true;
			} else {
				// if not we just comment the entire of the next token
				stream.eatWhile(pattern[0]); // eat non spaces
				returnType = COMMENT;
				break;
			}
		case "false":
		
		default: // default parsing mode
			String ch = stream.next();
			if (ch.equals("\"")) {
				state.mode = "string";
				returnType = STRING;
			} else if (ch.equals("'")) {
				returnType = ATOM;
			} else if (ch.equals("#")) {
				if (!stream.eat("|").isEmpty()) { // Multi-line comment
					state.mode = "comment"; // toggle to comment mode
					returnType = ATOM;
				} else if (!stream.eat(pattern[1]).isEmpty()) { // #t/#f (atom)
					returnType = ATOM;
				} else if (!stream.eat(";").isEmpty()) { // S-Expr comment
					state.mode = "s-expr-comment";
					returnType = COMMENT;
				} else {
					List<String> numTest = new ArrayList<>();
					Boolean hasExactness = false;
					Boolean hasRadix = true;
					if (!stream.eat(pattern[2]).isEmpty()) {
						hasExactness = true;
					} else {
						stream.backUp(1); // must be radix specifier
					}
					if (!stream.match(pattern[3]).isEmpty()) {
						numTest = isBinaryNumber(stream);
					} else if (!stream.match(pattern[4]).isEmpty()) {
						numTest = isOctalNumber(stream);
					} else if (!stream.match(pattern[5]).isEmpty()) {
						numTest = isHexNumber(stream);
					} else if (!stream.match(pattern[6]).isEmpty()) {
						numTest = isDecimalNumber(stream, true);
					} else if (!stream.match(pattern[7], false).isEmpty()) {
						hasRadix = false;
						numTest = isDecimalNumber(stream, false);
					} else if (!hasExactness) {
						stream.eat("#");
					}
					if (!numTest.isEmpty()) {
						if (hasRadix && hasExactness) {
							// consume optional exactness after radix
							stream.match(pattern[8]);
						}
						returnType = NUMBER;
					}
				}
			} else if (pattern[7].matcher(ch).matches() && !isDecimalNumber(stream, true).isEmpty()) { 
				// match non-prefixed number,must be decimal	
				returnType = NUMBER;
			} else if (ch.equals(";")) { // comment
				stream.skipToEnd(); // rest of the line is a comment
				returnType = COMMENT;
			} else if (ch.equals("(") || ch.equals("[")) {
				String keyWord = "";
				int indentTemp = stream.column();
				String letter;
				/*
				 * Either (indent-word .. (non-indent-word .. (;something else,
				 * bracket, etc.
				 */
				while ((letter = stream.eat(pattern[9])).length() != 0) {
					keyWord += letter;
				}
				if (keyWord.length() > 0 && indentKeys.contains(keyWord)) { // indent-word
					pushStack(state, indentTemp + INDENT_WORD_SKIP, ch);
				} else { // non-indent word
							// we continue eating the spaces
					stream.eatSpace();
					if (stream.eol() || stream.peek().equals(";")) {
						// nothing significant after
						// we restart indentation 1 space after
						pushStack(state, indentTemp + 1, ch);
					} else {
						pushStack(state, indentTemp + stream.current().length(), ch); // else we match
					}
				}
				stream.backUp(stream.current().length() - 1); // undo all the eating
				if (state.sExprCommentIsNum == true)
					state.sExprComment++;

				returnType = BRACKET;
			} else if (ch.equals(")") || ch.equals("]")) {
				returnType = BRACKET;
				if (state.indentStack != null && state.indentStack.type == (ch.equals(")") ? "(" : "[")) {
					popStack(state);
					if (state.sExprCommentIsNum == true) {
						if (--state.sExprComment == 0) {
							returnType = COMMENT; // final closing bracket
							state.sExprCommentIsNum = false; // turn off s-expr commenting mode																										
						}
					}
				}
			} else {
				stream.eatWhile(pattern[10]);
				if (!keywords.isEmpty() && keywords.contains(stream.current())) {
					returnType = BUILTIN;
				} else {
					returnType = "variable";
				}
			}
		}

		return (state.sExprCommentIsNum == true) ? COMMENT : returnType;
	}
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-scheme");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("scheme");
	}
}