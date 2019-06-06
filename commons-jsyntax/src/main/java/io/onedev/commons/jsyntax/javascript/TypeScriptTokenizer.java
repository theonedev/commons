package io.onedev.commons.jsyntax.javascript;

public class TypeScriptTokenizer extends AbstractJavaScriptTokenizer {

	public TypeScriptTokenizer() {
		super(true);
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "ts");
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
		return mime != null && mime.equals("application/typescript");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("typescript");
	}
}