package io.onedev.commons.jsyntax.z80;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class Z80Tokenizer extends AbstractTokenizer<Z80Tokenizer.State> {
	
	static final Pattern variables1 = Pattern.compile("^(af?|bc?|c|de?|e|hl?|l|i[xy]?|r|sp)\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern variables2 = Pattern.compile("^(n?[zc]|p[oe]?|m)\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern errors = Pattern.compile("^([hl][xy]|i[xy][hl]|slia|sll)\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern numbers = Pattern.compile("^([\\da-f]+h|[0-7]+o|[01]+b|\\d+d?)\\b", Pattern.CASE_INSENSITIVE);
	
	static final Pattern patterns[] = new Pattern[4];
	static {
	    patterns[0] = Pattern.compile("^(exx?|(ld|cp)([di]r?)?|[lp]ea|pop|push|ad[cd]|cpl|daa|dec|inc|neg|sbc|sub|and|bit|[cs]cf|x?or|res|set|r[lr]c?a?|r[lr]d|s[lr]a|srl|djnz|nop|[de]i|halt|im|in([di]mr?|ir?|irx|2r?)|ot(dmr?|[id]rx|imr?)|out(0?|[di]r?|[di]2r?)|tst(io)?|slp)(\\.([sl]?i)?[sl])?\\b", Pattern.CASE_INSENSITIVE);
	    patterns[1] = Pattern.compile("^(((call|j[pr]|rst|ret[in]?)(\\.([sl]?i)?[sl])?)|(rs|st)mix)\\b", Pattern.CASE_INSENSITIVE);
	    patterns[2] = Pattern.compile("^(exx?|(ld|cp|in)([di]r?)?|pop|push|ad[cd]|cpl|daa|dec|inc|neg|sbc|sub|and|bit|[cs]cf|x?or|res|set|r[lr]c?a?|r[lr]d|s[lr]a|srl|djnz|nop|rst|[de]i|halt|im|ot[di]r|out[di]?)\\b", Pattern.CASE_INSENSITIVE);
	    patterns[3] = Pattern.compile("^(call|j[pr]|ret[in]?|b_?(call|jump))\\b", Pattern.CASE_INSENSITIVE);
	}
	
	final Pattern keywords1, keywords2;
	
	final boolean ez80;
	
	public Z80Tokenizer() {
		this(false);
	}
	
	public Z80Tokenizer(boolean ez80) {
		this.ez80 = ez80;
	    if (ez80) {
	        keywords1 = patterns[0];
	        keywords2 = patterns[1];
	    }
	    else {
	        keywords1 = patterns[2];
	        keywords2 = patterns[3];
	    }
	}
	
	static class State {
	    int context;

	    public State(int context) {
	        this.context = context;
	    }
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "z80");
	}

	@Override
	public State startState() {
		return new State(0);
	}
	
	static final Pattern pattern[] = new Pattern[6];
	static {
	    pattern[0] = Pattern.compile("\\w");
	    pattern[1] = Pattern.compile("\\w");
	    pattern[2] = Pattern.compile("\\\\?.'");
	    pattern[3] = Pattern.compile("\\w");
	    pattern[4] = Pattern.compile("[\\da-f]", Pattern.CASE_INSENSITIVE);
	    pattern[5] = Pattern.compile("[01]");
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (stream.column() == 0) state.context = 0;
	    if (stream.eatSpace()) return "";
	    String w;
	    if (stream.eatWhile(pattern[0])) {
	        if (ez80 && !stream.eat(".").isEmpty()) {
	            stream.eatWhile(pattern[1]);
	        }
	        w = stream.current();
	        if (stream.indentation() != 0) {
	            if ((state.context == 1 || state.context == 4) && variables1.matcher(w).matches()) {
	                state.context = 4;
	                return "var2";
	            }
	            if (state.context == 2 && variables2.matcher(w).matches()) {
	                state.context = 4;
	                return "var3";
	            }
	            if (keywords1.matcher(w).matches()) {
	                state.context = 1;
	                return "keyword";
	            }
	            else if (keywords2.matcher(w).matches()) {
	                state.context = 2;
	                return "keyword";
	            }
	            else if (state.context == 4 && numbers.matcher(w).matches()) {
	                return "number";
	            }
	            if (errors.matcher(w).matches()) return "error";
	        }
	        else if (!stream.match(numbers).isEmpty()) {
	            return "number";
	        }
	        else {
	            return "";
	        }
	    }
	    else if (!stream.eat(";").isEmpty()) {
	        stream.skipToEnd();
	        return "comment";
	    }
	    else if (!stream.eat("\"").isEmpty()) {
	        while (!(w = stream.next()).isEmpty()) {
	            if (w.equals("\"")) break;
	            if (w.equals("\\")) stream.next();
	        }
	        return "string";
	    }
	    else if (!stream.eat("\'").isEmpty()) {
	        if (!stream.match(pattern[2]).isEmpty()) return "number";
	    }
	    else if (!stream.eat(".").isEmpty() || stream.sol() && !stream.eat("#").isEmpty()) {
	        state.context = 5;
	        if (stream.eatWhile(pattern[3])) return "def";
	    }
	    else if (!stream.eat("$").isEmpty()) {
	        if (stream.eatWhile(pattern[4])) return "number";
	    }
	    else if (!stream.eat("%").isEmpty()) {
	        if (stream.eatWhile(pattern[5])) return "number";
	    }
	    else {
	        stream.next();
	    }
	    return "";
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-z80");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("z80");
	}
}
