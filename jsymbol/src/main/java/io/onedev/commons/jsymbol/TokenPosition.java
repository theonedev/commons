package io.onedev.commons.jsymbol;

import java.io.Serializable;

public class TokenPosition implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int fromLine, toLine, fromCh, toCh;
	
	public TokenPosition(int fromLine, int fromCh, int toLine, int toCh) {
		this.fromLine = fromLine;
		this.fromCh = fromCh;
		this.toLine = toLine;
		this.toCh = toCh;
	}

	public int getFromLine() {
		return fromLine;
	}

	public int getToLine() {
		return toLine;
	}

	public int getFromCh() {
		return fromCh;
	}

	public int getToCh() {
		return toCh;
	}

}
