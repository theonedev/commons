package io.onedev.commons.jsyntax.django;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class DjangoInner extends AbstractTokenizer<DjangoInner.State> {

	static final Set<String> _keywords = Sets.newHashSet("block", "endblock", "for", "endfor", "true", "false", "filter", "endfilter",
            "loop", "none", "self", "super", "if", "elif", "endif", "as", "else", "import",
            "with", "endwith", "without", "context", "ifequal", "endifequal", "ifnotequal",
            "endifnotequal", "extends", "include", "load", "comment", "endcomment",
            "empty", "url", "static", "trans", "blocktrans", "endblocktrans", "now",
            "regroup", "lorem", "ifchanged", "endifchanged", "firstof", "debug", "cycle",
            "csrf_token", "autoescape", "endautoescape", "spaceless", "endspaceless",
            "ssi", "templatetag", "verbatim", "endverbatim", "widthratio");
	static final Set<String> _filters = Sets.newHashSet("add", "addslashes", "capfirst", "center", "cut", "date",
            "default", "default_if_none", "dictsort",
            "dictsortreversed", "divisibleby", "escape", "escapejs",
            "filesizeformat", "first", "floatformat", "force_escape",
            "get_digit", "iriencode", "join", "last", "length",
            "length_is", "linebreaks", "linebreaksbr", "linenumbers",
            "ljust", "lower", "make_list", "phone2numeric", "pluralize",
            "pprint", "random", "removetags", "rjust", "safe",
            "safeseq", "slice", "slugify", "stringformat", "striptags",
            "time", "timesince", "timeuntil", "title", "truncatechars",
            "truncatechars_html", "truncatewords", "truncatewords_html",
            "unordered_list", "upper", "urlencode", "urlize",
            "urlizetrunc", "wordcount", "wordwrap", "yesno");
	static final Set<String> _operators = Sets.newHashSet("==", "!=", "<", ">", "<=", ">=");
	static final Set<String> _wordOperators = Sets.newHashSet("in", "not", "or", "and");
	
	static final Pattern keywords = Pattern.compile("^\\b(" + StringUtils.join(_keywords, "|") + ")\\b");
	static final Pattern filters = Pattern.compile("^\\b(" + StringUtils.join(_filters, "|") + ")\\b");
	static final Pattern operators = Pattern.compile("^\\b(" + StringUtils.join(_operators, "|") + ")\\b");
	static final Pattern wordOperators = Pattern.compile("^\\b(" + StringUtils.join(_wordOperators, "|") + ")\\b");
	
	static interface Processor {
	    String process(StringStream stream, State state);
	}
	
	static class State {
		Processor tokenize;
		Boolean waitProperty;
		Boolean waitFilter;
		Boolean waitDot;
		Boolean waitPipe;
		Boolean escapeNext;
		Boolean blockCommentTag;
		
		public State() {
			this.tokenize = tokenBase;
			this.waitProperty = null;
			this.waitFilter = null;
			this.waitDot = null;
			this.waitPipe = null;
			this.escapeNext = null;
			this.blockCommentTag = null;
		}
	}
	
	static final Pattern patternForBase = Pattern.compile("\\{[{%#]");
	
	static final Processor tokenBase = (stream, state) -> {
	    if (stream.match("{{")) {
	        state.tokenize = inVariable();
	        return "tag";
	    }
	    else if (stream.match("{%")) {
	        state.tokenize = inTag();
	        return "tag";
	    }
	    else if (stream.match("{#")) {
	        state.tokenize = inComment();
	        return "comment";
	    }
	    while (!stream.next().isEmpty() && stream.match(patternForBase, false).isEmpty());
	    return "";
	};
	
	static Processor inString(String delimiter, Processor previousTokenizer) {
		return (stream, state) -> {
		    if (state.escapeNext != Boolean.TRUE && !stream.eat(delimiter).isEmpty()) {
		        state.tokenize = previousTokenizer;
		    }
		    else {
		        if (state.escapeNext == Boolean.TRUE) {
		            state.escapeNext = false;
		        }
		        String ch = stream.next();
		        if (ch.equals("\\")) {
		            state.escapeNext = true;
		        }
		    }
		    return "string";
		};
	}
	
	static final Pattern patternForVar[] = new Pattern[5];
	static {
		patternForVar[0] = Pattern.compile("\\.\\W+");
		patternForVar[1] = Pattern.compile("\\.\\W+");
		patternForVar[2] = Pattern.compile("\\b(\\w+)\\b");
		patternForVar[3] = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
		patternForVar[4] = Pattern.compile("\\b(\\w+)\\b");
	}
	
	static Processor inVariable() {
		return (stream, state) -> {
		    if (state.waitDot == Boolean.TRUE) {
		        state.waitDot = false;
		        if (!stream.peek().equals(".")) {
		            return "null";
		        }
		        if (!stream.match(patternForVar[0]).isEmpty()) {
		            return "error";
		        }
		        else if (!stream.eat(".").isEmpty()) {
		            state.waitProperty = true;
		            return "null";
		        }
		        else {
		            //throw new Exception("Unexpected error while waiting for property.");
		        }
		    }
		    if (state.waitPipe == Boolean.TRUE) {
		        state.waitPipe = false;
		        if (!stream.peek().equals("|")) {
		            return "null";
		        }
		        if (!stream.match(patternForVar[1]).isEmpty()) {
		            return "error";
		        }
		        else if (!stream.eat("|").isEmpty()) {
		            state.waitFilter = true;
		            return "null";
		        }
		        else {
		            //throw new Exception("Unexpected error while waiting for filter.");
		        }
		    }
		    if (state.waitProperty == Boolean.TRUE) {
		        state.waitProperty = false;
		        if (!stream.match(patternForVar[2]).isEmpty()) {
		            state.waitDot = true;
		            state.waitPipe = true;
		            return "property";
		        }
		    }
		    if (state.waitFilter == Boolean.TRUE) {
		        state.waitFilter = false;
		        if (!stream.match(filters).isEmpty()) {
		            return "variable-2";
		        }
		    }
		    if (stream.eatSpace()) {
		        state.waitProperty = false;
		        return "null";
		    }
		    if (!stream.match(patternForVar[3]).isEmpty()) {
		        return "number";
		    }
		    if (stream.match("'")) {
		        state.tokenize = inString("'", state.tokenize);
		        return "string";
		    }
		    else if (stream.match("\"")) {
		        state.tokenize = inString("\"", state.tokenize);
		        return "string";
		    }
		    if (!stream.match(patternForVar[4]).isEmpty()) {
		        state.waitDot = true;
		        state.waitPipe = true;
		        return "variable";
		    }
		    if (stream.match("}}")) {
		        state.waitProperty = null;
		        state.waitFilter = null;
		        state.waitDot = null;
		        state.waitPipe = null;
		        state.tokenize = tokenBase;
		        return "tag";
		    }
		    stream.next();
		    return "null";
		};
	}
	
	static final Pattern patternForTag[] = new Pattern[5];
	static {
	    patternForTag[0] = Pattern.compile("\\.\\W+");
	    patternForTag[1] = Pattern.compile("\\.\\W+");
	    patternForTag[2] = Pattern.compile("\\b(\\w+)\\b");
	    patternForTag[3] = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
	    patternForTag[4] = Pattern.compile("\\b(\\w+)\\b");
	}
	
	static Processor inTag() {
		return (stream, state) -> {
		    if (state.waitDot == Boolean.TRUE) {
		        state.waitDot = false;
		        if (!stream.peek().equals(".")) {
		            return "null";
		        }
		        if (!stream.match(patternForTag[0]).isEmpty()) {
		            return "error";
		        }
		        else if (!stream.eat(".").isEmpty()) {
		            state.waitProperty = true;
		            return "null";
		        }
		        else {
		            //throw new Exception("Unexpected error while waiting for property.");
		        }
		    }
		    if (state.waitPipe == Boolean.TRUE) {
		        state.waitPipe = false;
		        if (!stream.peek().equals("|")) {
		            return "null";
		        }
		        if (!stream.match(patternForTag[1]).isEmpty()) {
		            return "error";
		        }
		        else if (!stream.eat("|").isEmpty()) {
		            state.waitFilter = true;
		            return "null";
		        }
		        else {
		            //throw new Exception("Unexpected error while waiting for filter.");
		        }
		    }
		    if (state.waitProperty == Boolean.TRUE) {
		        state.waitProperty = false;
		        if (!stream.match(patternForTag[2]).isEmpty()) {
		            state.waitDot = true;
		            state.waitPipe = true;
		            return "property";
		        }
		    }
		    if (state.waitFilter == Boolean.TRUE) {
		        state.waitFilter = false;
		        if (!stream.match(filters).isEmpty()) {
		            return "variable-2";
		        }
		    }
		    if (stream.eatSpace()) {
		        state.waitProperty = false;
		        return "null";
		    }
		    if (!stream.match(patternForTag[3]).isEmpty()) {
		        return "number";
		    }
		    if (stream.match("'")) {
		        state.tokenize = inString("'", state.tokenize);
		        return "string";
		    }
		    else if (stream.match("\"")) {
		        state.tokenize = inString("\"", state.tokenize);
		        return "string";
		    }
		    if (!stream.match(operators).isEmpty()) {
		        return "operator";
		    }
		    if (!stream.match(wordOperators).isEmpty()) {
		        return "keyword";
		    }
		    List<String> keywordMatch = stream.match(keywords);
		    if (!keywordMatch.isEmpty()) {
		        if (keywordMatch.get(0).equals("comment")) {
		            state.blockCommentTag = true;
		        }
		        return "keyword";
		    }
		    if (!stream.match(patternForTag[4]).isEmpty()) {
		        state.waitDot = true;
		        state.waitPipe = true;
		        return "variable";
		    }
		    if (stream.match("%}")) {
		        state.waitProperty = null;
		        state.waitFilter = null;
		        state.waitDot = null;
		        state.waitPipe = null;
		        if (state.blockCommentTag == Boolean.TRUE) {
		            state.blockCommentTag = false;
		            state.tokenize = inBlockComment();
		        }
		        else {
		            state.tokenize = tokenBase;
		        }
		        return "tag";
		    }
		    stream.next();
		    return "null";
		};
	}
	
	static final Pattern patternForComment = Pattern.compile("^.*?#\\}");
	
	static Processor inComment() {
		return (stream, state) -> {
		    if (!stream.match(patternForComment).isEmpty()) state.tokenize = tokenBase;
		    else stream.skipToEnd();
		    return "comment";
		};
	}
	
	static final Pattern patternForBlockComment = Pattern.compile("\\{%\\s*endcomment\\s*%\\}");
	
	static Processor inBlockComment() {
		return (stream, state) -> {
		    if (!stream.match(patternForBlockComment, false).isEmpty()) {
		        state.tokenize = inTag();
		        stream.match("{%");
		        return "tag";
		    }
		    else {
		        stream.next();
		        return "comment";
		    }
		};
	}

	@Override
	public boolean accept(String fileName) {
		return false;
	}

	@Override
	public boolean acceptMime(String mime) {
		return false;
	}

	@Override
	public boolean acceptMode(String mode) {
		return false;
	}

	@Override
	public State startState() {
		return new State();
	}

	@Override
	public String token(StringStream stream, State state) {
		return state.tokenize.process(stream, state);
	}
}
