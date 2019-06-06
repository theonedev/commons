package io.onedev.commons.jsyntax.addon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.TokenizerUtils;

public abstract class Multiplex extends AbstractTokenizer<Multiplex.State> {

	static final int Infinity = 1000000007;
	@SuppressWarnings("rawtypes")
	private final AbstractTokenizer outer;
	private final Mode[] others;
	
	public static class Mode {
	    Object open;
	    Object close;
	    @SuppressWarnings("rawtypes")
		AbstractTokenizer mode;
	    String delimStyle;
	    String innerStyle;
	    boolean parseDelimiters;
	    
		@SuppressWarnings("rawtypes")
		public Mode(Object open, Object close, AbstractTokenizer mode, String delimStyle, String innerStyle, boolean parseDelimiters) {
	        this.open = open;
	        this.close = close;
	        this.mode = mode;
	        this.delimStyle = delimStyle;
	        this.innerStyle = innerStyle;
	        this.parseDelimiters = parseDelimiters;
	    }
	}
	
	static class State {
	    Object outerState;
	    Object innerState;
	    Mode innerActive;

	    public State(Object outerState, Object innerState, Mode innerActive) {
	        this.outerState = outerState;
	        this.innerState = innerState;
	        this.innerActive = innerActive;
	    }
	}
	
	@SuppressWarnings("rawtypes")
	public Multiplex(AbstractTokenizer outer, Mode... others) {
		this.outer = outer;
		this.others = others;
	}
	
	int indexOf(String string, Object pattern, int from, boolean returnEnd) {
	    if (pattern instanceof String) {
	        int found = string.indexOf((String)pattern, from);
	        return returnEnd && found > -1 ? found + ((String)pattern).length() : found;
	    }
	    Matcher m = ((Pattern)pattern).matcher(from > 0 ? TokenizerUtils.slice(string, from) : string);
	    return m.find() ? m.start() + from + (returnEnd ? m.group(0).length() : 0) : -1;
	}

	@Override
	public State startState() {
		return new State(outer.startState(), null, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public String token(StringStream stream, State state) {
	    if (state.innerActive == null) {
	        int cutOff = Infinity;
	        String oldContent = stream.string();
	        for (int i = 0; i < others.length; ++i) {
	            Mode other = others[i];
	            int found = indexOf(oldContent, other.open, stream.pos(), false);
	            if (found == stream.pos()) {
	                if (!other.parseDelimiters) {
	                	if (other.open instanceof String) stream.match((String)other.open);
	                	else if (other.open instanceof Pattern) stream.match((Pattern)other.open);
	                }
	                state.innerActive = other;
	                state.innerState = other.mode.startState();
	                return other.delimStyle.isEmpty() ? "" : other.delimStyle + " " + other.delimStyle + "-open";
	            }
	            else if (found != -1 && found < cutOff) {
	                cutOff = found;
	            }
	        }
	        if (cutOff != Infinity) stream.string(TokenizerUtils.slice(oldContent, 0, cutOff));
	        String outerToken = outer.token(stream, state.outerState);
	        if (cutOff != Infinity) stream.string(oldContent);
	        return outerToken;
	    }
	    else {
	        Mode curInner = state.innerActive;
	        String oldContent = stream.string();
	        if (curInner.close == null && stream.sol()) {
	            state.innerActive = null;
	            state.innerState = null;
	            return this.token(stream, state);
	        }
	        int found = curInner.close != null ? indexOf(oldContent, curInner.close, stream.pos(), curInner.parseDelimiters) : -1;
	        if (found == stream.pos() && !curInner.parseDelimiters) {
	        	if (curInner.close instanceof String) stream.match((String)curInner.close);
	        	else if (curInner.close instanceof Pattern) stream.match((Pattern)curInner.close);
	            state.innerActive = null;
	            state.innerState = null;
	            return curInner.delimStyle.isEmpty() ? "" : curInner.delimStyle + " " + curInner.delimStyle + "-close";
	        }
	        if (found > -1) stream.string(TokenizerUtils.slice(oldContent, 0, found));
	        String innerToken = curInner.mode.token(stream, state.innerState);
	        if (found > -1) stream.string(oldContent);
	        if (found == stream.pos() && curInner.parseDelimiters) {
	        	state.innerActive = null;
	        	state.innerState = null;
	        }
	        if (!curInner.innerStyle.isEmpty()) {
	            if (!innerToken.isEmpty()) innerToken = innerToken + " " + curInner.innerStyle;
	            else innerToken = curInner.innerStyle;
	        }
	        return innerToken;
	    }
	}
}
