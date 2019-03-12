package io.onedev.commons.jsyntax.addon;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public abstract class Overlay extends AbstractTokenizer<Overlay.State> {

	@SuppressWarnings("rawtypes")
	private final AbstractTokenizer base, overlay;
	private final boolean combineTokens, combine;
	
	static class State {
	    Object baseState;
	    Object overlayState;
	    int basePos;
	    String baseCur;
	    int overlayPos;
	    String overlayCur;
	    StringStream streamSeen;

	    public State(Object baseState, Object overlayState, int basePos, String baseCur, int overlayPos, String overlayCur, StringStream streamSeen) {
	        this.baseState = baseState;
	        this.overlayState = overlayState;
	        this.basePos = basePos;
	        this.baseCur = baseCur;
	        this.overlayPos = overlayPos;
	        this.overlayCur = overlayCur;
	        this.streamSeen = streamSeen;
	    }
	}
	
	@SuppressWarnings("rawtypes")
	public Overlay(AbstractTokenizer base, AbstractTokenizer overlay, boolean combineTokens, boolean combine) {
		this.base = base;
		this.overlay = overlay;
		this.combineTokens = combineTokens;
		this.combine = combine;
	}
	
	@SuppressWarnings("rawtypes")
	public Overlay(AbstractTokenizer base, AbstractTokenizer overlay) {
		this(base, overlay, false, false);
	}

	@Override
	public State startState() {
		return new State(base.startState(), overlay.startState(), 0, "", 0, "", null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public String token(StringStream stream, State state) {
	    if (stream != state.streamSeen || Math.min(state.basePos, state.overlayPos) < stream.start()) {
	        state.streamSeen = stream;
	        state.basePos = state.overlayPos = stream.start();
	    }
	    if (stream.start() == state.basePos) {
	        state.baseCur = base.token(stream, state.baseState);
	        state.basePos = stream.pos();
	    }
	    if (stream.start() == state.overlayPos) {
	        stream.pos(stream.start());
	        state.overlayCur = overlay.token(stream, state.overlayState);
	        state.overlayPos = stream.pos();
	    }
	    stream.pos(Math.min(state.basePos, state.overlayPos));
	    if (state.overlayCur.isEmpty())
	    	return state.baseCur;
	    else if (!state.baseCur.isEmpty() && combineTokens || combine && !combineTokens)
	    	return state.baseCur + " " + state.overlayCur;
	    else
	    	return state.overlayCur;
	}
}
