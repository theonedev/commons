package io.onedev.commons.jsyntax.verilog;

import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class VerilogTokenizer extends AbstractTokenizer<VerilogTokenizer.State> {

	static final Set<String> keywords = wordsOf("accept_on alias always always_comb always_ff always_latch and assert assign assume automatic before begin bind " +
		    "bins binsof bit break buf bufif0 bufif1 byte case casex casez cell chandle checker class clocking cmos config " +
		    "const constraint context continue cover covergroup coverpoint cross deassign default defparam design disable " +
		    "dist do edge else end endcase endchecker endclass endclocking endconfig endfunction endgenerate endgroup " +
		    "endinterface endmodule endpackage endprimitive endprogram endproperty endspecify endsequence endtable endtask " +
		    "enum event eventually expect export extends extern final first_match for force foreach forever fork forkjoin " +
		    "function generate genvar global highz0 highz1 if iff ifnone ignore_bins illegal_bins implements implies import " +
		    "incdir include initial inout input inside instance int integer interconnect interface intersect join join_any " +
		    "join_none large let liblist library local localparam logic longint macromodule matches medium modport module " +
		    "nand negedge nettype new nexttime nmos nor noshowcancelled not notif0 notif1 null or output package packed " +
		    "parameter pmos posedge primitive priority program property protected pull0 pull1 pulldown pullup " +
		    "pulsestyle_ondetect pulsestyle_onevent pure rand randc randcase randsequence rcmos real realtime ref reg " +
		    "reject_on release repeat restrict return rnmos rpmos rtran rtranif0 rtranif1 s_always s_eventually s_nexttime " +
		    "s_until s_until_with scalared sequence shortint shortreal showcancelled signed small soft solve specify " +
		    "specparam static string strong strong0 strong1 struct super supply0 supply1 sync_accept_on sync_reject_on " +
		    "table tagged task this throughout time timeprecision timeunit tran tranif0 tranif1 tri tri0 tri1 triand trior " +
		    "trireg type typedef union unique unique0 unsigned until until_with untyped use uwire var vectored virtual void " +
		    "wait wait_order wand weak weak0 weak1 while wildcard wire with within wor xnor xor");
	static final Set<String> blockKeywords = wordsOf("case checker class clocking config function generate interface module package" +
		    "primitive program property specify sequence table task");
	static final Set<String> statementKeywords = wordsOf("always always_comb always_ff always_latch assert assign assume else export for foreach forever if import initial repeat while");
	
	static final Pattern isOperatorChar = Pattern.compile("[\\+\\-\\*\\/!~&|^%=?:]");
	static final Pattern isBracketChar = Pattern.compile("[\\[\\]{}()]");
	static final Pattern unsignedNumber = Pattern.compile("\\d[0-9_]*");
	static final Pattern decimalLiteral = Pattern.compile("\\d*\\s*'s?d\\s*\\d[0-9_]*", Pattern.CASE_INSENSITIVE);
	static final Pattern binaryLiteral = Pattern.compile("\\d*\\s*'s?b\\s*[xz01][xz01_]*", Pattern.CASE_INSENSITIVE);
	static final Pattern octLiteral = Pattern.compile("\\d*\\s*'s?o\\s*[xz0-7][xz0-7_]*", Pattern.CASE_INSENSITIVE);
	static final Pattern hexLiteral = Pattern.compile("\\d*\\s*'s?h\\s*[0-9a-fxz?][0-9a-fxz?_]*", Pattern.CASE_INSENSITIVE);
	static final Pattern realLiteral = Pattern.compile("(\\d[\\d_]*(\\.\\d[\\d_]*)?E-?[\\d_]+)|(\\d[\\d_]*\\.\\d[\\d_]*)", Pattern.CASE_INSENSITIVE);
	static final Pattern closingBracketOrWord = Pattern.compile("^((\\w+)|[)}\\]])");
	static final Pattern closingBracket = Pattern.compile("[)}\\]]");
	
	static final Map<String, Processor> emptyHooks = Maps.newHashMap();
	static final Map<String, String> openClose = Maps.newHashMap();
	static {
		for (String keyword : blockKeywords) {
			openClose.put(keyword, "end" + keyword);
		}
		openClose.put("begin", "end");
		openClose.put("casex", "endcase");
		openClose.put("casez", "endcase");
		openClose.put("do", "while");
		openClose.put("fork", "join;join_any;join_none");
		openClose.put("covergroup", "endgroup");
	}
	
	@FunctionalInterface
	static interface Processor {
	    String process(StringStream stream, State state);
	}
	
	static class Context {
	    String type;
	    boolean align;

	    public Context(String type, boolean align) {
	        this.type = type;
	        this.align = align;
	    }
	}
	
	static class State {
	    Processor tokenize;
	    Stack<Context> context;
	    boolean startOfLine;
	    String curPunc;
	    String curKeyword;
	    Map<String, Object> attr;

	    public State(Processor tokenize, Stack<Context> context, boolean startOfLine) {
	        this.tokenize = tokenize;
	        this.context = context;
	        this.startOfLine = startOfLine;
	        this.curPunc = "";
	        this.curKeyword = "";
	        this.attr = Maps.newHashMap();
	    }
	}
	
	static final Pattern pattern[] = new Pattern[5];
	static {
	    pattern[0] = Pattern.compile("[,;:\\.]");
	    pattern[1] = Pattern.compile("[\\w\\$_]");
	    pattern[2] = Pattern.compile("[\\w\\$_]");
	    pattern[3] = Pattern.compile("[\\d_.]");
	    pattern[4] = Pattern.compile("[\\w\\$_]");
	}
	
	final Processor tokenBase = (stream, state) -> {
	    String ch = stream.peek(), style;
	    if (hooks().containsKey("tokenBase") && !(style = hooks().get("tokenBase").process(stream, state)).isEmpty())
	    	return style;
	    if (pattern[0].matcher(ch).matches()) {
	        state.curPunc = stream.next();
	        return "";
	    }
	    if (isBracketChar.matcher(ch).matches()) {
	        state.curPunc = stream.next();
	        return "bracket";
	    }
	    if (ch.equals("`")) {
	        stream.next();
	        if (stream.eatWhile(pattern[1])) {
	            return "def";
	        }
	        else {
	            return "";
	        }
	    }
	    if (ch.equals("$")) {
	        stream.next();
	        if (stream.eatWhile(pattern[2])) {
	            return "meta";
	        }
	        else {
	            return "";
	        }
	    }
	    if (ch.equals("#")) {
	        stream.next();
	        stream.eatWhile(pattern[3]);
	        return "def";
	    }
	    if (ch.equals("\"")) {
	        stream.next();
	        state.tokenize = tokenString(ch);
	        return state.tokenize.process(stream, state);
	    }
	    if (ch.equals("/")) {
	        stream.next();
	        if (!stream.eat("*").isEmpty()) {
	            state.tokenize = tokenComment();
	            return tokenComment().process(stream, state);
	        }
	        if (!stream.eat("/").isEmpty()) {
	            stream.skipToEnd();
	            return "comment";
	        }
	        stream.backUp(1);
	    }
	    if (!stream.match(realLiteral).isEmpty() 
	    		|| !stream.match(decimalLiteral).isEmpty()
	    		|| !stream.match(binaryLiteral).isEmpty()
	    		|| !stream.match(octLiteral).isEmpty()
	    		|| !stream.match(hexLiteral).isEmpty()
	    		|| !stream.match(unsignedNumber).isEmpty()
	    		|| !stream.match(realLiteral).isEmpty()) {
	        return "number";
	    }
	    if (stream.eatWhile(isOperatorChar)) {
	        return "meta";
	    }
	    if (stream.eatWhile(pattern[4])) {
	        String cur = stream.current();
	        if (keywords.contains(cur)) {
	            if (openClose.containsKey(cur)) {
	                state.curPunc = "newblock";
	            }
	            if (statementKeywords.contains(cur)) {
	                state.curPunc = "newstatement";
	            }
	            state.curKeyword = cur;
	            return "keyword";
	        }
	        return "variable";
	    }
	    stream.next();
	    return "";
	};
	
	Processor tokenString(String quote) {
		return (stream, state) -> {
		    boolean escaped = false, end = false;
		    String next;
		    while (!(next = stream.next()).isEmpty()) {
		        if (next.equals(quote) && !escaped) {
		            end = true;
		            break;
		        }
		        escaped = !escaped && next.equals("\\");
		    }
		    if (end || !(escaped /*|| multiLineStrings*/)) state.tokenize = tokenBase;
		    return "string";
		};
	}
	
	Processor tokenComment() {
		return (stream, state) -> {
		    boolean maybeEnd = false;
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (ch.equals("/") && maybeEnd) {
		            state.tokenize = tokenBase;
		            break;
		        }
		        maybeEnd = (ch.equals("*"));
		    }
		    return "comment";
		};
	}

	static void pushContext(State state, String type) {
		state.context.push(new Context(type, false));
	}
	
	static Context popContext(State state) {
		if (state.context.isEmpty())
			return null;
		return state.context.pop();
	}

	static boolean isClosing(String text, String contextClosing) {
	    if (text.equals(contextClosing)) {
	        return true;
	    }
	    else {
	        String[] closingKeywords = contextClosing.split(";");
	        for (String i : closingKeywords) {
	            if (text.equals(i)) {
	                return true;
	            }
	        }
	        return false;
	    }
	}

	protected Map<String, Processor> hooks() {
		return emptyHooks;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "v");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-verilog");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("verilog");
	}

	@Override
	public State startState() {
		Stack<Context> stack = new Stack<Context>();
		stack.push(new Context("top", false));
		return new State(null, stack, true);
	}

	@Override
	public String token(StringStream stream, State state) {
	    Context ctx = state.context.isEmpty() ? new Context("", false) : state.context.peek();
	    if (stream.sol()) {
	        state.startOfLine = true;
	    }
	    if (hooks().containsKey("token")) {
	        String style = hooks().get("token").process(stream, state);
	        if (!style.equals("undefined"))
	        	return style;
	    }
	    if (stream.eatSpace()) return "";
	    state.curPunc = "";
	    state.curKeyword = "";
	    String style = (state.tokenize != null ? state.tokenize : tokenBase).process(stream, state);
	    if (style.equals("comment") || style.equals("meta") || style.equals("variable")) return style;
	    if (!ctx.align) ctx.align = true;
	    if (state.curPunc.equals(ctx.type)) {
	        popContext(state);
	    }
	    else if ((state.curPunc.equals(";") && ctx.type.equals("statement"))
	    		|| (!ctx.type.isEmpty() && isClosing(state.curKeyword, ctx.type))) {
	        ctx = popContext(state);
	        while (!state.context.isEmpty() && ctx.type.equals("statement"))
	        	ctx = popContext(state);
	    }
	    else if (state.curPunc.equals("{")) {
	        pushContext(state, "}");
	    }
	    else if (state.curPunc.equals("[")) {
	        pushContext(state, "]");
	    }
	    else if (state.curPunc.equals("(")) {
	        pushContext(state, ")");
	    }
	    else if (ctx != null && ctx.type.equals("endcase") && state.curPunc.equals(":")) {
	        pushContext(state, "statement");
	    }
	    else if (state.curPunc.equals("newstatement")) {
	        pushContext(state, "statement");
	    }
	    else if (state.curPunc.equals("newblock")) {
	        if (state.curKeyword.equals("function") && ctx != null && (ctx.type.equals("statement") || ctx.type.equals("endgroup"))) {
	        }
	        else if (state.curKeyword.equals("task") && ctx != null && ctx.type.equals("statement")) {
	        }
	        else {
	            String close = openClose.get(state.curKeyword);
	            pushContext(state, close);
	        }
	    }
	    state.startOfLine = false;
	    return style;
	}
}
