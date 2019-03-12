package io.onedev.commons.jsyntax.htmlembedded;

import io.onedev.commons.jsyntax.addon.Multiplex;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;
import io.onedev.commons.jsyntax.javascript.JavaScriptTokenizer;

public class EmbeddedJavascriptTokenizer extends Multiplex {

	public EmbeddedJavascriptTokenizer() {
		super(new HtmlMixedTokenizer(), new Mode("<%", "%>", new JavaScriptTokenizer(), "", "", false));
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "ejs");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/x-ejs");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("ejs");
	}
}
