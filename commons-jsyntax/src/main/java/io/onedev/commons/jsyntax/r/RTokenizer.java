package io.onedev.commons.jsyntax.r;

import java.util.Set;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class RTokenizer extends AbstractTokenizer<RTokenizer.State> {
	
	static final Set<String> atoms = wordsOf("NULL NA Inf NaN NA_integer_ NA_real_ NA_complex_ NA_character_");
	static final Set<String> builtins = wordsOf("list quote bquote eval return call parse deparse");
	static final Set<String> keywords = wordsOf("if else repeat while function for in next break");
	static final Set<String> blockkeywords = wordsOf("if else repeat while function for");
	static final Pattern opChars = Pattern.compile("[+\\-*\\/^<>=!&|~$:]");
	static final int ALIGN_YES = 1, ALIGN_NO = 2, BRACELESS = 4;

	static class Context {
	    String type;
	    int flags;
	    boolean argList;
	    Context prev;

	    public Context(String type, int flags, boolean argList, Context prev) {
	        this.type = type;
	        this.flags = flags;
	        this.argList = argList;
	        this.prev = prev;
	    }
	}
	
	static class State {
	    Processor tokenize;
	    Context ctx;
	    boolean afterIdent;
	    String curPunc;

	    public State(Processor tokenize, Context ctx, boolean afterIdent, String curPunc) {
	        this.tokenize = tokenize;
	        this.ctx = ctx;
	        this.afterIdent = afterIdent;
	        this.curPunc = curPunc;
	    }
	}
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static final Pattern pattern[] = new Pattern[12];
	static {
	    pattern[0] = Pattern.compile("[\\da-f]", Pattern.CASE_INSENSITIVE);
	    pattern[1] = Pattern.compile("\\d");
	    pattern[2] = Pattern.compile("\\d*(?:e[+\\-]?\\d+)?");
	    pattern[3] = Pattern.compile("\\d");
	    pattern[4] = Pattern.compile("\\d*(?:\\.\\d+)?(?:e[+\\-]\\d+)?L?");
	    pattern[5] = Pattern.compile("[^`]+`");
	    pattern[6] = Pattern.compile(".[.\\d]+");
	    pattern[7] = Pattern.compile("[\\w\\.]");
	    pattern[8] = Pattern.compile("[\\w\\.]");
	    pattern[9] = Pattern.compile("\\s*if(\\s+|$)");
	    pattern[10] = Pattern.compile(">>?");
	    pattern[11] = Pattern.compile("[\\(\\){}\\[\\];]");
	}
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		    state.curPunc = "";
		    String ch = stream.next();
		    if (ch.equals("#")) {
		        stream.skipToEnd();
		        return "comment";
		    }
		    else if (ch.equals("0") && !stream.eat("x").isEmpty()) {
		        stream.eatWhile(pattern[0]);
		        return "number";
		    }
		    else if (ch.equals(".") && !stream.eat(pattern[1]).isEmpty()) {
		        stream.match(pattern[2]);
		        return "number";
		    }
		    else if (pattern[3].matcher(ch).matches()) {
		        stream.match(pattern[4]);
		        return "number";
		    }
		    else if (ch.equals("'") || ch.equals("\"")) {
		        state.tokenize = new TokenString(ch);
		        return "string";
		    }
		    else if (ch.equals("`")) {
		        stream.match(pattern[5]);
		        return "variable-3";
		    }
		    else if (ch.equals(".") && !stream.match(pattern[6]).isEmpty()) {
		        return "keyword";
		    }
		    else if (pattern[7].matcher(ch).matches() && !ch.equals("_")) {
		        stream.eatWhile(pattern[8]);
		        Object word = stream.current();
		        if (atoms.contains(word)) return "atom";
		        if (keywords.contains(word)) {
		            if (blockkeywords.contains(word) && !!stream.match(pattern[9],false).isEmpty()) state.curPunc = "block";
		            return "keyword";
		        }
		        if (builtins.contains(word)) return "builtin";
		        return "variable";
		    }
		    else if (ch.equals("%")) {
		        if (stream.skipTo("%")) stream.next();
		        return "operator variable-2";
		    }
		    else if ((ch.equals("<") && !stream.eat("-").isEmpty()) || (ch.equals("<") && stream.match("<-")) || (ch.equals("-") && !stream.match(pattern[10]).isEmpty())) {
		        return "operator arrow";
		    }
		    else if (ch.equals("=") && state.ctx.argList) {
		        return "arg-is";
		    }
		    else if (opChars.matcher(ch).matches()) {
		        if (ch.equals("$")) return "operator dollar";
		        stream.eatWhile(opChars);
		        return "operator";
		    }
		    else if (pattern[11].matcher(ch).matches()) {
		        state.curPunc = ch;
		        if (ch.equals(";")) return "semi";
		        return "";
		    }
		    else {
		        return "";
		    }
		}
	}
	
	static final Pattern pattern2[] = new Pattern[5];
	static {
	    pattern2[0] = Pattern.compile("^[a-f0-9]{2}", Pattern.CASE_INSENSITIVE);
	    pattern2[1] = Pattern.compile("^[a-f0-9]{4}", Pattern.CASE_INSENSITIVE);
	    pattern2[2] = Pattern.compile("^[a-f0-9]{8}", Pattern.CASE_INSENSITIVE);
	    pattern2[3] = Pattern.compile("[0-7]");
	    pattern2[4] = Pattern.compile("^[0-7]{1,2}");
	}
	
	class TokenString implements Processor {
	    String quote;

	    public TokenString(String quote) {
	        this.quote = quote;
	    }

		@Override
		public String process(StringStream stream, State state) {
		    if (!stream.eat("\\").isEmpty()) {
		        String ch = stream.next();
		        if (ch.equals("x"))
		        	stream.match(pattern2[0]);
		        else if ((ch.equals("u") || ch.equals("U")) && !stream.eat("{").isEmpty() && stream.skipTo("}"))
		        	stream.next();
		        else if (ch.equals("u"))
		        	stream.match(pattern2[1]);
		        else if (ch.equals("U"))
		        	stream.match(pattern2[2]);
		        else if (pattern2[3].matcher(ch).matches())
		        	stream.match(pattern2[4]);
		        return "string-2";
		    }
		    else {
		        String next;
		        while (!(next = stream.next()).isEmpty()) {
		            if (next.equals(quote)) {
		                state.tokenize = new TokenBase();
		                break;
		            }
		            if (next.equals("\\")) {
		                stream.backUp(1);
		                break;
		            }
		        }
		        return "string";
		    }
		}
	}
	
	private static void push(State state, String type, StringStream stream) {
		state.ctx = new Context(type, 0, false, state.ctx);
	}
	
	private static void setFlag(State state, int flag) {
		Context ctx = state.ctx;
		state.ctx = new Context(ctx.type, ctx.flags | flag, ctx.argList, ctx.prev);
	}
	
	private static void pop(State state) {
		state.ctx = state.ctx.prev;
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "r", "R");
	}

	@Override
	public State startState() {
		return new State(new TokenBase(), new Context("top", ALIGN_NO, false, null), false, "");
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (stream.sol()) {
	        if ((state.ctx.flags & 3) == 0)
	        	state.ctx.flags |= ALIGN_NO; 
	        if ((state.ctx.flags & BRACELESS) != 0)
	        	pop(state);
	    }
	    if (stream.eatSpace()) return "";
	    String style = state.tokenize.process(stream, state);
	    if (style.equals("comment") == false && (state.ctx.flags & ALIGN_NO) == 0)
	    	setFlag(state, ALIGN_YES);
	    if ((state.curPunc.equals(";") || state.curPunc.equals("{") ||
	    		state.curPunc.equals("}")) && state.ctx.type.equals("block"))
	    	pop(state);
	    if (state.curPunc.equals("{"))
	    	push(state, "}", stream);
	    else if (state.curPunc.equals("(")) {
	        push(state, ")", stream);
	        if (state.afterIdent)
	        	state.ctx.argList = true;
	    }
	    else if (state.curPunc.equals("["))
	    	push(state, "]", stream);
	    else if (state.curPunc.equals("block"))
	    	push(state, "block", stream);
	    else if (state.curPunc.equals(state.ctx.type))
	    	pop(state);
	    else if (state.ctx.type.equals("block") && style.equals("comment") == false)
	    	setFlag(state, BRACELESS);
	    state.afterIdent = style.equals("variable") || style.equals("keyword");
	    return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-rsrc");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("r");
	}
}