package io.onedev.commons.jsyntax.markdown;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.Tokenizer;
import io.onedev.commons.jsyntax.TokenizerRegistry;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;
import io.onedev.commons.jsyntax.xml.XmlTokenizer;

public class MarkdownTokenizer extends AbstractTokenizer<MarkdownTokenizer.State> {

	static final HtmlMixedTokenizer htmlMode = new HtmlMixedTokenizer();

	final boolean highlightFormatting;
	final int maxBlockquoteDepth;
	final boolean underscoresBreakWords;
	final boolean taskLists;
	final boolean strikethrough;
	final Map<String, String> tokenTypeOverrides;
	final String fencedCodeBlocks;
	final Map<String, String> tokenTypes = Maps.newHashMap();

	static final Pattern hrRE = Pattern.compile("^([*\\-_])(?:\\s*\\1){2,}\\s*$");
	static final Pattern listRE = Pattern.compile("^(?:[*\\-+]|^[0-9]+([.)]))\\s+");
	static final Pattern taskListRE = Pattern.compile("^\\[(x| )\\](?=\\s)");
	static final Pattern atxHeaderRE = Pattern.compile("^(#+)(?: |$)");
	static final Pattern setextHeaderRE = Pattern.compile("^ *(?:\\={1,}|-{1,})\\s*$");
	static final Pattern textRE = Pattern.compile("^[^#!\\[\\]*_\\\\<>` \"'(~]+");
	final Pattern fencedCodeRE;

	public MarkdownTokenizer(boolean highlightFormatting, int maxBlockquoteDepth, boolean underscoresBreakWords,
			boolean taskLists, boolean strikethrough, Map<String, String> tokenTypeOverrides, String fencedCodeBlocks) {
		this.highlightFormatting = highlightFormatting;
		this.maxBlockquoteDepth = maxBlockquoteDepth;
		this.underscoresBreakWords = underscoresBreakWords;
		this.taskLists = taskLists;
		this.strikethrough = strikethrough;
		this.tokenTypeOverrides = tokenTypeOverrides;
		this.fencedCodeBlocks = fencedCodeBlocks;

		tokenTypes.put("header", "header");
		tokenTypes.put("code", "comment");
		tokenTypes.put("quote", "quote");
		tokenTypes.put("list1", "variable-2");
		tokenTypes.put("list2", "variable-3");
		tokenTypes.put("list3", "keyword");
		tokenTypes.put("hr", "hr");
		tokenTypes.put("image", "image");
		tokenTypes.put("imageAltText", "image-alt-text");
		tokenTypes.put("imageMarker", "image-marker");
		tokenTypes.put("formatting", "formatting");
		tokenTypes.put("linkInline", "link");
		tokenTypes.put("linkEmail", "link");
		tokenTypes.put("linkText", "link");
		tokenTypes.put("linkHref", "string");
		tokenTypes.put("em", "em");
		tokenTypes.put("strong", "strong");
		tokenTypes.put("strikethrough", "strikethrough");

		for (String key : tokenTypes.keySet()) {
			if (tokenTypeOverrides.containsKey(key)) {
				tokenTypes.replace(key, tokenTypeOverrides.get(key));
			}
		}

		fencedCodeRE = Pattern.compile("^(" + (fencedCodeBlocks.equals("true") ? "~~~+|```+" : fencedCodeBlocks) + ")[ \\t]*([\\w+#\\-]*)");
	}

	public MarkdownTokenizer() {
		this(false, 0, true, false, false, Maps.newHashMap(), "");
	}

	@FunctionalInterface
	interface Processor {
		String process(StringStream stream, State state);
	}

	class State {
		Processor f;

		String prevLine;
		String thisLine;

		Processor block;
		HtmlMixedTokenizer.State htmlState;
		int indentation;
		int indentationDiff;

		@SuppressWarnings("rawtypes")
		AbstractTokenizer localMode;
		Object localState;

		Processor inline;
		Processor text;

		List<String> formatting;
		boolean linkText;
		boolean linkHref;
		boolean linkTitle;
		int code;
		String em;
		String strong;
		int header;
		boolean hr;
		boolean taskList;
		Boolean list;
		Stack<Integer> listStack;
		int quote;
		boolean indentedCode;
		int trailingSpace;
		boolean trailingSpaceNewLine;
		boolean strikethrough;
		boolean md_inside;
		String fencedChars;
		
		boolean taskOpen;
		boolean taskClosed;
		boolean image;
		boolean imageAltText;
		boolean imageMarker;

		public State() {
			this.f = blockNormal();
			
			this.prevLine = "";
			this.thisLine = "";
			
			this.block = blockNormal();
			this.htmlState = null;
			this.indentation = 0;
			this.indentationDiff = 0;
			
			this.localMode = null;
			this.localState = null;
			
			this.inline = inlineNormal();
			this.text = handleText();
			
			this.formatting = new Stack<String>();
			this.linkText = false;
			this.linkHref = false;
			this.linkTitle = false;
			this.code = 0;
			this.em = "false";
			this.strong = "false";
			this.header = 0;
			this.hr = false;
			this.taskList = false;
			this.list = Boolean.FALSE;
			this.listStack = new Stack<Integer>();
			this.quote = 0;
			this.indentedCode = false;
			this.trailingSpace = 0;
			this.trailingSpaceNewLine = false;
			this.strikethrough = false;
			this.md_inside = false;
			this.fencedChars = "";
			
			this.taskOpen = false;
			this.taskClosed = false;
			this.image = false;
			this.imageAltText = false;
			this.imageMarker = false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	static AbstractTokenizer getMode(String mode) {
		Tokenizer f = TokenizerRegistry.getTokenizerByMime(mode);
		if (f == null)
			f = TokenizerRegistry.getTokenizerByMode(mode);
		return (AbstractTokenizer)f;
	}

	static String switchInline(StringStream stream, State state, Processor f) {
		state.f = state.inline = f;
		return f.process(stream, state);
	}

	static String switchBlock(StringStream stream, State state, Processor f) {
		state.f = state.block = f;
		return f.process(stream, state);
	}

	static final Pattern empty = Pattern.compile("\\S");

	static boolean lineIsEmpty(String line) {
		return line.isEmpty() || !empty.matcher(line).find();
	}

	protected void blankLine(State state) {
		state.linkTitle = false;
		state.em = "false";
		state.strong = "false";
		state.strikethrough = false;
		state.quote = 0;
		state.indentedCode = false;
		state.trailingSpace = 0;
		state.trailingSpaceNewLine = false;
		state.prevLine = state.thisLine;
		state.thisLine = "";
	}
	
	Processor blockNormal() {
		return (stream, state) -> {
		    boolean sol = stream.sol();
		    boolean prevLineIsList = state.list != Boolean.FALSE, prevLineIsIndentedCode = state.indentedCode;
		    state.indentedCode = false;
		    if (prevLineIsList) {
		        if (state.indentationDiff >= 0) {
		            if (state.indentationDiff < 4) {
		                state.indentation -= state.indentationDiff;
		            }
		            state.list = null;
		        }
		        else if (state.indentation > 0) {
		            state.list = null;
		        }
		        else {
		            state.list = Boolean.FALSE;
		        }
		    }
		    List<String> match;
		    if (state.indentationDiff >= 4) {
		        stream.skipToEnd();
		        if (prevLineIsIndentedCode || lineIsEmpty(state.prevLine)) {
		            state.indentation -= 4;
		            state.indentedCode = true;
		            return tokenTypes.get("code");
		        }
		        else {
		            return "";
		        }
		    }
		    else if (stream.eatSpace()) {
		        return "";
		    }
		    else if (!(match = stream.match(atxHeaderRE)).isEmpty() && match.get(1).length() <= 6) {
		        state.header = match.get(1).length();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("header");
		        }
		        state.f = state.inline;
		        return getType(state);
		    }
		    else if (!lineIsEmpty(state.prevLine) 
		    		&& state.quote == 0
		    		&& !prevLineIsList
		    		&& !prevLineIsIndentedCode
		    		&& !(match = stream.match(setextHeaderRE)).isEmpty()) {
		        state.header = match.get(0).charAt(0) == '=' ? 1 : 2;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("header");
		        }
		        state.f = state.inline;
		        return getType(state);
		    }
		    else if (!stream.eat(">").isEmpty()) {
		        state.quote = sol ? 1 : state.quote + 1;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("quote");
		        }
		        stream.eatSpace();
		        return getType(state);
		    }
		    else if (stream.peek().equals("[")) {
		        return switchInline(stream, state, footnoteLink());
		    }
		    else if (!stream.match(hrRE,true).isEmpty()) {
		        state.hr = true;
		        return tokenTypes.get("hr");
		    }
		    else if (!(match = stream.match(listRE)).isEmpty()) {
		        String listType = match.get(1) != null && !match.get(1).isEmpty() ? "ol" : "ul";
		        state.indentation = stream.column() + stream.current().length();
		        state.list = Boolean.TRUE;
		        while (!state.listStack.isEmpty() && stream.column() < state.listStack.peek()) {
		            state.listStack.pop();
		        }
		        state.listStack.push(state.indentation);
		        if (taskLists && !stream.match(taskListRE, false).isEmpty()) {
		            state.taskList = true;
		        }
		        state.f = state.inline;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("list");
		        	state.formatting.add("list-" + listType);
		        }
		        return getType(state);
		    }
		    else if (!fencedCodeBlocks.isEmpty() && !(match = stream.match(fencedCodeRE, true)).isEmpty()) {
		        state.fencedChars = match.get(1);
		        state.localMode = getMode(match.get(2));
		        if (state.localMode != null)
		        	state.localState = state.localMode.startState();
		        state.f = state.block = local();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("code-block");
		        }
		        state.code = -1;
		        return getType(state);
		    }
		    return switchInline(stream, state, state.inline);
		};
	}
	
	Processor htmlBlock() {
		return (stream, state) -> {
		    String style = htmlMode.token(stream, state.htmlState);
		    XmlTokenizer.State innerState = state.htmlState.getXmlState();
		    if ((innerState != null && innerState.getTagStart() == 0 && (innerState.isContextNull() && innerState.isInText()))
		    		|| (state.md_inside && stream.current().indexOf(">") > -1)) {
		        state.f = inlineNormal();
		        state.block = blockNormal();
		        state.htmlState = null;
		    }
		    return style;
		};
	}
	
	@SuppressWarnings("unchecked")
	Processor local() {
		return (stream, state) -> {
		    if (!state.fencedChars.isEmpty() && stream.match(state.fencedChars, false, false)) {
		        state.localMode = null;
		        state.localState = null;
		        state.f = state.block = leavingLocal();
		        return "";
		    }
		    else if (state.localMode != null) {
		        return state.localMode.token(stream, state.localState);
		    }
		    else {
		        stream.skipToEnd();
		        return tokenTypes.get("code");
		    }
		};
	}
	
	Processor leavingLocal() {
		return (stream, state) -> {
		    stream.match(state.fencedChars);
		    state.block = blockNormal();
		    state.f = inlineNormal();
		    state.fencedChars = "";
		    if (highlightFormatting) {
		    	state.formatting.clear();
		    	state.formatting.add("code-block");
		    }
		    state.code = 1;
		    String returnType = getType(state);
		    state.code = 0;
		    return returnType;
		};
	}
	
	String getType(State state) {
	    List<String> styles = Lists.newArrayList();
	    if (!state.formatting.isEmpty()) {
	        styles.add(tokenTypes.get("formatting"));
	        for (int i = 0; i < state.formatting.size(); i++) {
	            styles.add(tokenTypes.get("formatting") + "-" + state.formatting.get(i));
	            if (state.formatting.get(i).equals("header")) {
	                styles.add(tokenTypes.get("formatting") + "-" + state.formatting.get(i) + "-" + state.header);
	            }
	            if (state.formatting.get(i).equals("quote")) {
	                if (maxBlockquoteDepth == 0 || maxBlockquoteDepth >= state.quote) {
	                    styles.add(tokenTypes.get("formatting") + "-" + state.formatting.get(i) + "-" + state.quote);
	                }
	                else {
	                    styles.add("error");
	                }
	            }
	        }
	    }
	    if (state.taskOpen) {
	        styles.add("meta");
	        return !styles.isEmpty() ? StringUtils.join(styles, " ") : "";
	    }
	    if (state.taskClosed) {
	        styles.add("property");
	        return !styles.isEmpty() ? StringUtils.join(styles, " ") : "";
	    }
	    if (state.linkHref) {
	        styles.add(tokenTypes.get("linkHref"));
	        styles.add("url");
	    }
	    else {
	        if (!state.strong.equals("false")) {
	            styles.add(tokenTypes.get("strong"));
	        }
	        if (!state.em.equals("false")) {
	            styles.add(tokenTypes.get("em"));
	        }
	        if (state.strikethrough) {
	            styles.add(tokenTypes.get("strikethrough"));
	        }
	        if (state.linkText) {
	            styles.add(tokenTypes.get("linkText"));
	        }
	        if (state.code != 0) {
	            styles.add(tokenTypes.get("code"));
	        }
	        if (state.image) {
	            styles.add(tokenTypes.get("image"));
	        }
	        if (state.imageAltText) {
	            styles.add(tokenTypes.get("imageAltText"));
	            styles.add("link");
	        }
	        if (state.imageMarker) {
	            styles.add(tokenTypes.get("imageMarker"));
	        }
	    }
	    if (state.header != 0) {
	        styles.add(tokenTypes.get("header"));
	        styles.add(tokenTypes.get("header") + "-" + state.header);
	    }
	    if (state.quote != 0) {
	        styles.add(tokenTypes.get("quote"));
	        if (maxBlockquoteDepth == 0 || maxBlockquoteDepth >= state.quote) {
	            styles.add(tokenTypes.get("quote") + "-" + state.quote);
	        }
	        else {
	            styles.add(tokenTypes.get("quote") + "-" + maxBlockquoteDepth);
	        }
	    }
	    if (state.list != Boolean.FALSE) {
	        int listMod = (state.listStack.size() - 1) % 3;
	        if (listMod == 0) {
	            styles.add(tokenTypes.get("list1"));
	        }
	        else if (listMod == 1) {
	            styles.add(tokenTypes.get("list2"));
	        }
	        else {
	            styles.add(tokenTypes.get("list3"));
	        }
	    }
	    if (state.trailingSpaceNewLine) {
	        styles.add("trailing-space-new-line");
	    }
	    else if (state.trailingSpace != 0) {
	        styles.add("trailing-space-" + (state.trailingSpace % 2 != 0 ? "a" : "b"));
	    }
	    return !styles.isEmpty() ? StringUtils.join(styles, " ") : "";
	}
	
	Processor handleText() {
		return (stream, state) -> {
			if (!stream.match(textRE, true).isEmpty()) {
				return getType(state);
			}
			return "undefined";
		};
	}
	
	static final Pattern patternForInlineNormal[] = new Pattern[15];
	static {
		patternForInlineNormal[0] = Pattern.compile("^#+$");
		patternForInlineNormal[1] = Pattern.compile("([.?*+^$[\\\\]\\\\(){}|-])");
		patternForInlineNormal[2] = Pattern.compile("\\[[^\\]]*\\] ?(?:\\(|\\[)");
		patternForInlineNormal[3] = Pattern.compile("[^\\]]*\\](\\(.*\\)| ?\\[.*?\\])");
		patternForInlineNormal[4] = Pattern.compile("\\(.*?\\)| ?\\[.*?\\]");
		patternForInlineNormal[5] = Pattern.compile("^(https?|ftps?):\\/\\/(?:[^\\\\>]|\\\\.)+>");
		patternForInlineNormal[6] = Pattern.compile("^[^> \\\\]+@(?:[^\\\\>]|\\\\.)+>");
		patternForInlineNormal[7] = Pattern.compile("^(!--|\\w)");
		patternForInlineNormal[8] = Pattern.compile("markdown\\s*=\\s*('|\"){0,1}1('|\"){0,1}");
		patternForInlineNormal[9] = Pattern.compile("^\\/\\w*?>");
		patternForInlineNormal[10] = Pattern.compile("(\\w)");
		patternForInlineNormal[11] = Pattern.compile("(\\w)");
		patternForInlineNormal[12] = Pattern.compile("^[^\\s]");
		patternForInlineNormal[13] = Pattern.compile("^~~");
		patternForInlineNormal[14] = Pattern.compile(" +$");
	}
	
	Processor inlineNormal() {
		return (stream, state) -> {
		    String style = state.text.process(stream, state);
		    if (!style.equals("undefined")) return style;
		    if (state.list == Boolean.TRUE) {
		        state.list = null;
		        return getType(state);
		    }
		    if (state.taskList) {
		        boolean taskOpen = !stream.match(taskListRE, true).get(1).equals("x");
		        if (taskOpen) state.taskOpen = true;
		        else state.taskClosed = true;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("task");
		        }
		        state.taskList = false;
		        return getType(state);
		    }
		    state.taskOpen = false;
		    state.taskClosed = false;
		    if (state.header != 0 && !stream.match(patternForInlineNormal[0], true).isEmpty()) {
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("header");
		        }
		        return getType(state);
		    }
		    boolean sol = stream.sol();
		    String ch = stream.next();
		    if (state.linkTitle) {
		        state.linkTitle = false;
		        String matchCh = ch;
		        if (ch.equals("(")) {
		            matchCh = ")";
		        }
		        Matcher matcher = patternForInlineNormal[1].matcher(matchCh);
		        matchCh = matcher.replaceAll("\\$1");
		        String regex = "^\\s*(?:[^" + matchCh + "\\\\]+|\\\\\\\\|\\\\.)" + matchCh;
		        if (!stream.match(Pattern.compile(regex), true).isEmpty()) {
		            return tokenTypes.get("linkHref");
		        }
		    }
		    if (ch.equals("`")) {
		        List<String> previousFormatting = state.formatting;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("code");
		        }
		        stream.eatWhile("`");
		        int count = stream.current().length();
		        if (state.code == 0) {
		            state.code = count;
		            return getType(state);
		        }
		        else if (count == state.code) {
		            String t = getType(state);
		            state.code = 0;
		            return t;
		        }
		        else {
		            state.formatting = previousFormatting;
		            return getType(state);
		        }
		    }
		    else if (state.code != 0) {
		        return getType(state);
		    }
		    if (ch.equals("\\")) {
		        stream.next();
		        if (highlightFormatting) {
		            String type = getType(state);
		            String formattingEscape = tokenTypes.get("formatting") + "-escape";
		            return !type.isEmpty() ? type + " " + formattingEscape : formattingEscape;
		        }
		    }
		    if (ch.equals("!") && !stream.match(patternForInlineNormal[2],false).isEmpty()) {
		        state.imageMarker = true;
		        state.image = true;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("image");
		        }
		        return getType(state);
		    }
		    if (ch.equals("[") && state.imageMarker) {
		        state.imageMarker = false;
		        state.imageAltText = true;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("image");
		        }
		        return getType(state);
		    }
		    if (ch.equals("]") && state.imageAltText) {
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("image");
		        }
		        String type = getType(state);
		        state.imageAltText = false;
		        state.image = false;
		        state.inline = state.f = linkHref();
		        return type;
		    }
		    if (ch.equals("[") && !stream.match(patternForInlineNormal[3], false).isEmpty() && !state.image) {
		        state.linkText = true;
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link");
		        }
		        return getType(state);
		    }
		    if (ch.equals("]") && state.linkText && !stream.match(patternForInlineNormal[4], false).isEmpty()) {
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link");
		        }
		        String type = getType(state);
		        state.linkText = false;
		        state.inline = state.f = linkHref();
		        return type;
		    }
		    if (ch.equals("<") && !stream.match(patternForInlineNormal[5], false).isEmpty()) {
		        state.f = state.inline = linkInline();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link");
		        }
		        String type = getType(state);
		        if (!type.isEmpty()) {
		            type += " ";
		        }
		        else {
		            type = "";
		        }
		        return type + tokenTypes.get("linkInline");
		    }
		    if (ch.equals("<") && !stream.match(patternForInlineNormal[6], false).isEmpty()) {
		        state.f = state.inline = linkInline();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link");
		        }
		        String type = getType(state);
		        if (!type.isEmpty()) {
		            type += " ";
		        }
		        else {
		            type = "";
		        }
		        return type + tokenTypes.get("linkEmail");
		    }
		    if (ch.equals("<") && !stream.match(patternForInlineNormal[7], false).isEmpty()) {
		        int end = stream.string().indexOf(">", stream.pos());
		        if (end != -1) {
		            String atts = StringUtils.substring(stream.string(), stream.start(), end);
		            if (patternForInlineNormal[8].matcher(atts).matches()) state.md_inside = true;
		        }
		        stream.backUp(1);
		        state.htmlState = htmlMode.startState();
		        return switchBlock(stream, state, htmlBlock());
		    }
		    if (ch.equals("<") && !stream.match(patternForInlineNormal[9]).isEmpty()) {
		        state.md_inside = false;
		        return "tag";
		    }
		    boolean ignoreUnderscore = false;
		    if (!underscoresBreakWords) {
		        if (ch.equals("_") && !stream.peek().equals("_") && !stream.match(patternForInlineNormal[10],false).isEmpty()) {
		            int prevPos = stream.pos() - 2;
		            if (prevPos >= 0) {
		                char prevCh = stream.string().charAt(prevPos);
		                if (prevCh != '_' && patternForInlineNormal[11].matcher(prevCh + "").matches()) {
		                    ignoreUnderscore = true;
		                }
		            }
		        }
		    }
		    if (ch.equals("*") || (ch.equals("_") && !ignoreUnderscore)) {
		        if (sol && stream.peek().equals(" ")) {
		        }
		        else if (state.strong.equals(ch) && !stream.eat(ch).isEmpty()) {
		            if (highlightFormatting) {
		            	state.formatting.clear();
		            	state.formatting.add("strong");
		            }
		            String t = getType(state);
		            state.strong = "false";
		            return t;
		        }
		        else if (state.strong.equals("false")  && !stream.eat(ch).isEmpty()) {
		            state.strong = ch;
		            if (highlightFormatting) {
		            	state.formatting.clear();
		            	state.formatting.add("strong");
		            }
		            return getType(state);
		        }
		        else if (state.em.equals(ch)) {
		            if (highlightFormatting) {
		            	state.formatting.clear();
		            	state.formatting.add("em");
		            }
		            String t = getType(state);
		            state.em = "false";
		            return t;
		        }
		        else if (state.em.equals("false")) {
		            state.em = ch;
		            if (highlightFormatting) {
		            	state.formatting.clear();
		            	state.formatting.add("em");
		            }
		            return getType(state);
		        }
		    }
		    else if (ch.equals(" ")) {
		        if (!stream.eat("*").isEmpty() || !stream.eat("_").isEmpty()) {
		            if (stream.peek().equals(" ")) {
		                return getType(state);
		            }
		            else {
		                stream.backUp(1);
		            }
		        }
		    }
		    if (strikethrough) {
		        if (ch.equals("~") && stream.eatWhile(ch)) {
		            if (state.strikethrough) {
		                if (highlightFormatting) {
			            	state.formatting.clear();
			            	state.formatting.add("strikethrough");
		                }
		                String t = getType(state);
		                state.strikethrough = false;
		                return t;
		            }
		            else if (!stream.match(patternForInlineNormal[12], false).isEmpty()) {
		                state.strikethrough = true;
		                if (highlightFormatting) {
		                	state.formatting.clear();
		                	state.formatting.add("strikethrough");
		                }
		                return getType(state);
		            }
		        }
		        else if (ch.equals(" ")) {
		            if (!stream.match(patternForInlineNormal[13], true).isEmpty()) {
		                if (stream.peek().equals(" ")) {
		                    return getType(state);
		                }
		                else {
		                    stream.backUp(2);
		                }
		            }
		        }
		    }
		    if (ch.equals(" ")) {
		        if (!stream.match(patternForInlineNormal[14], false).isEmpty()) {
		            state.trailingSpace++;
		        }
		        else if (state.trailingSpace != 0) {
		            state.trailingSpaceNewLine = true;
		        }
		    }
		    return getType(state);
		};
	}
	
	static final Pattern patternForLinkInline = Pattern.compile("^[^>]+");
	
	Processor linkInline() {
		return (stream, state) -> {
		    String ch = stream.next();
		    if (ch.equals(">")) {
		        state.f = state.inline = inlineNormal();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link");
		        }
		        String type = getType(state);
		        if (!type.isEmpty()) {
		            type += " ";
		        }
		        else {
		            type = "";
		        }
		        return type + tokenTypes.get("linkInline");
		    }
		    stream.match(patternForLinkInline, true);
		    return tokenTypes.get("linkInline");
		};
	}
	
	Processor linkHref() {
		return (stream, state) -> {
		    if (stream.eatSpace()) {
		        return "";
		    }
		    String ch = stream.next();
		    if (ch.equals("(") || ch.equals("[")) {
		        state.f = state.inline = getLinkHrefInside(ch.equals("(") ? ")" : "]");
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link-string");
		        }
		        state.linkHref = true;
		        return getType(state);
		    }
		    return "error";
		};
	}
	
	static final Map<String, Pattern> linkRE = Maps.newHashMap();
	static {
		linkRE.put(")", Pattern.compile("^(?:[^\\\\\\(\\)]|\\\\.|\\((?:[^\\\\\\(\\)]|\\\\.)*\\))*?(?=\\))"));
		linkRE.put("]", Pattern.compile("^(?:[^\\\\\\[\\]]|\\\\.|\\[(?:[^\\\\\\[\\\\]]|\\\\.)*\\])*?(?=\\])"));
	}
	
	Processor getLinkHrefInside(String endChar) {
		return (stream, state) -> {
		    String ch = stream.next();
		    if (ch.equals(endChar)) {
		        state.f = state.inline = inlineNormal();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link-string");
		        }
		        String returnState = getType(state);
		        state.linkHref = false;
		        return returnState;
		    }
		    stream.match(linkRE.get(endChar));
		    state.linkHref = true;
		    return getType(state);
		};
	}
	
	static final Pattern patternForFootnoteLink = Pattern.compile("^([^\\]\\\\]|\\\\.)*\\]:");
	
	Processor footnoteLink() {
		return (stream, state) -> {
		    if (!stream.match(patternForFootnoteLink, false).isEmpty()) {
		        state.f = footnoteLinkInside();
		        stream.next();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link");
		        }
		        state.linkText = true;
		        return getType(state);
		    }
		    return switchInline(stream, state, inlineNormal());
		};
	}
	
	static final Pattern patternForFootnoteLinkInside[] = new Pattern[2];
	static {
		patternForFootnoteLinkInside[0] = Pattern.compile("^\\]:");
		patternForFootnoteLinkInside[1] = Pattern.compile("^([^\\]\\\\]|\\\\.)+");
	}
	
	Processor footnoteLinkInside() {
		return (stream, state) -> {
		    if (!stream.match(patternForFootnoteLinkInside[0], true).isEmpty()) {
		        state.f = state.inline = footnoteUrl();
		        if (highlightFormatting) {
		        	state.formatting.clear();
		        	state.formatting.add("link");
		        }
		        String returnType = getType(state);
		        state.linkText = false;
		        return returnType;
		    }
		    stream.match(patternForFootnoteLinkInside[1], true);
		    return tokenTypes.get("linkText");
		};
	}
	
	static final Pattern patternForFootnoteUrl[] = new Pattern[2];
	static {
		patternForFootnoteUrl[0] = Pattern.compile("^[^\\s]+");
		patternForFootnoteUrl[1] = Pattern.compile("^(?:\\s+(?:\"(?:[^\"\\\\]|\\\\\\\\|\\\\.)+\"|'(?:[^'\\\\]|\\\\\\\\|\\\\.)+'|\\((?:[^)\\\\]|\\\\\\\\|\\\\.)+\\)))?");
	}
	
	Processor footnoteUrl() {
		return (stream, state) -> {
		    if (stream.eatSpace()) {
		        return "";
		    }
		    stream.match(patternForFootnoteUrl[0], true);
		    if (stream.peek().equals("")) {
		        state.linkTitle = true;
		    }
		    else {
		        stream.match(patternForFootnoteUrl[1], true);
		    }
		    state.f = state.inline = inlineNormal();
		    return tokenTypes.get("linkHref") + " url";
		};
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "markdown", "md", "mkd");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-markdown");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("markdown");
	}

	@Override
	public State startState() {
		return new State();
	}
	
	static final Pattern pattern[] = new Pattern[3];
	static {
	    pattern[0] = Pattern.compile("^\\s*$");
	    pattern[1] = Pattern.compile("^\\s*");
	    pattern[2] = Pattern.compile("\\t");
	}

	@Override
	public String token(StringStream stream, State state) {
	    state.formatting.clear();;
	    if (!stream.string().equals(state.thisLine)) {
	        boolean forceBlankLine = state.header != 0 || state.hr;
	        state.header = 0;
	        state.hr = false;
	        if (!stream.match(pattern[0], true).isEmpty() || forceBlankLine) {
	            blankLine(state);
	            if (!forceBlankLine) return "";
	            state.prevLine = "";
	        }
	        state.prevLine = state.thisLine;
	        state.thisLine = stream.string();
	        state.taskList = false;
	        state.trailingSpace = 0;
	        state.trailingSpaceNewLine = false;
	        state.f = state.block;
	        int indentation = 0;
	        List<String> match = stream.match(pattern[1], true);
	        if (!match.isEmpty()) {
	        	Matcher matcher = pattern[2].matcher(match.get(0));
	        	indentation = matcher.replaceAll("    ").length();
	        }
	        state.indentationDiff = Math.min(indentation - state.indentation, 4);
	        state.indentation = state.indentation + state.indentationDiff;
	        if (indentation > 0) return "";
	    }
	    return state.f.process(stream, state);
	}
}
