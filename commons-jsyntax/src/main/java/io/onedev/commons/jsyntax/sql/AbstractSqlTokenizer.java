package io.onedev.commons.jsyntax.sql;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public abstract class AbstractSqlTokenizer extends AbstractTokenizer<AbstractSqlTokenizer.State> {

	static final String sqlKeywords = "alter and as asc between by count create delete desc distinct drop from group having in insert into is join like not on or order select set table union update values where limit ";
	static final Set<String> _client = Sets.newHashSet();
	static final Set<String> _atoms = Sets.newHashSet("false", "true", "null");
	static final Set<String> _builtin = Sets.newHashSet();
	static final Set<String> _keywords = Sets.newHashSet();
	static final Pattern _operatorChars = Pattern.compile("^[*+\\-%<>!=&|~^]");
	static final Set<String> _support = Sets.newHashSet();
	static final Map<String, Processor> _hooks = Maps.newHashMap();
	static final Set<String> _dateSQL = Sets.newHashSet("date", "time", "timestamp");
	
	static interface Processor {
	    String process(StringStream stream, State state);
	}
	
	static class State {
		Processor tokenize;

		public State(Processor tokenize) {
			this.tokenize = tokenize;
		}
	}
	
	public Set<String> client() {
		return _client;
	}
	
	public Set<String> atoms() {
		return _atoms;
	}
	
	public Set<String> builtin() {
		return _builtin;
	}
	
	public Set<String> keywords() {
		return _keywords;
	}
	
	public Pattern operatorChars() {
		return _operatorChars;
	}
	
	public Set<String> support() {
		return _support;
	}
	
	public Map<String, Processor> hooks() {
		return _hooks;
	}
	
	public Set<String> dateSQL() {
		return _dateSQL;
	}
	
	static final Pattern patternForId = Pattern.compile("\\w");
	
	static final Processor hookIdentifier = (stream, state) -> {
	    String ch;
	    while (!(ch = stream.next()).isEmpty()) {
	        if (ch.equals("`") && !!stream.eat("`").isEmpty()) return "variable-2";
	    }
	    stream.backUp(stream.current().length() - 1);
	    return stream.eatWhile(patternForId) ? "variable-2" : "";
	};
	
	static final Pattern patternForVar[] = new Pattern[7];
	static {
		patternForVar[0] = Pattern.compile("^session\\.");
		patternForVar[1] = Pattern.compile("^local\\.");
		patternForVar[2] = Pattern.compile("^global\\.");
		patternForVar[3] = Pattern.compile("^.*'");
		patternForVar[4] = Pattern.compile("^.*\"");
		patternForVar[5] = Pattern.compile("^.*`");
		patternForVar[6] = Pattern.compile("^[0-9a-zA-Z$\\.\\_]+");
	}
	
	static final Processor hookVar = (stream, state) -> {
	    if (!stream.eat("@").isEmpty()) {
	        stream.match(patternForVar[0]);
	        stream.match(patternForVar[1]);
	        stream.match(patternForVar[2]);
	    }
	    if (!stream.eat("'").isEmpty()) {
	        stream.match(patternForVar[3]);
	        return "variable-2";
	    }
	    else if (!stream.eat("\"").isEmpty()) {
	        stream.match(patternForVar[4]);
	        return "variable-2";
	    }
	    else if (!stream.eat("`").isEmpty()) {
	        stream.match(patternForVar[5]);
	        return "variable-2";
	    }
	    else if (!stream.match(patternForVar[6]).isEmpty()) {
	        return "variable-2";
	    }
	    return "";
	};
	
	static final Pattern patternForClient = Pattern.compile("^[a-zA-Z.#!?]");
	
	static final Processor hookClient = (stream, state) -> {
	    if (!stream.eat("N").isEmpty()) {
	        return "atom";
	    }
	    return !stream.match(patternForClient).isEmpty() ? "variable-2" : "";
	};

	@Override
	public State startState() {
		return new State(tokenBase);
	}
	
	static final Pattern pattern[] = new Pattern[14];
	static {
	    pattern[0] = Pattern.compile("^[xX][0-9a-fA-F]+");
	    pattern[1] = Pattern.compile("^'[0-9a-fA-F]+'");
	    pattern[2] = Pattern.compile("^'[01]+'");
	    pattern[3] = Pattern.compile("^b[01]+");
	    pattern[4] = Pattern.compile("^[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?");
	    pattern[5] = Pattern.compile("[a-z][a-z0-9]*", Pattern.CASE_INSENSITIVE);
	    pattern[6] = Pattern.compile("^[\\(\\),\\;\\[\\]]");
	    pattern[7] = Pattern.compile("^(?:\\d+(?:e[+-]?\\d+)?)", Pattern.CASE_INSENSITIVE);
	    pattern[8] = Pattern.compile("^[a-zA-Z_]+");
	    pattern[9] = Pattern.compile("^( )*(d|D|t|T|ts|TS)( )*'[^']*'( )*}");
	    pattern[10] = Pattern.compile("^( )*(d|D|t|T|ts|TS)( )*\"[^\"]*\"( )*}");
	    pattern[11] = Pattern.compile("^[_\\w\\d]");
	    pattern[12] = Pattern.compile("^( )+'[^']*'");
	    pattern[13] = Pattern.compile("^( )+\"[^\"]*\"");
	}
	
	final Processor tokenBase = (stream, state) -> {
	    String ch = stream.next();
	    if (hooks().containsKey(ch)) {
	        String result = hooks().get(ch).process(stream, state);
	        if (!result.equals("false")) return result;
	    }
	    if (support().contains("hexNumber")
	    		&& ((ch.equals("0")
	    				&& !stream.match(pattern[0]).isEmpty())
	    				|| (ch.equals("x") || ch.equals("X"))
	    				&& !stream.match(pattern[1]).isEmpty())) {
	        return "number";
	    }
	    else if (support().contains("binaryNumber")
	    		&& (((ch.equals("b") || ch.equals("B"))
	    				&& !stream.match(pattern[2]).isEmpty())
	    				|| (ch.equals("0") && !stream.match(pattern[3]).isEmpty()))) {
	        return "number";
	    }
	    else if ((int)ch.charAt(0) > 47 && (int)ch.charAt(0) < 58) {
	        stream.match(pattern[4]);
	        if (support().contains("decimallessFloat"))
	        	stream.eat(".");
	        return "number";
	    }
	    else if (ch.equals("?") && (stream.eatSpace() || stream.eol() || !stream.eat(";").isEmpty())) {
	        return "variable-3";
	    }
	    else if (ch.equals("'") || (ch.equals("\"") && support().contains("doubleQuote"))) {
	        state.tokenize = tokenLiteral(ch);
	        return state.tokenize.process(stream, state);
	    }
	    else if ((((support().contains("nCharCast")
	    		&& (ch.equals("n") || ch.equals("N"))) ||
	    		(support().contains("charsetCast")
	    				&& ch.equals("_") && !stream.match(pattern[5]).isEmpty()))
	    		&& (stream.peek().equals("'") || stream.peek().equals("\"")))) {
	        return "keyword";
	    }
	    else if (pattern[6].matcher(ch).matches()) {
	        return "";
	    }
	    else if (support().contains("commentSlashSlash") && ch.equals("/") && !stream.eat("/").isEmpty()) {
	        stream.skipToEnd();
	        return "comment";
	    }
	    else if ((support().contains("commentHash")
	    		&& ch.equals("#")) || (ch.equals("-")
	    				&& !stream.eat("-").isEmpty()
	    				&& (!support().contains("commentSpaceRequired")
	    						|| !stream.eat(" ").isEmpty()))) {
	        stream.skipToEnd();
	        return "comment";
	    }
	    else if (ch.equals("/") && !stream.eat("*").isEmpty()) {
	        state.tokenize = tokenComment();
	        return state.tokenize.process(stream, state);
	    }
	    else if (ch.equals(".")) {
	        if (support().contains("zerolessFloat") && !stream.match(pattern[7]).isEmpty()) {
	            return "number";
	        }
	        if (support().contains("ODBCdotTable") && !stream.match(pattern[8]).isEmpty()) {
	            return "variable-2";
	        }
	    }
	    else if (operatorChars().matcher(ch).matches()) {
	        stream.eatWhile(operatorChars());
	        return "";
	    }
	    else if (ch.equals("{") && (!stream.match(pattern[9]).isEmpty() || !stream.match(pattern[10]).isEmpty())) {
	        return "number";
	    }
	    else {
	        stream.eatWhile(pattern[11]);
	        String word = stream.current().toLowerCase();
	        if (dateSQL().contains(word) 
	        		&& (!stream.match(pattern[12]).isEmpty()
	        				|| !stream.match(pattern[13]).isEmpty())) {
	        	return "number";
	        }
	        if (atoms().contains(word)) return "atom";
	        if (builtin().contains(word)) return "builtin";
	        if (keywords().contains(word)) return "keyword";
	        if (client().contains(word)) return "string-2";
	    }
		return "";
	};
	
	Processor tokenLiteral(String quote) {
		return (stream, state) -> {
		    boolean escaped = false;
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (ch.equals(quote) && !escaped) {
		            state.tokenize = tokenBase;
		            break;
		        }
		        escaped = !escaped && ch.equals("\\");
		    }
		    return "string";
		};
	}
	
	Processor tokenComment() {
		return (stream, state) -> {
		    while (true) {
		        if (stream.skipTo("*")) {
		            stream.next();
		            if (!stream.eat("/").isEmpty()) {
		                state.tokenize = tokenBase;
		                break;
		            }
		        }
		        else {
		            stream.skipToEnd();
		            break;
		        }
		    }
		    return "comment";
		};
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "sql");
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (stream.eatSpace()) return "";
	    String style = state.tokenize.process(stream, state);
	    return style;
	}
}
