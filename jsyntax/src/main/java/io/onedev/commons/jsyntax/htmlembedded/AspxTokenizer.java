package io.onedev.commons.jsyntax.htmlembedded;

import io.onedev.commons.jsyntax.addon.Multiplex;
import io.onedev.commons.jsyntax.clike.CSharpTokenizer;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;

public class AspxTokenizer extends Multiplex {

	public AspxTokenizer() {
		super(new HtmlMixedTokenizer(), new Mode("<%", "%>", new CSharpTokenizer(), "", "", false));
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "aspx");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/x-aspx");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("asp.net");
	}
}
