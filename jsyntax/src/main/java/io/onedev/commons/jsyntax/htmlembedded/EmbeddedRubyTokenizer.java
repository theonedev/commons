package io.onedev.commons.jsyntax.htmlembedded;

import io.onedev.commons.jsyntax.addon.Multiplex;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;
import io.onedev.commons.jsyntax.ruby.RubyTokenizer;

public class EmbeddedRubyTokenizer extends Multiplex {

	public EmbeddedRubyTokenizer() {
		super(new HtmlMixedTokenizer(), new Mode("<%", "%>", new RubyTokenizer(), "", "", false));
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "erb");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/x-erb");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("erb");
	}
}
