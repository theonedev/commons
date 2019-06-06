package io.onedev.commons.jsyntax.haxe;

import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class HaxeTokenizer extends AbstractTokenizer<HaxeTokenizer.State> {
	
	static final Keyword A = kw("keyword a"), B = kw("keyword b"), C = kw("keyword c");
	static final Keyword operator = kw("operator");
	static final Keyword atom = new Keyword("atom", "atom");
	static final Keyword attribute = new Keyword("attribute", "attribute");
	static final Keyword type = kw("typedef");
	
	static final Map<String, Keyword> keywords = Maps.newHashMap();
	static {
		keywords.put("if", A);
		keywords.put("while", A);
		keywords.put("else", B);
		keywords.put("do", B);
		keywords.put("try", B);
		keywords.put("return", C);
		keywords.put("break", C);
		keywords.put("continue", C);
		keywords.put("new", C);
		keywords.put("throw", C);
		keywords.put("var", kw("var"));
		keywords.put("inline", attribute);
		keywords.put("static", attribute);
		keywords.put("using", kw("import"));
		keywords.put("public", attribute);
		keywords.put("private", attribute);
		keywords.put("cast", kw("cast"));
		keywords.put("import", kw("import"));
		keywords.put("macro", kw("macro"));
		keywords.put("function", kw("function"));
		keywords.put("catch", kw("catch"));
		keywords.put("untyped", kw("untyped"));
		keywords.put("callback", kw("cb"));
		keywords.put("for", kw("for"));
		keywords.put("switch", kw("switch"));
		keywords.put("case", kw("case"));
		keywords.put("default", kw("default"));
		keywords.put("in", operator);
		keywords.put("never", kw("property_access"));
		keywords.put("trace", kw("trace"));
		keywords.put("class", type);
		keywords.put("abstract", type);
		keywords.put("enum", type);
		keywords.put("interface", type);
		keywords.put("typedef", type);
		keywords.put("extends", type);
		keywords.put("implements", type);
		keywords.put("dynamic", type);
		keywords.put("true", atom);
		keywords.put("false", atom);
		keywords.put("null", atom);
	}
	
	static final Pattern isOperatorChar = Pattern.compile("[+\\-*&%=<>!?|]");
	
	static final Set<String> atomicTypes = Sets.newHashSet("atom", "number", "variable", "string", "regexp");

	static class Keyword {
	    String type;
	    String style;

	    public Keyword(String type, String style) {
	        this.type = type;
	        this.style = style;
	    }
	}
	
	static class Vars {
	    String name;
	    Vars next;

	    public Vars(String name, Vars next) {
	        this.name = name;
	        this.next = next;
	    }
	}
	
	static class Context {
	    Vars vars;
	    Context prev;

	    public Context(Vars vars, Context prev) {
	        this.vars = vars;
	        this.prev = prev;
	    }
	}
	
	static class HaxeLexical {
	    String type;
	    HaxeLexical prev;

	    public HaxeLexical(String type, HaxeLexical prev) {
	        this.type = type;
	        this.prev = prev;
	    }
	}

	static class State {
	    Processor tokenize;
	    boolean reAllowed;
	    boolean kwAllowed;
	    Stack<Combinator> cc;
	    HaxeLexical lexical;
	    Vars localVars;
	    Vars importestypes;
	    Context context;
	    String type;
	    String content;
	    String marked;

	    public State(Processor tokenize, boolean reAllowed, boolean kwAllowed, Stack<Combinator> cc, HaxeLexical lexical, Vars localVars, Vars importestypes, Context context) {
	        this.tokenize = tokenize;
	        this.reAllowed = reAllowed;
	        this.kwAllowed = kwAllowed;
	        this.cc = cc;
	        this.lexical = lexical;
	        this.localVars = localVars;
	        this.importestypes = importestypes;
	        this.context = context;
	        this.type = "";
	        this.content = "";
	        this.marked = "";
	    }
	}

	@FunctionalInterface
	static interface Processor {
	    String process(StringStream stream, State state);
	}
	
	@FunctionalInterface
	static interface Executor {
		boolean process(State state, String type, String value);
	}
	
	static class Combinator {
		Executor executor;
		boolean lex;
		
		public Combinator(Executor executor, boolean lex) {
			this.executor = executor;
			this.lex = lex;
		}
	}
	
	static Keyword kw(String type) {
		return new Keyword(type, "keyword");
	}
	
	static String chain(StringStream stream, State state, Processor f) {
		state.tokenize = f;
		return f.process(stream, state);
	}
	
	static boolean toUnescaped(StringStream stream, String end) {
	    boolean escaped = false;
	    String next;
	    while (!(next = stream.next()).isEmpty()) {
	        if (next.equals(end) && !escaped) return true;
	        escaped = !escaped && next.equals("\\");
	    }
	    return false;
	}
	
	static String ret(String type, String style, String content, State state) {
		state.type = type;
		state.content = content;
		return style;
	}
	
	static final Pattern pattern[] = new Pattern[13];
	static {
	    pattern[0] = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.]");
	    pattern[1] = Pattern.compile("[\\da-f]", Pattern.CASE_INSENSITIVE);
	    pattern[2] = Pattern.compile("\\d");
	    pattern[3] = Pattern.compile("\\d");
	    pattern[4] = Pattern.compile("^\\d*(?:\\.\\d*(?!\\.))?(?:[eE][+\\-]?\\d+)?");
	    pattern[5] = Pattern.compile("\\/");
	    pattern[6] = Pattern.compile("[gimsu]");
	    pattern[7] = Pattern.compile(":");
	    pattern[8] = Pattern.compile("[\\w_]");
	    pattern[9] = Pattern.compile("[A-Z]");
	    pattern[10] = Pattern.compile("[\\w_<>]");
	    pattern[11] = Pattern.compile("[\\w_]");
	    pattern[12] = Pattern.compile("x", Pattern.CASE_INSENSITIVE);
	}
	
	static final Processor haxeTokenBase = (stream, state) -> {
	    String ch = stream.next();
	    if (ch.equals("\"") || ch.equals("'")) {
	        return chain(stream, state, haxeTokenString(ch));
	    }
	    else if (pattern[0].matcher(ch).matches()) {
	        return ret(ch, "", "", state);
	    }
	    else if (ch.equals("0") && !stream.eat(pattern[12]).isEmpty()) {
	        stream.eatWhile(pattern[1]);
	        return ret("number", "number", "", state);
	    }
	    else if (pattern[2].matcher(ch).matches() || ch.equals("-") && !stream.eat(pattern[3]).isEmpty()) {
	        stream.match(pattern[4]);
	        return ret("number", "number", "", state);
	    }
	    else if (state.reAllowed && (ch.equals("~") && !stream.eat(pattern[5]).isEmpty())) {
	        toUnescaped(stream, "/");
	        stream.eatWhile(pattern[6]);
	        return ret("regexp", "string-2", "", state);
	    }
	    else if (ch.equals("/")) {
	        if (!stream.eat("*").isEmpty()) {
	            return chain(stream, state, haxeTokenComment());
	        }
	        else if (!stream.eat("/").isEmpty()) {
	            stream.skipToEnd();
	            return ret("comment", "comment", "", state);
	        }
	        else {
	            stream.eatWhile(isOperatorChar);
	            return ret("operator", "", stream.current(), state);
	        }
	    }
	    else if (ch.equals("#")) {
	        stream.skipToEnd();
	        return ret("conditional", "meta", "", state);
	    }
	    else if (ch.equals("@")) {
	        stream.eat(pattern[7]);
	        stream.eatWhile(pattern[8]);
	        return ret("metadata", "meta", "", state);
	    }
	    else if (isOperatorChar.matcher(ch).matches()) {
	        stream.eatWhile(isOperatorChar);
	        return ret("operator", "", stream.current(), state);
	    }
	    else {
	        String word;
	        if (pattern[9].matcher(ch).matches()) {
	            stream.eatWhile(pattern[10]);
	            word = stream.current();
	            return ret("type", "variable-3", word, state);
	        }
	        else {
	            stream.eatWhile(pattern[11]);
	            word = stream.current();
	            Keyword known = null;
	            if (keywords.containsKey(word)) {
	            	known = keywords.get(word);
	            }
	            return (known != null && state.kwAllowed) ? ret(known.type, known.style, word, state) : ret("variable", "variable", word, state);
	        }
	    }
	};
	
	static Processor haxeTokenString(String quote) {
		return (stream, state) -> {
			if (toUnescaped(stream, quote))
				state.tokenize = haxeTokenBase;
			return ret("string", "string", "", state);
		};
	}
	
	static Processor haxeTokenComment() {
		return (stream, state) -> {
		    boolean maybeEnd = false;
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (ch.equals("/") && maybeEnd) {
		            state.tokenize = haxeTokenBase;
		            break;
		        }
		        maybeEnd = (ch.equals("*"));
		    }
		    return ret("comment", "comment", "", state);
		};
	}
	
	static boolean inScope(State state, String varName) {
		for (Vars v = state.localVars; v != null; v = v.next) {
			if (v.name.equals(varName)) {
				return true;
			}
		}
		return false;
	}
	
	static String parseHaxe(State state, String style, String type, String content) {
		state.marked = "";
		while (true) {
			Combinator combinator = state.cc.isEmpty() ? statement() : state.cc.pop();
			if (combinator.executor.process(state, type, content)) {
				while (!state.cc.isEmpty() && state.cc.peek().lex) {
					state.cc.pop().executor.process(state, type, content);
				}
				if (!state.marked.isEmpty()) return state.marked;
				if (type.equals("variable") && inScope(state, content)) return "variable-2";
				if (type.equals("variable") && imported(state, content)) return "variable-3";
				return style;
			}
		}
	}
	
	static final Pattern patternForImported = Pattern.compile("[a-z]");
	
	static boolean imported(State state, String typename) {
		if (patternForImported.matcher(typename.charAt(0) + "").matches())
			return false;
		for (Vars v = state.importestypes; v != null; v = v.next) {
			if (v.name.equals(typename)) {
				return true;
			}
		}
		return false;
	}
	
	static void registerimport(State state, String importname) {
		for (Vars v = state.importestypes; v != null; v = v.next) {
			if (v.name.equals(importname)) {
				return;
			}
		}
		state.importestypes = new Vars(importname, state.importestypes);
	}
	
	static boolean pass(State state, Combinator... combinators) {
		for (int i = combinators.length - 1; i >= 0; i--) {
			state.cc.push(combinators[i]);
		}
		return false;
	}
	
	static boolean cont(State state, Combinator... combinators) {
		pass(state, combinators);
		return true;
	}
	
	static boolean inList(String name, Vars list) {
		for (Vars v = list; v != null; v = v.next) {
			if (v.name.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	static void register(State state, String varname) {
	    if (state.context != null) {
	    	state.marked = "def";
	    	if (inList(varname, state.localVars)) return;
	    	state.localVars = new Vars(varname, state.localVars);
	    }
	}
	
	static Combinator pushcontext() {
		return new Combinator((state, type, value) -> {
			if (state.context == null)
				state.localVars = new Vars("this", null);
			state.context = new Context(state.localVars, state.context);
			return false;
		}, false);
	}
	
	static Combinator popcontext() {
		return new Combinator((state, type, value) -> {
			state.localVars = state.context.vars;
			state.context = state.context.prev;
			return false;
		}, true);
	}
	
	static Combinator pushlex(String _type) {
		return new Combinator((state, type, value) -> {
			state.lexical = new HaxeLexical(_type, state.lexical);
			return false;
		}, true);
	}
	
	static Combinator poplex() {
		return new Combinator((state, type, value) -> {
			if (state.lexical.prev != null) {
				state.lexical = state.lexical.prev;
			}
			return false;
		}, true);
	}
	
	static Combinator expect(String wanted) {
		return new Combinator((state, type, value) -> {
			if (type.equals(wanted)) return cont(state);
			else if (wanted.equals(";")) return pass(state);
			else return cont(state, expect(wanted));
		}, false);
	}
	
	static Combinator statement() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("@")) return cont(state, metadef());
		    if (type.equals("var")) return cont(state, pushlex("vardef"), vardef1(), expect(";"), poplex());
		    if (type.equals("keyword a")) return cont(state, pushlex("form"), expression(), statement(), poplex());
		    if (type.equals("keyword b")) return cont(state, pushlex("form"), statement(), poplex());
		    if (type.equals("{")) return cont(state, pushlex("}"), pushcontext(), block(), poplex(), popcontext());
		    if (type.equals(";")) return cont(state);
		    if (type.equals("attribute")) return cont(state, maybeattribute());
		    if (type.equals("function")) return cont(state, functiondef());
		    if (type.equals("for")) return cont(state, pushlex("form"), expect("("), pushlex(")"), forspec1(), expect(")"), poplex(), statement(), poplex());
		    if (type.equals("variable")) return cont(state, pushlex("stat"), maybelabel());
		    if (type.equals("switch")) return cont(state, pushlex("form"), expression(), pushlex("}"), expect("{"), block(), poplex(), poplex());
		    if (type.equals("case")) return cont(state, expression(), expect(":"));
		    if (type.equals("default")) return cont(state, expect(":"));
		    if (type.equals("catch")) return cont(state, pushlex("form"), pushcontext(), expect("("), funarg(), expect(")"), statement(), poplex(), popcontext());
		    if (type.equals("import")) return cont(state, importdef(), expect(";"));
		    if (type.equals("typedef")) return cont(state, typedef());
		    return pass(state, pushlex("stat"), expression(), expect(";"), poplex());
		}, false);
	}
	
	static Combinator expression() {
		return new Combinator((state, type, value) -> {
		    if (atomicTypes.contains(type)) return cont(state, maybeoperator());
		    if (type.equals("type")) return cont(state, maybeoperator());
		    if (type.equals("function")) return cont(state, functiondef());
		    if (type.equals("keyword c")) return cont(state, maybeexpression());
		    if (type.equals("(")) return cont(state, pushlex(")"), maybeexpression(), expect(")"), poplex(), maybeoperator());
		    if (type.equals("operator")) return cont(state, expression());
		    if (type.equals("[")) return cont(state, pushlex("]"), commasep(maybeexpression(), "]"), poplex(), maybeoperator());
		    if (type.equals("{")) return cont(state, pushlex("}"), commasep(objprop(), "}"), poplex(), maybeoperator());
		    return cont(state);
		}, false);
	}
	
	static final Pattern patternForMaybeexpression = Pattern.compile("[;\\}\\)\\],]");
	
	static Combinator maybeexpression() {
		return new Combinator((state, type, value) -> {
		    if (patternForMaybeexpression.matcher(type).matches()) return pass(state);
		    return pass(state, expression());
		}, false);
	}
	
	static final Pattern patternForMaybeoperator = Pattern.compile("\\+\\+|--");
	
	static Combinator maybeoperator() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("operator") && patternForMaybeoperator.matcher(value).matches()) return cont(state, maybeoperator());
		    if (type.equals("operator") || type.equals(":")) return cont(state, expression());
		    if (type.equals(";")) return false;
		    if (type.equals("(")) return cont(state, pushlex(")"), commasep(expression(), ")"), poplex(), maybeoperator());
		    if (type.equals(".")) return cont(state, property(), maybeoperator());
		    if (type.equals("[")) return cont(state, pushlex("]"), expression(), expect("]"), poplex(), maybeoperator());
		    return false;
		}, false);
	}
	
	static Combinator maybeattribute() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("attribute")) return cont(state, maybeattribute());
		    if (type.equals("function")) return cont(state, functiondef());
		    if (type.equals("var")) return cont(state, vardef1());
		    return false;
		}, false);
	}
	
	static Combinator metadef() {
		return new Combinator((state, type, value) -> {
		    if (type.equals(":")) return cont(state, metadef());
		    if (type.equals("variable")) return cont(state, metadef());
		    if (type.equals("(")) return cont(state, pushlex(")"), commasep(metaargs(), ")"), poplex(), statement());
		    return false;
		}, false);
	}
	
	static Combinator metaargs() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable")) return cont(state);
			return false;
		}, false);
	}
	
	static final Pattern patternForImportdef = Pattern.compile("[A-Z]");
	
	static Combinator importdef() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable") && patternForImportdef.matcher(value.charAt(0) + "").matches()) {
		        registerimport(state, value);
		        return cont(state);
		    } else if (type.equals("variable") || type.equals("property") || type.equals(".") || value.equals("*")) {
		    	return cont(state, importdef());
		    } else {
		    	return false;
		    }
		}, false);
	}
	
	static Combinator typedef() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable") && patternForImportdef.matcher(value.charAt(0) + "").matches()) {
		        registerimport(state, value);
		        return cont(state);
		    } else if (type.equals("type") && patternForImportdef.matcher(value.charAt(0) + "").matches()) {
		        return cont(state);
		    } else {
		    	return false;
		    }
		}, false);
	}
	
	static Combinator maybelabel() {
		return new Combinator((state, type, value) -> {
		    if (type.equals(":")) return cont(state, poplex(), statement());
		    return pass(state, maybeoperator(), expect(";"), poplex());
		}, false);
	}
	
	static Combinator property() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable")) {
		        state.marked = "property";
		        return cont(state);
		    } else {
		    	return false;
		    }
		}, false);
	}
	
	static Combinator objprop() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable")) state.marked = "property";
		    if (atomicTypes.contains(type)) return cont(state, expect(":"), expression());
		    return false;
		}, false);
	}
	
	static Combinator proceed(Combinator what, String end) {
		return new Combinator((state, type, value) -> {
		    if (type.equals(",")) return cont(state, what, proceed(what, end));
		    if (type.equals(end)) return cont(state);
		    return cont(state, expect(end));
		}, false);
	}
	
	static Combinator commasep(Combinator what, String end) {
		return new Combinator((state, type, value) -> {
			if (type.equals(end)) return cont(state);
			else return pass(state, what, proceed(what, end));
		}, false);
	}
	
	static Combinator block() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("}")) return cont(state);
		    return pass(state, statement(), block());
		}, false);
	}
	
	static Combinator vardef1() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable")) {
		        register(state, value);
		        return cont(state, typeuse(), vardef2());
		    }
		    return cont(state);
		}, false);
	}
	
	static Combinator vardef2() {
		return new Combinator((state, type, value) -> {
		    if (value.equals("=")) return cont(state, expression(), vardef2());
		    if (type.equals(",")) return cont(state, vardef1());
		    return false;
		}, false);
	}
	
	static Combinator forspec1() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable")) {
		        register(state, value);
		        return cont(state, forin(), expression());
		    } else {
		        return pass(state);
		    }
		}, false);
	}
	
	static Combinator forin() {
		return new Combinator((state, type, value) -> {
		    if (value.equals("in")) return cont(state);
		    return false;
		}, false);
	}
	
	static Combinator functiondef() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable") || type.equals("type")) {
		        register(state, value);
		        return cont(state, functiondef());
		    }
		    if (value.equals("new")) return cont(state, functiondef());
		    if (type.equals("(")) return cont(state, pushlex(")"), pushcontext(), commasep(funarg(), ")"), poplex(), typeuse(), statement(), popcontext());
		    return false;
		}, false);
	}
	
	static Combinator typeuse() {
		return new Combinator((state, type, value) -> {
		    if (type.equals(":")) return cont(state, typestring());
		    return false;
		}, false);
	}
	
	static Combinator typestring() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("type")) return cont(state);
		    if (type.equals("variable")) return cont(state);
		    if (type.equals("{")) return cont(state, pushlex("}"), commasep(typeprop(), "}"), poplex());
		    return false;
		}, false);
	}
	
	static Combinator typeprop() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable")) return cont(state, typeuse());
		    return false;
		}, false);
	}
	
	static Combinator funarg() {
		return new Combinator((state, type, value) -> {
		    if (type.equals("variable")) {
		        register(state, value);
		        return cont(state, typeuse());
		    } else {
		    	return false;
		    }
		}, false);
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "hx");
	}

	@Override
	public State startState() {
		Vars defaulttypes = new Vars("Int", null);
		defaulttypes = new Vars("Float", defaulttypes);
		defaulttypes = new Vars("String", defaulttypes);
		defaulttypes = new Vars("Void", defaulttypes);
		defaulttypes = new Vars("Std", defaulttypes);
		defaulttypes = new Vars("Bool", defaulttypes);
		defaulttypes = new Vars("Dynamic", defaulttypes);
		defaulttypes = new Vars("Array", defaulttypes);
		return new State(haxeTokenBase, true, true, new Stack<Combinator>(),
				new HaxeLexical("block", null), null, defaulttypes, null);
	}
	
	static final Pattern patternForToken = Pattern.compile("^[\\[{}\\(,;:]$");
	
	@Override
	public String token(StringStream stream, State state) {
	    if (stream.eatSpace()) return "";
	    String style = state.tokenize.process(stream, state);
	    if (state.type.equals("comment")) return style;
	    state.reAllowed = !!(state.type.equals("operator") || state.type.equals("keyword c") 
	    		|| patternForToken.matcher(state.type).matches());
	    state.kwAllowed = !state.type.equals(".");
	    return parseHaxe(state, style, state.type, state.content);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-haxe");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("haxe");
	}
}
