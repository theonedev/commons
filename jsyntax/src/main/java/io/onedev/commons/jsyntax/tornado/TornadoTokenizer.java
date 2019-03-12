package io.onedev.commons.jsyntax.tornado;

import io.onedev.commons.jsyntax.addon.Overlay;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;

public class TornadoTokenizer extends Overlay {

	public TornadoTokenizer() {
		super(new HtmlMixedTokenizer(), new TornadoInnerTokenizer());
	}

	@Override
	public boolean accept(String fileName) {
		return false;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-tornado");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("tornado");
	}
}
