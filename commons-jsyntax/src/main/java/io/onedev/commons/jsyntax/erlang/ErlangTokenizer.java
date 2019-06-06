package io.onedev.commons.jsyntax.erlang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class ErlangTokenizer extends AbstractTokenizer<ErlangTokenizer.State> {

	static Set<String> typeWords = Sets.newHashSet("-type", "-spec", "-export_type", "-opaque");

	static Set<String> keywordWords = Sets.newHashSet("after", "begin", "catch", "case", "cond", "end", "fun", "if",
			"let", "of", "query", "receive", "try", "when");

	static Pattern separatorRE = Pattern.compile("[\\->,;]");

	static Set<String> separatorWords = Sets.newHashSet("->", ";", ",");

	static Set<String> operatorAtomWords = Sets.newHashSet("and", "andalso", "band", "bnot", "bor", "bsl", "bsr",
			"bxor", "div", "not", "or", "orelse", "rem", "xor");

	static Pattern operatorSymbolRE = Pattern.compile("[\\+\\-\\*\\/<>=\\|:!]");

	static Set<String> operatorSymbolWords = Sets.newHashSet("=", "+", "-", "*", "/", ">", ">=", "<", "=<", "=:=", "==",
			"=/=", "/=", "||", "<-", "!");

	static Pattern openParenRE = Pattern.compile("[<\\(\\[\\{]");

	static Set<String> openParenWords = Sets.newHashSet("<<", "(", "[", "{");

	static Pattern closeParenRE = Pattern.compile("[>\\)\\]\\}]");

	static Set<String> closeParenWords = Sets.newHashSet("}", "]", ")", ">>");

	static Set<String> guardWords = Sets.newHashSet("is_atom", "is_binary", "is_bitstring", "is_boolean", "is_float",
			"is_function", "is_integer", "is_list", "is_number", "is_pid", "is_port", "is_record", "is_reference",
			"is_tuple", "atom", "binary", "bitstring", "boolean", "function", "integer", "list", "number", "pid",
			"port", "record", "reference", "tuple");

	static Set<String> bifWords = Sets.newHashSet("abs", "adler32", "adler32_combine", "alive", "apply",
			"atom_to_binary", "atom_to_list", "binary_to_atom", "binary_to_existing_atom", "binary_to_list",
			"binary_to_term", "bit_size", "bitstring_to_list", "byte_size", "check_process_code", "contact_binary",
			"crc32", "crc32_combine", "date", "decode_packet", "delete_module", "disconnect_node", "element", "erase",
			"exit", "float", "float_to_list", "garbage_collect", "get", "get_keys", "group_leader", "halt", "hd",
			"integer_to_list", "internal_bif", "iolist_size", "iolist_to_binary", "is_alive", "is_atom", "is_binary",
			"is_bitstring", "is_boolean", "is_float", "is_function", "is_integer", "is_list", "is_number", "is_pid",
			"is_port", "is_process_alive", "is_record", "is_reference", "is_tuple", "length", "link", "list_to_atom",
			"list_to_binary", "list_to_bitstring", "list_to_existing_atom", "list_to_float", "list_to_integer",
			"list_to_pid", "list_to_tuple", "load_module", "make_ref", "module_loaded", "monitor_node", "node",
			"node_link", "node_unlink", "nodes", "notalive", "now", "open_port", "pid_to_list", "port_close",
			"port_command", "port_connect", "port_control", "pre_loaded", "process_flag", "process_info", "processes",
			"purge_module", "put", "register", "registered", "round", "self", "setelement", "size", "spawn",
			"spawn_link", "spawn_monitor", "spawn_opt", "split_binary", "statistics", "term_to_binary", "time", "throw",
			"tl", "trunc", "tuple_size", "tuple_to_list", "unlink", "unregister", "whereis");

	// upper case: [A-Z] [Ø-Þ] [À-Ö]
	// lower case: [a-z] [ß-ö] [ø-ÿ]
	static Pattern anumRE = Pattern.compile("[\\w@Ø-ÞÀ-Öß-öø-ÿ]");

	static Pattern escapesRE = Pattern.compile("[0-7]{1,3}|[bdefnrstv\\\"']|\\^[a-zA-Z]|x[0-9a-zA-Z]{2}|x[0-9a-zA-Z]+");

	// tokenizer

	static Pattern[] tokenizerPattern = new Pattern[8];
	static {
		tokenizerPattern[0] = Pattern.compile("-\\s*[a-zß-öø-ÿ][\\wØ-ÞÀ-Öß-öø-ÿ]*");
		tokenizerPattern[1] = Pattern.compile("\\s*\\/\\s*[0-9]");
		tokenizerPattern[2] = Pattern.compile("\\s*\\(");
		tokenizerPattern[3] = Pattern.compile("\\s*:");
		tokenizerPattern[4] = Pattern.compile("[A-Z_Ø-ÞÀ-Ö]");
		tokenizerPattern[5] = Pattern.compile("[a-z_ß-öø-ÿ]");
		tokenizerPattern[6] = Pattern.compile("[eE]");
		tokenizerPattern[7] = Pattern.compile("[-+]");
	}
	static Pattern digitRE = Pattern.compile("[0-9]");
	static Pattern radixRE = Pattern.compile("[0-9a-zA-Z]");
	static Set<String> tf = Sets.newHashSet("true", "false");

	public String tokenizer(StringStream stream, State state) {

		// in multi-line string
		if (state.in_string) {
			state.in_string = (!doubleQuote(stream));
			return rval(state, stream, "string");
		}

		// in multi-line atom
		if (state.in_atom) {
			state.in_atom = (!singleQuote(stream));
			return rval(state, stream, "atom");
		}

		// whitespace
		if (stream.eatSpace()) {
			return rval(state, stream, "whitespace");
		}

		// attributes and type specs
		if (peekToken(state, 0) == null && !stream.match(tokenizerPattern[0]).isEmpty()) {
			if (typeWords.contains(stream.current())) {
				return rval(state, stream, "type");
			} else {
				return rval(state, stream, "attribute");
			}
		}

		String ch = stream.next();
		// comment
		if (ch.equals("%")) {
			stream.skipToEnd();
			return rval(state, stream, "comment");
		}

		// colon
		if (ch.equals(":")) {
			return rval(state, stream, "colon");
		}

		// macro
		if (ch.equals("?")) {
			stream.eatSpace();
			stream.eatWhile(anumRE);
			return rval(state, stream, "macro");
		}

		// record
		if (ch.equals("#")) {
			stream.eatSpace();
			stream.eatWhile(anumRE);
			return rval(state, stream, "record");
		}

		// dollar escape
		if (ch.equals("$")) {
			if (stream.next().equals("\\")) {
				int position=stream.pos();
				if(stream.next().equals("\\")){
					return rval(state, stream, "number");
				}
				stream.pos(position);
				if(stream.match(escapesRE).isEmpty()){
					return rval(state, stream, "error");
				}			
			}

			return rval(state, stream, "number");
		}

		// dot
		if (ch.equals(".")) {
			return rval(state, stream, "dot");
		}

		// quoted atom
		if (ch.equals("'")) {
			if (!(state.in_atom = (!singleQuote(stream)))) {
				if (!stream.match(tokenizerPattern[1], false).isEmpty()) {
					stream.match(tokenizerPattern[1], true);
					return rval(state, stream, "fun"); // 'f'/0 style fun
				}
				if (!stream.match(tokenizerPattern[2], false).isEmpty()
						|| !stream.match(tokenizerPattern[3], false).isEmpty()) {
					return rval(state, stream, "function");
				}
			}
			return rval(state, stream, "atom");
		}
		// string
		if (ch.equals("\"")) {
			state.in_string = (!doubleQuote(stream));
			return rval(state, stream, "string");
		}

		// variable
		if (tokenizerPattern[4].matcher(ch).find()) {
			stream.eatWhile(anumRE);
			return rval(state, stream, "variable");
		}
		// atom/keyword/BIF/function
		if (tokenizerPattern[5].matcher(ch).find()) {
			stream.eatWhile(anumRE);

			if (!stream.match(tokenizerPattern[1], false).isEmpty()) {
				stream.match(tokenizerPattern[1], true);
				return rval(state, stream, "fun"); // f/0 style fun
			}

			String w = stream.current();

			if (keywordWords.contains(w)) {
				return rval(state, stream, "keyword");
			} else if (operatorAtomWords.contains(w)) {
				return rval(state, stream, "operator");
			} else if (!stream.match(tokenizerPattern[2], false).isEmpty()) {
				// 'put' and 'erlang:put' are bifs, 'foo:put' is not
				Token peekedToken0 = peekToken(state, 0);
				Token peekedToken2 = peekToken(state, 2);
				String token0 = peekedToken0!=null?peekedToken0.token:"";
				String token2 = peekedToken2!=null?peekedToken2.token:"";
				if (bifWords.contains(w) && ((!token0.equals(":")) || (token2.equals("erlang")))) {
					return rval(state, stream, "builtin");
				} else if (guardWords.contains(w)) {
					return rval(state, stream, "guard");
				} else {
					return rval(state, stream, "function");
				}
			} else if (lookahead(stream).equals(":")) {
				if (w.equals("erlang")) {
					return rval(state, stream, "builtin");
				} else {
					return rval(state, stream, "function");
				}
			} else if (tf.contains(w)) {
				return rval(state, stream, "boolean");
			} else {
				return rval(state, stream, "atom");
			}
		}

		// number
		if (digitRE.matcher(ch).find()) {
			stream.eatWhile(digitRE);
			if (stream.eat("#").length() != 0) { // 36#aZ style integer
				if (!stream.eatWhile(radixRE)) {
					stream.backUp(1); // "36#" - syntax error
				}
			} else if (stream.eat(".").length() != 0) { // float
				if (!stream.eatWhile(digitRE)) {
					stream.backUp(1); // "3." - probably end of function
				} else {
					if (!stream.eat(tokenizerPattern[6]).isEmpty()) { // float
																		// with
																		// exponent
						if (!stream.eat(tokenizerPattern[7]).isEmpty()) {
							if (!stream.eatWhile(digitRE)) {
								stream.backUp(2); // "2e-" - syntax error
							}
						} else {
							if (!stream.eatWhile(digitRE)) {
								stream.backUp(1); // "2e" - syntax error
							}
						}
					}
				}
			}
			return rval(state, stream, "number"); // normal integer
		}
		// open parens
		if (nongreedy(stream, openParenRE, openParenWords)) {
			return rval(state, stream, "open_paren");
		}

		// close parens
		if (nongreedy(stream, closeParenRE, closeParenWords)) {
			return rval(state, stream, "close_paren");
		}

		// separators
		if (greedy(stream, separatorRE, separatorWords)) {
			return rval(state, stream, "separator");
		}

		// operators
		if (greedy(stream, operatorSymbolRE, operatorSymbolWords)) {
			return rval(state, stream, "operator");
		}

		return rval(state, stream, "");
	}

	// utilities
	Boolean nongreedy(StringStream stream, Pattern re, Set<String> words) {

		if (stream.current().length() == 1 && re.matcher(stream.current()).find()) {
			stream.backUp(1);
			while (re.matcher(stream.peek()).find()) {
				stream.next();
				if (words.contains(stream.current())) {
					return true;
				}
			}
			stream.backUp(stream.current().length() - 1);
		}
		return false;
	}

	Boolean greedy(StringStream stream, Pattern re, Set<String> words) {

		if (stream.current().length() == 1 && re.matcher(stream.current()).find()) {
			while (re.matcher(stream.peek()).find()) {
				stream.next();
			}
			while (0 < stream.current().length()) {
				if (words.contains(stream.current())) {
					return true;
				} else {
					stream.backUp(1);
				}
			}
			stream.next();
		}
		return false;
	}

	Boolean doubleQuote(StringStream stream) {
		return quote(stream, "\"", "\\");
	}

	Boolean singleQuote(StringStream stream) {
		return quote(stream, "'", "\\");
	}

	Boolean quote(StringStream stream, String quoteChar, String escapeChar) {

		while (!stream.eol()) {
			String ch = stream.next();
			if (ch.equals(quoteChar)) {
				return true;
			} else if (ch.equals(escapeChar)) {
				stream.next();
			}
		}
		return false;
	}

	static Pattern lookahead_pattern = Pattern.compile("([\\n\\s]+|%[^\\n]*\\n)*(.)");

	String lookahead(StringStream stream) {
		List<String> m = stream.match(lookahead_pattern, false);

		return !m.isEmpty() ? m.get(m.size() - 1) : "";
	}

	String rval(State state, StringStream stream, String type) {
		// parse stack
		pushToken(state, realToken(type, stream));

		// map erlang token type to CodeMirror style class
		// erlang -> CodeMirror tag
		switch (type) {
		case "atom":
			return "atom";
		case "attribute":
			return "attribute";
		case "boolean":
			return "atom";
		case "builtin":
			return "builtin";
		case "close_paren":
			return "";
		case "colon":
			return "";
		case "comment":
			return "comment";
		case "dot":
			return "";
		case "error":
			return "error";
		case "fun":
			return "meta";
		case "function":
			return "tag";
		case "guard":
			return "property";
		case "keyword":
			return "keyword";
		case "macro":
			return "variable-2";
		case "number":
			return "number";
		case "open_paren":
			return "";
		case "operator":
			return "operator";
		case "record":
			return "bracket";
		case "separator":
			return "";
		case "string":
			return "string";
		case "type":
			return "def";
		case "variable":
			return "variable";
		default:
			return "";
		}
	}

	static class State {
		Stack<Token> tokenStack;
		Boolean in_string;
		Boolean in_atom;

		State(Stack<Token> tokenStack, Boolean in_string, Boolean in_atom) {

			this.tokenStack = tokenStack;
			this.in_string = in_string;
			this.in_atom = in_atom;
		}

	}

	class Token {
		String token;
		int column;
		int indent;
		String type;

		Token(String token, int column, int indent, String type) {
			this.token = token;
			this.column = column;
			this.indent = indent;
			this.type = type;
		}

	}

	private Token aToken(String tok, int col, int ind, String typ) {
		return new Token(tok, col, ind, typ);
	}

	private Token realToken(String type, StringStream stream) {
		return aToken(stream.current(), stream.column(), stream.indentation(), type);
	}

	private Token fakeToken(String type) {
		return aToken(type, 0, 0, type);
	}

	private Token peekToken(State state, int depth) {
		int len = state.tokenStack.size() - 1;
		int dep = (depth != 0 ? depth : 1);
		if (len < dep) {
			return null;
		} else {
			return state.tokenStack.get(len - dep);
		}
	}

	private void pushToken(State state, Token token) {
		if (!(token.type.equals("comment") || token.type.equals("whitespace"))) {
			state.tokenStack = maybe_drop_pre(state.tokenStack, token);
			state.tokenStack = maybe_drop_post(state.tokenStack);
		}
	}

	private Stack<Token> maybe_drop_pre(Stack<Token> s, Token token) {
		int last = s.size() - 1;
		if (0 < last && s.get(last).type.equals("record") && token.type.equals("dot")) {
			s.pop();
		} else if ((0 < last) && s.get(last).type.equals("group")) {
			s.pop();
			s.push(token);
		} else {
			s.push(token);
		}
		return s;
	}

	private Stack<Token> slice(Stack<Token> s, int start, int end) {
		Stack<Token> st = new Stack<>();
		for (int i = start; i <= end; i++) {
			st.push(s.get(i));
		}
		return st;
	}

	static Map<String, Set<String>> tt = new HashMap<>();

	static Set<String> maybe_drop_post_Set1 = Sets.newHashSet("{");
	static Set<String> maybe_drop_post_Set2 = Sets.newHashSet("[");
	static Set<String> maybe_drop_post_Set3 = Sets.newHashSet("(");
	static Set<String> maybe_drop_post_Set4 = Sets.newHashSet("<<");
	static Set<String> maybe_drop_post_Set5 = Sets.newHashSet("begin", "case", "fun", "if", "receive", "try");
	static Set<String> maybe_drop_post_Set6 = Sets.newHashSet("begin", "try", "when", "->", ",", "(", "[", "{", "<<");
	static Set<String> maybe_drop_post_Set7 = Sets.newHashSet("when");
	static Set<String> maybe_drop_post_Set8 = Sets.newHashSet("try", "if", "case", "receive");
	static Set<String> maybe_drop_post_Set9 = Sets.newHashSet("case", "fun", "if", "receive", "try", "when");
	static Set<String> maybe_drop_post_Set10 = Sets.newHashSet("try");
	static Set<String> maybe_drop_post_Set11 = Sets.newHashSet("case");
	static Set<String> maybe_drop_post_Set12 = Sets.newHashSet("receive", "try");

	private Stack<Token> maybe_drop_post(Stack<Token> s) {
		if (s.empty())
			return s;
		int last = s.size() - 1;
		if (s.get(last).type.equals("dot")) {
			return new Stack<Token>();
		}
		if (last > 1 && s.get(last).type.equals("fun") && s.get(last - 1).token.equals("fun")) {
			return slice(s, 0, last - 1);
		}
		switch (s.get(last).token) {
		case "}":
			tt.put("g", maybe_drop_post_Set1);
			return d(s, tt);
		case "]":
			tt.put("i", maybe_drop_post_Set2);
			return d(s, tt);
		case ")":
			tt.put("i", maybe_drop_post_Set3);
			return d(s, tt);
		case ">>":
			tt.put("i", maybe_drop_post_Set4);
			return d(s, tt);
		case "end":
			tt.put("i", maybe_drop_post_Set5);
			return d(s, tt);
		case ",":
			tt.put("e", maybe_drop_post_Set6);
			return d(s, tt);
		case "->":
			tt.put("r", maybe_drop_post_Set7);
			tt.put("m", maybe_drop_post_Set8);
			return d(s, tt);
		case ";":
			tt.put("E", maybe_drop_post_Set9);
			return d(s, tt);
		case "catch":
			tt.put("e", maybe_drop_post_Set10);
			return d(s, tt);
		case "of":
			tt.put("e", maybe_drop_post_Set11);
			return d(s, tt);
		case "after":
			tt.put("e", maybe_drop_post_Set12);
			return d(s, tt);
		default:
			return s;
		}
	}

	@SuppressWarnings("unchecked")
	private Stack<Token> d(Stack<Token> stack, Map<String, Set<String>> tt) {

		// stack is a stack of Token objects.
		// tt is an object; {type:tokens}
		// type is a char, tokens is a list of token strings.
		// The function returns (possibly truncated) stack.
		// It will descend the stack, looking for a Token such that Token.token
		// is a member of tokens. If it does not find that, it will normally
		// (but
		// see "E" below) return stack. If it does find a match, it will remove
		// all the Tokens between the top and the matched Token.
		// If type is "m", that is all it does.
		// If type is "i", it will also remove the matched Token and the top
		// Token.
		// If type is "g", like "i", but add a fake "group" token at the top.
		// If type is "r", it will remove the matched Token, but not the top
		// Token.
		// If type is "e", it will keep the matched Token but not the top Token.
		// If type is "E", it behaves as for type "e", except if there is no
		// match,
		// in which case it will return an empty stack.
		Iterator<?> it = tt.entrySet().iterator();
		int len = stack.size() - 1;

		String typeCopy = "";
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry entry = (Map.Entry) it.next();
			String type = (String) entry.getKey();
			typeCopy = type;		
			Set<String> tokens = (Set<String>) entry.getValue();
			for (int i = len - 1; -1 < i; i--) {
				if (tokens.contains(stack.get(i).token)) {
					Stack<Token> ss = slice(stack, 0, i);
					switch (type) {
					case "m":
						ss.push(stack.get(i));
						ss.push(stack.get(len));
						return ss;
					case "r":
						ss.push(stack.get(len));
						return ss;
					case "i":
						return ss;
					case "g":
						ss.push(fakeToken("group"));
						return ss;
					case "E":
						ss.push(stack.get(i));
						return ss;
					case "e":
						ss.push(stack.get(i));
						return ss;
					}
				}
			}
		}
		return typeCopy.equals("E") ? new Stack<Token>() : stack;
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	@Override
	public boolean accept(String fileName) {

		return acceptExtensions(fileName, "erl");
	}

	@Override
	public State startState() {

		return new State(new Stack<Token>(), false, false);
	}

	@Override
	public String token(StringStream stream, State state) {

		return tokenizer(stream, state);
	}
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-erlang");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("erlang");
	}
}