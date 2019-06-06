package io.onedev.commons.jsyntax.pug;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.Tokenizer;
import io.onedev.commons.jsyntax.TokenizerRegistry;
import io.onedev.commons.jsyntax.javascript.JavaScriptTokenizer;

public class PugTokenizer extends AbstractTokenizer<PugTokenizer.State> {

	static final int Infinity = 1000000007;
	
	static final String KEYWORD = "keyword";
	static final String DOCTYPE = "meta";
	static final String ID = "builtin";
	static final String CLASS = "qualifier";
	
	static final Map<String, String> ATTRS_NEST = Maps.newHashMap();
	static {
		ATTRS_NEST.put("{", "}");
		ATTRS_NEST.put("(", ")");
		ATTRS_NEST.put("[", "]");
	}
	
	static class State {
		boolean javaScriptLine = false;
		boolean javaScriptLineExcludesColon = false;
		
	    boolean javaScriptArguments = false;
	    int javaScriptArgumentsDepth = 0;

	    boolean isInterpolating = false;
	    int interpolationNesting = 0;
	    
	    JavaScriptTokenizer jsMode = new JavaScriptTokenizer();
	    JavaScriptTokenizer.State jsState = jsMode.startState();

	    String restOfLine = "";

	    boolean isIncludeFiltered = false;
	    boolean isEach = false;

	    String lastTag = "";
	    String scriptType = "";

	    boolean isAttrs = false;
	    Stack<String> attrsNest = new Stack<String>();
	    boolean inAttributeName = true;
	    boolean attributeIsType = false;
	    String attrValue = "";

	    int indentOf = Infinity;
	    String indentToken = "";

	    @SuppressWarnings("rawtypes")
		AbstractTokenizer innerMode = null;
	    Object innerState = null;

	    boolean innerModeForLine = false;
	    
	    boolean mixinCallAfter = false;
	}
	
	String javaScript(StringStream stream, State state) {
	    if (stream.sol()) {
	        state.javaScriptLine = false;
	        state.javaScriptLineExcludesColon = false;
	    }
	    if (state.javaScriptLine) {
	        if (state.javaScriptLineExcludesColon && stream.peek().equals(":")) {
	            state.javaScriptLine = false;
	            state.javaScriptLineExcludesColon = false;
	            return "";
	        }
	        String tok = state.jsMode.token(stream, state.jsState);
	        if (stream.eol()) state.javaScriptLine = false;
	        return tok.isEmpty() ? "true" : tok;
	    }
	    return "";
	}

	String javaScriptArguments(StringStream stream, State state) {
	    if (state.javaScriptArguments) {
	        if (state.javaScriptArgumentsDepth == 0 && !stream.peek().equals("(")) {
	            state.javaScriptArguments = false;
	            return "";
	        }
	        if (stream.peek().equals("(")) {
	            state.javaScriptArgumentsDepth++;
	        }
	        else if (stream.peek().equals(")")) {
	            state.javaScriptArgumentsDepth--;
	        }
	        if (state.javaScriptArgumentsDepth == 0) {
	            state.javaScriptArguments = false;
	            return "";
	        }
	        String tok = state.jsMode.token(stream, state.jsState);
	        return tok.isEmpty() ? "true" : tok;
	    }
	    return "";
	}
	
	static final Pattern patternForYieldStatement = Pattern.compile("^yield\\b");
	
	String yieldStatement(StringStream stream) {
	    if (!stream.match(patternForYieldStatement).isEmpty()) {
	        return "keyword";
	    }
	    return "";
	}
	
	static final Pattern patternForDoctype = Pattern.compile("^(?:doctype) *([^\\n]+)?");

	String doctype(StringStream stream) {
	    if (!stream.match(patternForDoctype).isEmpty()) {
	        return DOCTYPE;
	    }
	    return "";
	}
	
	String interpolation(StringStream stream, State state) {
	    if (stream.match("#{")) {
	        state.isInterpolating = true;
	        state.interpolationNesting = 0;
	        return "punctuation";
	    }
	    return "";
	}

	String interpolationContinued(StringStream stream, State state) {
	    if (state.isInterpolating) {
	        if (stream.peek().equals("}")) {
	            state.interpolationNesting--;
	            if (state.interpolationNesting < 0) {
	                stream.next();
	                state.isInterpolating = false;
	                return "punctuation";
	            }
	        }
	        else if (stream.peek().equals("{")) {
	            state.interpolationNesting++;
	        }
	        String tok = state.jsMode.token(stream, state.jsState);
	        return tok.isEmpty() ? "true" : tok;
	    }
	    return "";
	}
	
	static final Pattern patternForCaseStatement = Pattern.compile("^case\\b");

	String caseStatement(StringStream stream, State state) {
	    if (!stream.match(patternForCaseStatement).isEmpty()) {
	        state.javaScriptLine = true;
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForWhen = Pattern.compile("^when\\b");

	String when(StringStream stream, State state) {
	    if (!stream.match(patternForWhen).isEmpty()) {
	        state.javaScriptLine = true;
	        state.javaScriptLineExcludesColon = true;
	        return KEYWORD;
	    }
	    return "";
	}
	
	static final Pattern patternForDefaultStatement = Pattern.compile("^default\\b");
	
	String defaultStatement(StringStream stream) {
	    if (!stream.match(patternForDefaultStatement).isEmpty()) {
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForExtendsStatement = Pattern.compile("^extends?\\b");

	String extendsStatement(StringStream stream, State state) {
	    if (!stream.match(patternForExtendsStatement).isEmpty()) {
	        state.restOfLine = "string";
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForAppend = Pattern.compile("^append\\b");

	String append(StringStream stream, State state) {
	    if (!stream.match(patternForAppend).isEmpty()) {
	        state.restOfLine = "variable";
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForPrepend = Pattern.compile("^prepend\\b");

	String prepend(StringStream stream, State state) {
	    if (!stream.match(patternForPrepend).isEmpty()) {
	        state.restOfLine = "variable";
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForBlock = Pattern.compile("^block\\b *(?:(prepend|append)\\b)?");

	String block(StringStream stream, State state) {
	    if (!stream.match(patternForBlock).isEmpty()) {
	        state.restOfLine = "variable";
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForInclude = Pattern.compile("^include\\b");

	String include(StringStream stream, State state) {
	    if (!stream.match(patternForInclude).isEmpty()) {
	        state.restOfLine = "string";
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForIncludeFiltered = Pattern.compile("^include:([a-zA-Z0-9\\-]+)");

	String includeFiltered(StringStream stream, State state) {
	    if (!stream.match(patternForIncludeFiltered, false).isEmpty() && stream.match("include")) {
	        state.isIncludeFiltered = true;
	        return KEYWORD;
	    }
	    return "";
	}

	String includeFilteredContinued(StringStream stream, State state) {
	    if (state.isIncludeFiltered) {
	        String tok = filter(stream, state);
	        state.isIncludeFiltered = false;
	        state.restOfLine = "string";
	        return tok;
	    }
	    return "";
	}

	static final Pattern patternForMixin = Pattern.compile("^mixin\\b");

	String mixin(StringStream stream, State state) {
	    if (!stream.match(patternForMixin).isEmpty()) {
	        state.javaScriptLine = true;
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForCall[] = new Pattern[3];
	static {
		patternForCall[0] = Pattern.compile("^\\+([-\\w]+)");
		patternForCall[1] = Pattern.compile("^\\( *[-\\w]+ *=");
		patternForCall[2] = Pattern.compile("^\\+#\\{");
	}
	
	String call(StringStream stream, State state) {
	    if (!stream.match(patternForCall[0]).isEmpty()) {
	        if (!!stream.match(patternForCall[1], false).isEmpty()) {
	            state.javaScriptArguments = true;
	            state.javaScriptArgumentsDepth = 0;
	        }
	        return "variable";
	    }
	    if (!stream.match(patternForCall[2],false).isEmpty()) {
	        stream.next();
	        state.mixinCallAfter = true;
	        return interpolation(stream, state);
	    }
	    return "";
	}

	static final Pattern patternForCallArguments = Pattern.compile("^\\( *[-\\w]+ *=");

	String callArguments(StringStream stream, State state) {
	    if (state.mixinCallAfter) {
	        state.mixinCallAfter = false;
	        if (!!stream.match(patternForCallArguments, false).isEmpty()) {
	            state.javaScriptArguments = true;
	            state.javaScriptArgumentsDepth = 0;
	        }
	        return "true";
	    }
	    return "";
	}

	static final Pattern patternForConditional = Pattern.compile("^(if|unless|else if|else)\\b");

	String conditional(StringStream stream, State state) {
	    if (!stream.match(patternForConditional).isEmpty()) {
	        state.javaScriptLine = true;
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForEach = Pattern.compile("^(- *)?(each|for)\\b");

	String each(StringStream stream, State state) {
	    if (!stream.match(patternForEach).isEmpty()) {
	        state.isEach = true;
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForEachContinued[] = new Pattern[2];
	static {
		patternForEachContinued[0] = Pattern.compile("^ in\\b");
		patternForEachContinued[1] = Pattern.compile("^ in\\b");
	}
	
	String eachContinued(StringStream stream, State state) {
	    if (state.isEach) {
	        if (!stream.match(patternForEachContinued[0]).isEmpty()) {
	            state.javaScriptLine = true;
	            state.isEach = false;
	            return KEYWORD;
	        }
	        else if (stream.sol() || stream.eol()) {
	            state.isEach = false;
	        }
	        else if (!stream.next().isEmpty()) {
	            while (stream.match(patternForEachContinued[1], false).isEmpty() && !stream.next().isEmpty());
	            return "variable";
	        }
	    }
	    return "";
	}

	static final Pattern patternForWhileStatement = Pattern.compile("^while\\b");

	String whileStatement(StringStream stream, State state) {
	    if (!stream.match(patternForWhileStatement).isEmpty()) {
	        state.javaScriptLine = true;
	        return KEYWORD;
	    }
	    return "";
	}

	static final Pattern patternForTag = Pattern.compile("^(\\w(?:[-:\\w]*\\w)?)\\/?");

	String tag(StringStream stream, State state) {
	    List<String> captures;
	    if (!(captures = stream.match(patternForTag)).isEmpty()) {
	        state.lastTag = captures.get(1).toLowerCase();
	        if (state.lastTag.equals("script")) {
	            state.scriptType = "application/javascript";
	        }
	        return "tag";
	    }
	    return "";
	}
	
	static final Pattern patternForFilter = Pattern.compile("^:([\\w\\-]+)");
	
	String filter(StringStream stream, State state) {
	    if (!stream.match(patternForFilter).isEmpty()) {
	        String innerMode = stream.current().substring(1);
	        setInnerMode(stream, state, innerMode);
	        return "atom";
	    }
	    return "";
	}

	static final Pattern patternForCode = Pattern.compile("^(!?=|-)");

	String code(StringStream stream, State state) {
	    if (!stream.match(patternForCode).isEmpty()) {
	        state.javaScriptLine = true;
	        return "punctuation";
	    }
	    return "";
	}

	static final Pattern patternForId = Pattern.compile("^#([\\w-]+)");

	String id(StringStream stream) {
	    if (!stream.match(patternForId).isEmpty()) {
	        return ID;
	    }
	    return "";
	}

	static final Pattern patternForClassName = Pattern.compile("^\\.([\\w-]+)");

	String className(StringStream stream) {
	    if (!stream.match(patternForClassName).isEmpty()) {
	        return CLASS;
	    }
	    return "";
	}

	String attrs(StringStream stream, State state) {
	    if (stream.peek().equals("(")) {
	        stream.next();
	        state.isAttrs = true;
	        state.attrsNest.clear();;
	        state.inAttributeName = true;
	        state.attrValue = "";
	        state.attributeIsType = false;
	        return "punctuation";
	    }
	    return "";
	}
	
	static final Pattern patternForAttrsContinued[] = new Pattern[3];
	static {
		patternForAttrsContinued[0] = Pattern.compile("^[^=,\\)!]+");
		patternForAttrsContinued[1] = Pattern.compile(",\\s*$");
		patternForAttrsContinued[2] = Pattern.compile("^!");
	}
	
	String attrsContinued(StringStream stream, State state) {
	    if (state.isAttrs) {
	        if (ATTRS_NEST.containsKey(stream.peek())) {
	            state.attrsNest.push(ATTRS_NEST.get(stream.peek()));
	        }
	        if (!state.attrsNest.isEmpty() && state.attrsNest.peek().equals(stream.peek())) {
	            state.attrsNest.pop();
	        }
	        else if (!stream.eat(")").isEmpty()) {
	            state.isAttrs = false;
	            return "punctuation";
	        }
	        if (state.inAttributeName && !stream.match(patternForAttrsContinued[0]).isEmpty()) {
	            if (stream.peek().equals("=") || stream.peek().equals("!")) {
	                state.inAttributeName = false;
	                state.jsState = state.jsMode.startState();
	                if (state.lastTag.equals("script") && stream.current().trim().toLowerCase().equals("type")) {
	                    state.attributeIsType = true;
	                }
	                else {
	                    state.attributeIsType = false;
	                }
	            }
	            return "attribute";
	        }
	        String tok = state.jsMode.token(stream, state.jsState);
	        if (state.attributeIsType && tok.equals("string")) {
	            state.scriptType = stream.current().toString();
	        }
	        if (state.attrsNest.isEmpty() && (tok.equals("string") || tok.equals("variable") || tok.equals("keyword"))) {
	        	Matcher matcher= patternForAttrsContinued[1].matcher(state.attrValue);
	        	matcher = patternForAttrsContinued[2].matcher(matcher.replaceAll(""));
	        	String s = matcher.replaceAll("");
	        	if (s.isEmpty()) {
	                state.inAttributeName = true;
	                state.attrValue = "";
	                stream.backUp(stream.current().length());
	                return attrsContinued(stream, state);
	            }
	        }
	        state.attrValue += stream.current();
	        return tok.isEmpty() ? "true" : tok;
	    }
	    return "";
	}

	static final Pattern patternForAttributesBlock = Pattern.compile("^&attributes\\b");

	String attributesBlock(StringStream stream, State state) {
	    if (!stream.match(patternForAttributesBlock).isEmpty()) {
	        state.javaScriptArguments = true;
	        state.javaScriptArgumentsDepth = 0;
	        return "keyword";
	    }
	    return "";
	}

	String indent(StringStream stream) {
	    if (stream.sol() && stream.eatSpace()) {
	        return "indent";
	    }
	    return "";
	}

	static final Pattern patternForComment = Pattern.compile("^ *\\/\\/(-)?([^\\n]*)");

	String comment(StringStream stream, State state) {
	    if (!stream.match(patternForComment).isEmpty()) {
	        state.indentOf = stream.indentation();
	        state.indentToken = "comment";
	        return "comment";
	    }
	    return "";
	}

	static final Pattern patternForColon = Pattern.compile("^: *");

	String colon(StringStream stream) {
	    if (!stream.match(patternForColon).isEmpty()) {
	        return "colon";
	    }
	    return "";
	}

	static final Pattern patternForText[] = new Pattern[2];
	static {
		patternForText[0] = Pattern.compile("^(?:\\| ?| )([^\\n]+)");
		patternForText[1] = Pattern.compile("^(<[^\\n]*)");
	}
	
	String text(StringStream stream, State state) {
	    if (!stream.match(patternForText[0]).isEmpty()) {
	        return "string";
	    }
	    if (!stream.match(patternForText[1], false).isEmpty()) {
	        setInnerMode(stream, state, "htmlmixed");
	        state.innerModeForLine = true;
	        return innerMode(stream, state, true);
	    }
	    return "";
	}

	static final Pattern patternForDot = Pattern.compile("\"|'");

	String dot(StringStream stream, State state) {
	    if (!stream.eat(".").isEmpty()) {
	        String innerMode = null;
	        if (state.lastTag.equals("script") && state.scriptType.toLowerCase().indexOf("javascript") != -1) {
	        	Matcher matcher = patternForDot.matcher(state.scriptType.toLowerCase());
	            innerMode = matcher.replaceAll("");
	        }
	        else if (state.lastTag.equals("style")) {
	            innerMode = "css";
	        }
	        setInnerMode(stream, state, innerMode);
	        return "dot";
	    }
	    return "";
	}

	String fail(StringStream stream) {
	    stream.next();
	    return "";
	}

	@SuppressWarnings("rawtypes")
	void setInnerMode(StringStream stream, State state, String mode) {
	    Tokenizer innerMode = TokenizerRegistry.getTokenizerByMime(mode);
	    if (innerMode == null) innerMode = TokenizerRegistry.getTokenizerByMode(mode);
	    state.indentOf = stream.indentation();
	    if (innerMode != null) {
	        state.innerMode = (AbstractTokenizer) innerMode;
	    } else {
	        state.indentToken = "string";
	    }
	}

	@SuppressWarnings("unchecked")
	String innerMode(StringStream stream, State state, boolean force) {
	    if (stream.indentation() > state.indentOf || (state.innerModeForLine && !stream.sol()) || force) {
	        if (state.innerMode != null) {
	            if (state.innerState == null) {
	                state.innerState = state.innerMode.startState();
	            }
	            return stream.hideFirstChars(state.indentOf + 2, () -> {
					String tok = state.innerMode.token(stream, state.innerState);
	                return tok.isEmpty() ? "true" : tok;
	            }).toString();
	        }
	        else {
	            stream.skipToEnd();
	            return state.indentToken;
	        }
	    }
	    else if (stream.sol()) {
	        state.indentOf = Infinity;
	        state.indentToken = "";
	        state.innerMode = null;
	        state.innerState = null;
	    }
	    return "";
	}

	String restOfLine(StringStream stream, State state) {
	    if (stream.sol()) {
	        state.restOfLine = "";
	    }
	    if (!state.restOfLine.isEmpty()) {
	        stream.skipToEnd();
	        String tok = state.restOfLine;
	        state.restOfLine = "";
	        return tok;
	    }
	    return "";
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "jade", "pug");
	}

	@Override
	public State startState() {
		return new State();
	}

	@Override
	public String token(StringStream stream, State state) {
		String tok = "";
		if (tok.isEmpty()) tok = innerMode(stream, state, false);
		if (tok.isEmpty()) tok = restOfLine(stream, state);
		if (tok.isEmpty()) tok = interpolationContinued(stream, state);
		if (tok.isEmpty()) tok = includeFilteredContinued(stream, state);
		if (tok.isEmpty()) tok = eachContinued(stream, state);
		if (tok.isEmpty()) tok = attrsContinued(stream, state);
		if (tok.isEmpty()) tok = javaScript(stream, state);
		if (tok.isEmpty()) tok = javaScriptArguments(stream, state);
		if (tok.isEmpty()) tok = callArguments(stream, state);
		if (tok.isEmpty()) tok = yieldStatement(stream);
		if (tok.isEmpty()) tok = doctype(stream);
		if (tok.isEmpty()) tok = interpolation(stream, state);
		if (tok.isEmpty()) tok = caseStatement(stream, state);
		if (tok.isEmpty()) tok = when(stream, state);
		if (tok.isEmpty()) tok = defaultStatement(stream);
		if (tok.isEmpty()) tok = extendsStatement(stream, state);
		if (tok.isEmpty()) tok = append(stream, state);
		if (tok.isEmpty()) tok = prepend(stream, state);
		if (tok.isEmpty()) tok = block(stream, state);
		if (tok.isEmpty()) tok = include(stream, state);
		if (tok.isEmpty()) tok = includeFiltered(stream, state);
		if (tok.isEmpty()) tok = mixin(stream, state);
		if (tok.isEmpty()) tok = call(stream, state);
		if (tok.isEmpty()) tok = conditional(stream, state);
		if (tok.isEmpty()) tok = each(stream, state);
		if (tok.isEmpty()) tok = whileStatement(stream, state);
		if (tok.isEmpty()) tok = tag(stream, state);
		if (tok.isEmpty()) tok = filter(stream, state);
		if (tok.isEmpty()) tok = code(stream, state);
		if (tok.isEmpty()) tok = id(stream);
		if (tok.isEmpty()) tok = className(stream);
		if (tok.isEmpty()) tok = attrs(stream, state);
		if (tok.isEmpty()) tok = attributesBlock(stream, state);
		if (tok.isEmpty()) tok = indent(stream);
		if (tok.isEmpty()) tok = text(stream, state);
		if (tok.isEmpty()) tok = comment(stream, state);
		if (tok.isEmpty()) tok = colon(stream);
		if (tok.isEmpty()) tok = dot(stream, state);
		if (tok.isEmpty()) tok = fail(stream);
		return tok.equals("true") ? "" : tok;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null
				&& (mime.equals("text/x-pug")
				|| mime.equals("text/x-jade"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("pug");
	}
}
