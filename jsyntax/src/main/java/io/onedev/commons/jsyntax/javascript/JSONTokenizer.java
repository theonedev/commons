package io.onedev.commons.jsyntax.javascript;

public class JSONTokenizer extends AbstractJavaScriptTokenizer {

	public JSONTokenizer() {
		super(false);
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "json", "map");
	}

	@Override
	protected boolean jsonldMode() {
		return false;
	}

	@Override
	protected boolean json() {
		return true;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null 
				&& (mime.equals("application/json")
				|| mime.equals("application/x-json"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("json");
	}
}