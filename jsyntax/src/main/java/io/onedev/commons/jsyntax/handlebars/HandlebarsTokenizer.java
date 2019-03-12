package io.onedev.commons.jsyntax.handlebars;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.addon.Multiplex;
import io.onedev.commons.jsyntax.textplain.TextplainTokenizer;

public class HandlebarsTokenizer extends Multiplex {

	@SuppressWarnings("rawtypes")
	public HandlebarsTokenizer(AbstractTokenizer tokenizer) {
		super(tokenizer, new Mode("{{", "}}", new HandlebarsTags(), "", "", true));
	}
	
	public HandlebarsTokenizer() {
		this(new TextplainTokenizer());
	}
	
	@Override
	public boolean accept(String fileName) {
		return false;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-handlebars-template");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("handlebars");
	}
}
