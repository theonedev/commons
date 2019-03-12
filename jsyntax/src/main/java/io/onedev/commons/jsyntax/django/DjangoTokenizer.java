package io.onedev.commons.jsyntax.django;

import io.onedev.commons.jsyntax.addon.Overlay;
import io.onedev.commons.jsyntax.htmlmixed.HtmlMixedTokenizer;

public class DjangoTokenizer extends Overlay {

	public DjangoTokenizer() {
		super(new HtmlMixedTokenizer(), new DjangoInner());
	}

	@Override
	public boolean accept(String fileName) {
		return false;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-django");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("django");
	}
}
