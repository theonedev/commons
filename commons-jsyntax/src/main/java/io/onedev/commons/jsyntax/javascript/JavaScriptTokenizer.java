package io.onedev.commons.jsyntax.javascript;

public class JavaScriptTokenizer extends AbstractJavaScriptTokenizer {

	public JavaScriptTokenizer() {
		super(false);
	}

	@Override
	public boolean accept(String fileName) {
		// Always use jsx tokenizer for .js files as jsx is a superset of js
		return false; 
	}

	@Override
	protected boolean jsonldMode() {
		return false;
	}

	@Override
	protected boolean json() {
		return false;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null 
				&& (mime.equals("text/javascript")
				|| mime.equals("text/ecmascript")
				|| mime.equals("application/javascript")
				|| mime.equals("application/x-javascript")
				|| mime.equals("application/ecmascript"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("javascript");
	}
	
}