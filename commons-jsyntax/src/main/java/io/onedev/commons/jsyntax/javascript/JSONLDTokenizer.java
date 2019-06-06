package io.onedev.commons.jsyntax.javascript;

public class JSONLDTokenizer extends AbstractJavaScriptTokenizer {

	public JSONLDTokenizer() {
		super(false);
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "jsonld");
	}

	@Override
	protected boolean jsonldMode() {
		return true;
	}

	@Override
	protected boolean json() {
		return false;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/ld+json");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("jsonld");
	}
}