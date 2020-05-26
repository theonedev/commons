package io.onedev.commons.jsyntax.jsx;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.javascript.AbstractJavaScriptTokenizer;
import io.onedev.commons.jsyntax.javascript.JavaScriptTokenizer;
import io.onedev.commons.jsyntax.xml.XmlTokenizer;

public class JsxTokenizer extends AbstractTokenizer<JsxTokenizer.State> {

	static final XmlTokenizer xmlMode = new XmlTokenizer(false, true);
	static final JavaScriptTokenizer jsMode = new JavaScriptTokenizer();
	
	static class Context {
		@SuppressWarnings("rawtypes")
		AbstractTokenizer mode;
		Object state;
		Integer depth;
		Context prev;
		
		@SuppressWarnings("rawtypes")
		public Context(AbstractTokenizer mode, Object state, Integer depth, Context prev) {
			this.mode = mode;
			this.state = state;
			this.depth = depth;
			this.prev = prev;
		}
	}
	
	static class State {
		Context context;

		public State(Context context) {
			this.context = context;
		}
	}
	
	static final Pattern pattern[] = new Pattern[5];
	static {
	    pattern[0] = Pattern.compile("^.*?\\*\\/");
	    pattern[1] = Pattern.compile("^[^>]*>\\s*$");
	    pattern[2] = Pattern.compile("\\btag\\b");
	    pattern[3] = Pattern.compile(">$");
	    pattern[4] = Pattern.compile("^<");
	}
	
	String xmlToken(StringStream stream, State state, Context cx) {
	    if (cx.depth == Integer.valueOf(2)) {
	        if (!stream.match(pattern[0]).isEmpty()) cx.depth = 1;
	        else stream.skipToEnd();
	        return "comment";
	    }
	    if (stream.peek().equals("{")) {
	        xmlMode.skipAttribute((XmlTokenizer.State)cx.state);
	        state.context = new Context(jsMode, jsMode.startState(), 0, state.context);
	        return "";
	    }
	    if (cx.depth == Integer.valueOf(1)) {
	        if (stream.peek().equals("<")) {
	            xmlMode.skipAttribute((XmlTokenizer.State)cx.state);
	            state.context = new Context(xmlMode, xmlMode.startState(), 0, state.context);
	            return "";
	        }
	        else if (stream.match("//")) {
	            stream.skipToEnd();
	            return "comment";
	        }
	        else if (stream.match("/*")) {
	            cx.depth = 2;
	            return token(stream, state);
	        }
	    }
	    String style = xmlMode.token(stream, (XmlTokenizer.State)cx.state), cur = stream.current();
	    int stop;
	    if (pattern[2].matcher(style).find()) {
	        if (pattern[3].matcher(cur).find()) {
	            if (!((XmlTokenizer.State)cx.state).isContextNull()) cx.depth = 0;
	            else state.context = state.context.prev;
	        }
	        else if (pattern[4].matcher(cur).find()) {
	            cx.depth = 1;
	        }
	    }
	    else if (style.isEmpty() && (stop = cur.indexOf("{")) > -1) {
	        stream.backUp(cur.length() - stop);
	    }
	    return style;
	}
	
	String jsToken(StringStream stream, State state, Context cx) {
	    if (stream.peek().equals("<") && JavaScriptTokenizer.expressionAllowed(stream, (AbstractJavaScriptTokenizer.State)cx.state)) {
	        jsMode.skipExpression((AbstractJavaScriptTokenizer.State)cx.state);
	        state.context = new Context(xmlMode, xmlMode.startState(), 0, state.context);
	        return "";
	    }
	    String style = jsMode.token(stream, (AbstractJavaScriptTokenizer.State)cx.state);
	    if (style.isEmpty() && cx.depth != null) {
	        String cur = stream.current();
	        if (cur.equals("{")) {
	            cx.depth++;
	        }
	        else if (cur.equals("}")) {
	            if (--cx.depth == 0) state.context = state.context.prev;
	        }
	    }
	    return style;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "js", "jsx");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/jsx");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("jsx");
	}

	@Override
	public State startState() {
		return new State(new Context(jsMode, jsMode.startState(), null, null));
	}

	@Override
	public String token(StringStream stream, State state) {
		if (state.context.mode == xmlMode)
			return xmlToken(stream, state, state.context);
		else
			return jsToken(stream, state, state.context);
	}
}
