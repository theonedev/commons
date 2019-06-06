package io.onedev.commons.jsyntax.vbscript;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.TokenizerUtils;

public class VbScriptTokenizer extends AbstractTokenizer<VbScriptTokenizer.State> {
	
	static String ERRORCLASS = "error";
    static Pattern singleOperators = Pattern.compile("^[\\+\\-\\*/&\\\\\\^<>=]");
    static Pattern doubleOperators = Pattern.compile("^((<>)|(<=)|(>=))");
    static Pattern singleDelimiters = Pattern.compile("^[\\.,]");
    static Pattern brakets = Pattern.compile("^[\\(\\)]");
    static Pattern identifiers = Pattern.compile("^[A-Za-z][_A-Za-z0-9]*");
    static Pattern wordOperators = wordRE(Lists.newArrayList("and", "or", "not", "xor", "is", "mod", "eqv", "imp"));
	
    static Pattern keywords = wordRE(Lists.newArrayList("dim", "redim", "then",  "until", "randomize",
            "byval","byref","new","property", "exit", "in",
            "const","private", "public",
            "get","set","let", "stop", "on error resume next", "on error goto 0", "option explicit", "call", "me"));
    static Pattern atoms = wordRE(Lists.newArrayList("true", "false", "nothing", "empty", "null"));
    static Pattern builtinFuncs = wordRE(Lists.newArrayList("abs", "array", "asc", "atn", "cbool", "cbyte", "ccur", "cdate", "cdbl", "chr", "cint", "clng", "cos", "csng", "cstr", "date", "dateadd", "datediff", "datepart",
            "dateserial", "datevalue", "day", "escape", "eval", "execute", "exp", "filter", "formatcurrency", "formatdatetime", "formatnumber", "formatpercent", "getlocale", "getobject",
            "getref", "hex", "hour", "inputbox", "instr", "instrrev", "int", "fix", "isarray", "isdate", "isempty", "isnull", "isnumeric", "isobject", "join", "lbound", "lcase", "left",
            "len", "loadpicture", "log", "ltrim", "rtrim", "trim", "maths", "mid", "minute", "month", "monthname", "msgbox", "now", "oct", "replace", "rgb", "right", "rnd", "round",
            "scriptengine", "scriptenginebuildversion", "scriptenginemajorversion", "scriptengineminorversion", "second", "setlocale", "sgn", "sin", "space", "split", "sqr", "strcomp",
            "string", "strreverse", "tan", "time", "timer", "timeserial", "timevalue", "typename", "ubound", "ucase", "unescape", "vartype", "weekday", "weekdayname", "year"));
    static Pattern builtinObjs = wordRE(Lists.newArrayList("WScript", "err", "debug", "RegExp", "vbBlack", "vbRed", "vbGreen", "vbYellow", "vbBlue", "vbMagenta", "vbCyan", "vbWhite", "vbBinaryCompare", "vbTextCompare",
            "vbSunday", "vbMonday", "vbTuesday", "vbWednesday", "vbThursday", "vbFriday", "vbSaturday", "vbUseSystemDayOfWeek", "vbFirstJan1", "vbFirstFourDays", "vbFirstFullWeek",
            "vbGeneralDate", "vbLongDate", "vbShortDate", "vbLongTime", "vbShortTime", "vbObjectError",
            "vbOKOnly", "vbOKCancel", "vbAbortRetryIgnore", "vbYesNoCancel", "vbYesNo", "vbRetryCancel", "vbCritical", "vbQuestion", "vbExclamation", "vbInformation", "vbDefaultButton1", "vbDefaultButton2",
            "vbDefaultButton3", "vbDefaultButton4", "vbApplicationModal", "vbSystemModal", "vbOK", "vbCancel", "vbAbort", "vbRetry", "vbIgnore", "vbYes", "vbNo",
            "vbCr", "VbCrLf", "vbFormFeed", "vbLf", "vbNewLine", "vbNullChar", "vbNullString", "vbTab", "vbVerticalTab", "vbUseDefault", "vbTrue", "vbFalse",
            "vbEmpty", "vbNull", "vbInteger", "vbLong", "vbSingle", "vbDouble", "vbCurrency", "vbDate", "vbString", "vbObject", "vbError", "vbBoolean", "vbVariant", "vbDataObject", "vbDecimal", "vbByte", "vbArray"));
    static Pattern known = wordRE(Lists.newArrayList("description", "firstindex", "global", "helpcontext", "helpfile", "ignorecase", "length", "number", "pattern", "source", "value", "count", "clear", "execute", "raise", "replace", "test", "write", "writeline", "close", "open", "state", "eof", "update", "addnew", "end", "createobject", "quit"));
    static String stringPrefixes = "\"";

    static Pattern opening = wordRE(Lists.newArrayList("class","sub","select","while","if","function", "property", "with", "for"));
    static Pattern middle = wordRE(Lists.newArrayList("else","elseif","case"));
    static Pattern closing = wordRE(Lists.newArrayList("next","loop","wend"));
    static Pattern doubleClosing = wordRE(Lists.newArrayList("end"));
    static Pattern doOpening = wordRE(Lists.newArrayList("do"));
    static Pattern noIndentWords = wordRE(Lists.newArrayList("on error resume next", "exit"));
    static Pattern comment = wordRE(Lists.newArrayList("rem"));
    static Set<String> knownWords = Sets.newHashSet("description", "firstindex", "global", "helpcontext", "helpfile", "ignorecase", "length", "number", "pattern", "source", "value", "count", "clear", "execute", "raise", "replace", "test", "write", "writeline", "close", "open", "state", "eof", "update", "addnew", "end", "createobject", "quit");
    
    final boolean singleLineStringErrors;
    
    public VbScriptTokenizer() {
    	this(false);
    }
    
    public VbScriptTokenizer(boolean singleLineStringErrors) {
    	this.singleLineStringErrors = singleLineStringErrors;
    }
    
	static Pattern wordRE(List<String> words) {
		return Pattern.compile("^((" + StringUtils.join(words, ")|(") + "))\\b", Pattern.CASE_INSENSITIVE);
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class Token {
		String style;
		String content;
		
		public Token(String style, String content) {
			this.style = style;
			this.content = content;
		}
	}
	
	static class State {
		Processor tokenize;
		Token lastToken;
		int currentIndent;
		int nextLineIndent;
		boolean doInCurrentLine;
		
		public State(Processor tokenize, Token lastToken, int currentIndent, int nextLineIndent, boolean doInCurrentLine) {
			this.tokenize = tokenize;
			this.lastToken = lastToken;
			this.currentIndent = currentIndent;
			this.nextLineIndent = nextLineIndent;
			this.doInCurrentLine = doInCurrentLine;
		}
	}
	
	void indent(State state) {
		state.currentIndent++;
	}

	void dedent(State state) {
		state.currentIndent--;
	}
	
	static Pattern pattern[] = new Pattern[11];
	
	static {
	    pattern[0] = Pattern.compile("^((&H)|(&O))?[0-9\\.]", Pattern.CASE_INSENSITIVE);
	    pattern[1] = Pattern.compile("^((&H)|(&O))?[0-9\\.]+[a-z_]", Pattern.CASE_INSENSITIVE);
	    pattern[2] = Pattern.compile("^\\d*\\.\\d+", Pattern.CASE_INSENSITIVE);
	    pattern[3] = Pattern.compile("^\\d+\\.\\d*");
	    pattern[4] = Pattern.compile("^\\.\\d+");
	    pattern[5] = Pattern.compile("^&H[0-9a-f]+", Pattern.CASE_INSENSITIVE);
	    pattern[6] = Pattern.compile("^&O[0-7]+", Pattern.CASE_INSENSITIVE);
	    pattern[7] = Pattern.compile("^[1-9]\\d*F?");
	    pattern[8] = Pattern.compile("^0(?![\\dx])", Pattern.CASE_INSENSITIVE);
	    pattern[9] = Pattern.compile("J", Pattern.CASE_INSENSITIVE);
	    pattern[10] = Pattern.compile("L", Pattern.CASE_INSENSITIVE);
	}
	
	class TokenBase implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		    if (stream.eatSpace()) {
		        return "space";
		    }
		    String ch = stream.peek();
		    if (ch.equals("'")) {
		        stream.skipToEnd();
		        return "comment";
		    }
		    if (!stream.match(comment).isEmpty()) {
		        stream.skipToEnd();
		        return "comment";
		    }
		    if (!stream.match(pattern[0], false).isEmpty() && !!stream.match(pattern[1], false).isEmpty()) {
		        boolean floatLiteral = false ;
		        if (!stream.match(pattern[2]).isEmpty()) {
		            floatLiteral = true ;
		        }
		        else if (!stream.match(pattern[3]).isEmpty()) {
		            floatLiteral = true ;
		        }
		        else if (!stream.match(pattern[4]).isEmpty()) {
		            floatLiteral = true ;
		        }
		        if (floatLiteral) {
		            stream.eat(pattern[9]);
		            return "number";
		        }
		        boolean intLiteral = false;
		        if (!stream.match(pattern[5]).isEmpty()) {
		            intLiteral = true;
		        }
		        else if (!stream.match(pattern[6]).isEmpty()) {
		            intLiteral = true;
		        }
		        else if (!stream.match(pattern[7]).isEmpty()) {
		            stream.eat(pattern[9]);
		            intLiteral = true;
		        }
		        else if (!stream.match(pattern[8]).isEmpty()) {
		            intLiteral = true;
		        }
		        if (intLiteral) {
		            stream.eat(pattern[10]);
		            return "number";
		        }
		    }
		    if (stream.match(stringPrefixes)) {
		        state.tokenize = new TokenStringFactory(stream.current());
		        return state.tokenize.process(stream, state);
		    }
		    if (!stream.match(doubleOperators).isEmpty() || !stream.match(singleOperators).isEmpty() || !stream.match(wordOperators).isEmpty()) {
		        return "operator";
		    }
		    if (!stream.match(singleDelimiters).isEmpty()) {
		        return "";
		    }
		    if (!stream.match(brakets).isEmpty()) {
		        return "bracket";
		    }
		    if (!stream.match(noIndentWords).isEmpty()) {
		        state.doInCurrentLine = true ;
		        return "keyword";
		    }
		    if (!stream.match(doOpening).isEmpty()) {
		        indent(state);
		        state.doInCurrentLine = true ;
		        return "keyword";
		    }
		    if (!stream.match(opening).isEmpty()) {
		        if (!state.doInCurrentLine) indent(state);
		        else state.doInCurrentLine = false ;
		        return "keyword";
		    }
		    if (!stream.match(middle).isEmpty()) {
		        return "keyword";
		    }
		    if (!stream.match(doubleClosing).isEmpty()) {
		        dedent(state);
		        dedent(state);
		        return "keyword";
		    }
		    if (!stream.match(closing).isEmpty()) {
		        if (!state.doInCurrentLine) dedent(state);
		        else state.doInCurrentLine = false ;
		        return "keyword";
		    }
		    if (!stream.match(keywords).isEmpty()) {
		        return "keyword";
		    }
		    if (!stream.match(atoms).isEmpty()) {
		        return "atom";
		    }
		    if (!stream.match(known).isEmpty()) {
		        return "variable-2";
		    }
		    if (!stream.match(builtinFuncs).isEmpty()) {
		        return "builtin";
		    }
		    if (!stream.match(builtinObjs).isEmpty()) {
		        return "variable-2";
		    }
		    if (!stream.match(identifiers).isEmpty()) {
		        return "variable";
		    }
		    stream.next();
		    return ERRORCLASS;
		}
	}
	
    static Pattern patternString[] = new Pattern[2];
    
    static {
    	patternString[0] = Pattern.compile("[^'\"]");
    	patternString[1] = Pattern.compile("['\"]");
    }
	
	class TokenStringFactory implements Processor {
		String delimiter;
		boolean singleline;
		String OUTCLASS = "string";
		
		public TokenStringFactory(String delimiter) {
			this.delimiter = delimiter;
			this.singleline = delimiter.length() == 1;
		}

		@Override
		public String process(StringStream stream, State state) {
	        while (!stream.eol()) {
	            stream.eatWhile(patternString[0]);
	            if (stream.match(delimiter)) {
	                state.tokenize = new TokenBase();
	                return OUTCLASS;
	            }
	            else {
	                stream.eat(patternString[1]);                ;
	            }
	        }
	        if (singleline) {
	            if (singleLineStringErrors) {
	                return ERRORCLASS;
	            }
	            else {
	                state.tokenize = new TokenBase();
	            }
	        }
	        return OUTCLASS;
		}
	}

	String tokenLexer(StringStream stream, State state) {
	    String style = state.tokenize.process(stream, state);
	    String current = stream.current();
	    if (current.equals(".")) {
	        style = state.tokenize.process(stream, state);
	        current = stream.current();
	        if (!style.isEmpty() && (TokenizerUtils.substr(style, 0, 8).equals("variable") || style.equals("builtin") || style.equals("keyword"))) {
	            if (style.equals("builtin") || style.equals("keyword")) style = "variable";
	            if (knownWords.contains(TokenizerUtils.substr(current, 1))) style = "variable-2";
	            return style;
	        }
	        else {
	            return ERRORCLASS;
	        }
	    }
	    return style;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "vbs");
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
	    if (style.equals("space")) style = "";
	    return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/vbscript");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("vbscript");
	}
}
