package io.onedev.commons.jsyntax.htmlembedded;

import io.onedev.commons.jsyntax.addon.Multiplex;
import io.onedev.commons.jsyntax.clike.JavaTokenizer;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;

public class JspTokenizer extends Multiplex {

	public JspTokenizer() {
		super(new HtmlMixedTokenizer(), new Mode("<%", "%>", new JavaTokenizer(), "", "", false));
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "jsp");
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/x-jsp");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("jsp");
	}
}
