package io.onedev.commons.jsyntax.slim;

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
import io.onedev.commons.jsyntax.TokenizerUtils;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;
import io.onedev.commons.jsyntax.ruby.RubyTokenizer;

public class SlimTokenizer extends AbstractTokenizer<SlimTokenizer.State> {

	static final HtmlMixedTokenizer htmlMode = new HtmlMixedTokenizer();
	static final RubyTokenizer rubyMode = new RubyTokenizer();

	static final Map<String, String> styleMap = Maps.newHashMap();
	static {
		styleMap.put("commentLine", "comment");
		styleMap.put("slimSwitch", "operator special");
		styleMap.put("slimTag", "tag");
		styleMap.put("slimId", "attribute def");
		styleMap.put("slimClass", "attribute qualifier");
		styleMap.put("slimAttribute", "attribute");
		styleMap.put("slimSubmode", "keyword special");
		styleMap.put("closeAttributeTag", "");
		styleMap.put("slimDoctype", "");
		styleMap.put("lineContinuation", "");
	}

	static final Map<String, String> closing = Maps.newHashMap();
	static {
		closing.put("{", "}");
		closing.put("[", "]");
		closing.put("(", ")");
	}

	static final List<String> arr = Lists.newArrayList("ruby", "javascript", "css", "sass", "scss", "less", "styl",
			"coffee", "asciidoc", "markdown", "textile", "creole", "wiki", "mediawiki", "rdoc", "builder", "nokogiri",
			"erb");

	static final Pattern embeddedRegexp = Pattern.compile("^(" + StringUtils.join(arr, "|") + "):");

	static final String nameStartChar = "_a-zA-Z\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD";
	static final String nameChar = nameStartChar + "\\-0-9\0267\u0300-\u036F\u203F-\u2040";

	static final Pattern nameRegexp = Pattern
			.compile("^[:" + nameStartChar + "](?::[" + nameChar + "]|[" + nameChar + "]*)");
	static final Pattern attributeNameRegexp = Pattern
			.compile("^[:" + nameStartChar + "][:\\." + nameChar + "]*(?=\\s*=)");
	static final Pattern wrappedAttributeNameRegexp = Pattern
			.compile("^[:" + nameStartChar + "][:\\." + nameChar + "]*");
	static final Pattern classNameRegexp = Pattern.compile("\\.-?[_a-zA-Z]+[\\w\\-]*");
	static final Pattern classIdRegexp = Pattern.compile("^#[_a-zA-Z]+[\\w\\-]*");

	@FunctionalInterface
	static interface Processor {
		String process(StringStream stream, State state);
	}

	static class Element {
		String style;
		int indented;
		Processor tokenize;
		Processor line;
		String endQuote;

		public Element(String style, int indented, Processor tokenize, Processor line, String endQuote) {
			this.style = style;
			this.indented = indented;
			this.tokenize = tokenize;
			this.line = line;
			this.endQuote = endQuote;
		}
	}

	@SuppressWarnings("rawtypes")
	static class State {
		HtmlMixedTokenizer.State htmlState;
		RubyTokenizer.State rubyState;
		AbstractTokenizer subMode;
		Object subState;
		Stack<Element> stack;
		String last;
		Processor tokenize;
		Processor line;
		int indented;
		boolean startOfLine;

		public State(AbstractTokenizer subMode, Object subState, Stack<Element> stack, String last, Processor tokenize,
				Processor line, int indented) {
			this.htmlState = htmlMode.startState();
			this.rubyState = rubyMode.startState();
			this.subMode = subMode;
			this.subState = subState;
			this.stack = stack;
			this.last = last;
			this.tokenize = tokenize;
			this.line = line;
			this.indented = indented;
			this.startOfLine = false;
		}
	}

	static Processor backup(int pos, Processor tokenize, String style) {
		Processor restore = (stream, state) -> {
			state.tokenize = tokenize;
			if (stream.pos() < pos) {
				stream.pos(pos);
				return style;
			}
			return state.tokenize.process(stream, state);
		};
		return (stream, state) -> {
			state.tokenize = restore;
			return tokenize.process(stream, state);
		};
	}
	
	static String maybeBackup(StringStream stream, State state, Pattern pat, int offset, String style) {
	    String cur = stream.current();
	    int idx = -1;
	    Matcher matcher = pat.matcher(cur);
	    if (matcher.find()) {
	    	idx = matcher.start();
	    }
	    if (idx > -1) {
	        state.tokenize = backup(stream.pos(), state.tokenize, style);
	        stream.backUp(cur.length() - idx - offset);
	    }
	    return style;
	}

	static void continueLine(State state, int column) {
	    state.stack.push(new Element("continuation", column, state.line, null, ""));
	    state.line = state.tokenize;
	}
	
	static void finishContinue(State state) {
		if (state.line == state.tokenize) {
			state.line = state.stack.peek().tokenize;
			state.stack.pop();
		}
	}
	
	static final Pattern patternForLineContinuable[] = new Pattern[2];
	static {
		patternForLineContinuable[0] = Pattern.compile("^\\\\$");
		patternForLineContinuable[1] = Pattern.compile("(?:^|[^\\\\])(?:\\\\\\\\)*\\\\$");
	}
	
	static Processor lineContinuable(int column, Processor tokenize) {
		return (stream, state) -> {
		    finishContinue(state);
		    if (!stream.match(patternForLineContinuable[0]).isEmpty()) {
		        continueLine(state, column);
		        return "lineContinuation";
		    }
		    String style = tokenize.process(stream, state);
		    if (stream.eol() && patternForLineContinuable[1].matcher(stream.current()).matches()) {
		        stream.backUp(1);
		    }
		    return style;
		};
	}
	
	static final Pattern patternForCommaContinuable = Pattern.compile(",$");
	
	static Processor commaContinuable(int column, Processor tokenize) {
		return (stream, state) -> {
		    finishContinue(state);
		    String style = tokenize.process(stream, state);
		    if (stream.eol() && patternForCommaContinuable.matcher(stream.current()).matches()) {
		        continueLine(state, column);
		    }
		    return style;
		};
	}
	
	static Processor rubyInQuote(String endQuote, Processor tokenize) {
		return (stream, state) -> {
		    String ch = stream.peek();
		    if (ch.equals(endQuote) && state.rubyState.getTokenize().size() == 1) {
		        stream.next();
		        state.tokenize = tokenize;
		        return "closeAttributeTag";
		    }
		    else {
		        return ruby.process(stream, state);
		    }
		};
	}
	
	static Processor runSplat(Processor tokenize, RubyTokenizer.State rubyState) {
		return (stream, state) -> {
		    if (state.rubyState.getTokenize().size() == 1 && state.rubyState.isContextPrevNull()) {
		        stream.backUp(1);
		        if (stream.eatSpace()) {
		            state.rubyState = rubyState;
		            state.tokenize = tokenize;
		            return tokenize.process(stream, state);
		        }
		        stream.next();
		    }
		    return ruby.process(stream, state);
		};
	}
	
	static Processor startRubySplat(Processor tokenize) {
		return (stream, state) -> {
			state.tokenize = runSplat(tokenize, state.rubyState);
			state.rubyState = rubyMode.startState();
			return ruby.process(stream, state);
		};
	}
	
	static final Processor ruby = (stream, state) -> {
		return rubyMode.token(stream, state.rubyState);
	};
	
	static final Pattern patternForHtmlLine = Pattern.compile("^\\\\$");

	static String htmlLine(StringStream stream, State state) {
	    if (!stream.match(patternForHtmlLine).isEmpty()) {
	        return "lineContinuation";
	    }
	    return html.process(stream, state);
	}
	
	static final Pattern patternForHtml[] = new Pattern[2];
	static {
		patternForHtml[0] = Pattern.compile("^#\\{");
	    patternForHtml[1] = Pattern.compile("[^\\\\]#\\{");
	}
	
	static final Processor html = (stream, state) -> {
	    if (!stream.match(patternForHtml[0]).isEmpty()) {
	        state.tokenize = rubyInQuote("}", state.tokenize);
	        return "";
	    }
	    return maybeBackup(stream, state, patternForHtml[1], 1, htmlMode.token(stream, state.htmlState));
	};
	
	static Processor startHtmlLine(Processor lastTokenize) {
		return (stream, state) -> {
			String style = htmlLine(stream, state);
			if (stream.eol()) {
				state.tokenize = lastTokenize;
			}
			return style;
		};
	}
	
	static String startHtmlMode(StringStream stream, State state, int offset) {
		state.stack.push(new Element("html", stream.column() + offset, state.line, null, ""));
		state.line = state.tokenize = html;
		return "";
	}
	
	static final Processor comment = (stream, state) -> {
		stream.skipToEnd();
		return state.stack.peek().style;
	};
	
	static final Processor commentMode = (stream, state) -> {
		state.stack.push(new Element("comment", state.indented + 1, state.line, null, ""));
		state.line = comment;
		return comment.process(stream, state);
	};
	
	static Processor attributeWrapper() { 
		return (stream, state) -> {
		    if (!stream.eat(state.stack.peek().endQuote).isEmpty()) {
		        state.line = state.stack.peek().line;
		        state.tokenize = state.stack.peek().tokenize;
		        state.stack.pop();
		        return "";
		    }
		    if (!stream.match(wrappedAttributeNameRegexp).isEmpty()) {
		        state.tokenize = attributeWrapperAssign();
		        return "slimAttribute";
		    }
		    stream.next();
		    return "";
		};
	}
	
	static final Pattern patternForAttributeWrapperAssign = Pattern.compile("^==?");

	static Processor attributeWrapperAssign() {
		return (stream, state) -> {
		    if (!stream.match(patternForAttributeWrapperAssign).isEmpty()) {
		        state.tokenize = attributeWrapperValue;
		        return "";
		    }
		    return attributeWrapper().process(stream, state);
		};
	}
	
	static final Pattern patternForAttributeWrapperValue = Pattern.compile("^(true|false|nil)\\b");

	static final Processor attributeWrapperValue = (stream, state) -> {
	    String ch = stream.peek();
	    if (ch.equals("\"") || ch.equals("\'")) {
	        state.tokenize = readQuoted(ch, "string", true, false, attributeWrapper());
	        stream.next();
	        return state.tokenize.process(stream, state);
	    }
	    if (ch.equals("[")) {
	        return startRubySplat(attributeWrapper()).process(stream, state);
	    }
	    if (!stream.match(patternForAttributeWrapperValue).isEmpty()) {
	        state.tokenize = attributeWrapper();
	        return "keyword";
	    }
	    return startRubySplat(attributeWrapper()).process(stream, state);
	};
	
	static String startAttributeWrapperMode(State state, String endQuote, Processor tokenize) {
		state.stack.push(new Element("wrapper", state.indented + 1, tokenize, state.line, endQuote));
		state.line = state.tokenize = attributeWrapper();
		return "";
	}
	
	static final Pattern patternForSub = Pattern.compile("^#\\{");

	static final Processor sub = (stream, state) -> {
	    if (!stream.match(patternForSub).isEmpty()) {
	        state.tokenize = rubyInQuote("}", state.tokenize);
	        return "";
	    }
	    StringStream subStream = new StringStream(TokenizerUtils.slice(stream.string(), state.stack.peek().indented));
	    subStream.pos(stream.pos() - state.stack.peek().indented);
	    subStream.start(stream.start() - state.stack.peek().indented);
	    @SuppressWarnings("unchecked")
		String style = state.subMode.token(subStream, state.subState);
	    stream.pos(subStream.pos() + state.stack.peek().indented);
	    return style;
	};
	
	static final Processor firstSub = (stream, state) -> {
		state.stack.peek().indented = stream.column();
		state.line = state.tokenize = sub;
		return state.tokenize.process(stream, state);
	};
	
	@SuppressWarnings("rawtypes")
	static AbstractTokenizer getMode(String mode) {
		Tokenizer m = TokenizerRegistry.getTokenizerByMime(mode);
		if (m == null) {
			m = TokenizerRegistry.getTokenizerByMode(mode);
		}
		if (m == null) {
			m = TokenizerRegistry.getTokenizerByMode("null");
		}
		return (AbstractTokenizer)m;
	}
	
	@SuppressWarnings("rawtypes")
	static String startSubMode(String mode, State state) {
		AbstractTokenizer subMode = getMode(mode);
		Object subState = subMode.startState();
		state.subMode = subMode;
		state.subState = subState;
		state.stack.push(new Element("sub", state.indented + 1, state.line, null, ""));
		state.line = state.tokenize = firstSub;
		return "slimSubmode";
	}
	
	static final Processor doctypeLine = (stream, state) -> {
		stream.skipToEnd();
		return "slimDoctype";
	};
	
	static final Pattern patternForStartLine[] = new Pattern[4];
	static {
		patternForStartLine[0] = Pattern.compile("^[|']");
		patternForStartLine[1] = Pattern.compile("^\\/(!|\\[\\w+])?");
		patternForStartLine[2] = Pattern.compile("^(-|==?[<>]?)");
		patternForStartLine[3] = Pattern.compile("^doctype\\b");
	}
	
	static final Processor startLine = (stream, state) -> {
	    String ch = stream.peek();
	    if (ch.equals("<")) {
	        return (state.tokenize = startHtmlLine(state.tokenize)).process(stream, state);
	    }
	    if (!stream.match(patternForStartLine[0]).isEmpty()) {
	        return startHtmlMode(stream, state, 1);
	    }
	    if (!stream.match(patternForStartLine[1]).isEmpty()) {
	        return commentMode.process(stream, state);
	    }
	    if (!stream.match(patternForStartLine[2]).isEmpty()) {
	        state.tokenize = lineContinuable(stream.column(), commaContinuable(stream.column(), ruby));
	        return "slimSwitch";
	    }
	    if (!stream.match(patternForStartLine[3]).isEmpty()) {
	        state.tokenize = doctypeLine;
	        return "keyword";
	    }
	    List<String> m = stream.match(embeddedRegexp);
	    if (!m.isEmpty()) {
	        return startSubMode(m.get(1), state);
	    }
	    return slimTag().process(stream, state);
	};
	
	static final Processor slim = (stream, state) -> {
		if (state.startOfLine) {
			return startLine.process(stream, state);
		}
		return slimTag().process(stream, state);
	};
	
	static Processor slimTag() {
		return (stream, state) -> {
		    if (!stream.eat("*").isEmpty()) {
		        state.tokenize = startRubySplat(slimTagExtras());
		        return "";
		    }
		    if (!stream.match(nameRegexp).isEmpty()) {
		        state.tokenize = slimTagExtras();
		        return "slimTag";
		    }
		    return slimClass().process(stream, state);
		};
	}
	
	static final Pattern patternForSlimTagExtras = Pattern.compile("^(<>?|><?)");

	static Processor slimTagExtras() {
		return (stream, state) -> {
		    if (!stream.match(patternForSlimTagExtras).isEmpty()) {
		        state.tokenize = slimClass();
		        return "";
		    }
		    return slimClass().process(stream, state);
		};
	}

	static Processor slimClass() {
		return (stream, state) -> {
		    if (!stream.match(classIdRegexp).isEmpty()) {
		        state.tokenize = slimClass();
		        return "slimId";
		    }
		    if (!stream.match(classNameRegexp).isEmpty()) {
		        state.tokenize = slimClass();
		        return "slimClass";
		    }
		    return slimAttribute().process(stream, state);
		};
	}
	
	static final Pattern patternForSlimAttribute = Pattern.compile("^([\\[\\{\\(])");

	static Processor slimAttribute() {
		return (stream, state) -> {
			List<String> m;
		    if (!(m = stream.match(patternForSlimAttribute)).isEmpty()) {
		        return startAttributeWrapperMode(state, closing.get(m.get(1)), slimAttribute());
		    }
		    if (!stream.match(attributeNameRegexp).isEmpty()) {
		        state.tokenize = slimAttributeAssign();
		        return "slimAttribute";
		    }
		    if (stream.peek().equals("*")) {
		        stream.next();
		        state.tokenize = startRubySplat(slimContent());
		        return "";
		    }
		    return slimContent().process(stream, state);
		};
	}

	static final Pattern patternForSlimAttributeAssign = Pattern.compile("^==?");

	static Processor slimAttributeAssign() {
		return (stream, state) -> {
		    if (!stream.match(patternForSlimAttributeAssign).isEmpty()) {
		        state.tokenize = slimAttributeValue();
		        return "";
		    }
		    return slimAttribute().process(stream, state);
		};
	}
	
	static final Pattern patternForSlimAttributeValue = Pattern.compile("^(true|false|nil)\\b");
	
	static Processor slimAttributeValue() {
		return (stream, state) -> {
		    String ch = stream.peek();
		    if (ch.equals("\"") || ch.equals("\'")) {
		        state.tokenize = readQuoted(ch, "string", true, false, slimAttribute());
		        stream.next();
		        return state.tokenize.process(stream, state);
		    }
		    if (ch.equals("[")) {
		        return startRubySplat(slimAttribute()).process(stream, state);
		    }
		    if (ch.equals(":")) {
		        return startRubySplat(slimAttributeSymbols()).process(stream, state);
		    }
		    if (!stream.match(patternForSlimAttributeValue).isEmpty()) {
		        state.tokenize = slimAttribute();
		        return "keyword";
		    }
		    return startRubySplat(slimAttribute()).process(stream, state);
		};
	}
	
	static final Pattern patternForSlimAttributeSymbols = Pattern.compile("^[^\\s],(?=:)");

	static Processor slimAttributeSymbols() {
		return (stream, state) -> {
		    stream.backUp(1);
		    if (!stream.match(patternForSlimAttributeSymbols).isEmpty()) {
		        state.tokenize = startRubySplat(slimAttributeSymbols());
		        return "";
		    }
		    stream.next();
		    return slimAttribute().process(stream, state);
		};
	}

	static final Pattern patternForReadQuoted[] = new Pattern[2];
	static {
		patternForReadQuoted[0] = Pattern.compile("^\\\\$");
		patternForReadQuoted[1] = Pattern.compile("^#\\{");
	}
	
	static Processor readQuoted(String quote, String style, boolean embed, boolean unescaped, Processor nextTokenize) {
	    return (stream, state) -> {
	        finishContinue(state);
	        boolean fresh = stream.current().length() == 0;
	        if (!stream.match(patternForReadQuoted[0],fresh).isEmpty()) {
	            if (!fresh) return style;
	            continueLine(state, state.indented);
	            return "lineContinuation";
	        }
	        if (!stream.match(patternForReadQuoted[1],fresh).isEmpty()) {
	            if (!fresh) return style;
	            state.tokenize = rubyInQuote("}", state.tokenize);
	            return "";
	        }
	        boolean escaped = false;
	        String ch;
	        while (!(ch = stream.next()).isEmpty()) {
	            if (ch.equals(quote) && (unescaped || !escaped)) {
	                state.tokenize = nextTokenize;
	                break;
	            }
	            if (embed && ch.equals("#") && !escaped) {
	                if (!stream.eat("{").isEmpty()) {
	                    stream.backUp(2);
	                    break;
	                }
	            }
	            escaped = !escaped && ch.equals("\\");
	        }
	        if (stream.eol() && escaped) {
	            stream.backUp(1);
	        }
	        return style;
	    };
	}

	static final Pattern patternForSlimContent[] = new Pattern[3];
	static {
		patternForSlimContent[0] = Pattern.compile("^==?");
		patternForSlimContent[1] = Pattern.compile("^\\/$");
		patternForSlimContent[2] = Pattern.compile("^:");
	}
	
	static Processor slimContent() {
		return (stream, state) -> {
		    if (!stream.match(patternForSlimContent[0]).isEmpty()) {
		        state.tokenize = ruby;
		        return "slimSwitch";
		    }
		    if (!stream.match(patternForSlimContent[1]).isEmpty()) {
		        state.tokenize = slim;
		        return "";
		    }
		    if (!stream.match(patternForSlimContent[2]).isEmpty()) {
		        state.tokenize = slimTag();
		        return "slimSwitch";
		    }
		    startHtmlMode(stream, state, 0);
		    return state.tokenize.process(stream, state);
		};
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "slim");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && (mime.equals("text/x-slim") || mime.equals("application/x-slim"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("slim");
	}

	@Override
	public State startState() {
		return new State(null, null, new Stack<Element>(), "", slim, slim, 0);
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (stream.sol()) {
	        state.indented = stream.indentation();
	        state.startOfLine = true;
	        state.tokenize = state.line;
	        while (!state.stack.isEmpty() && state.stack.peek().indented > state.indented && !state.last.equals("slimSubmode")) {
	            state.line = state.tokenize = state.stack.peek().tokenize;
	            state.stack.pop();
	            state.subMode = null;
	            state.subState = null;
	        }
	    }
	    if (stream.eatSpace()) return "";
	    String style = state.tokenize.process(stream, state);
	    state.startOfLine = false;
	    if (!style.isEmpty()) state.last = style;
	    return styleMap.containsKey(style) ? styleMap.get(style) : style;
	}
}
