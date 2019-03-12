package io.onedev.commons.jsyntax.css;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.onedev.commons.jsyntax.StringStream;

public class GSSTokenizer extends AbstractCssTokenizer {

	@Override
	protected Set<String> documentTypes() {
		return documentTypes;
	}

	@Override
	protected Set<String> mediaTypes() {
		return mediaTypes;
	}

	@Override
	protected Set<String> mediaFeatures() {
		return mediaFeatures;
	}

	@Override
	protected Set<String> propertyKeywords() {
		return propertyKeywords;
	}

	@Override
	protected Set<String> nonStandardPropertyKeywords() {
		return nonStandardPropertyKeywords;
	}

	@Override
	protected Set<String> fontProperties() {
		return fontProperties;
	}

	@Override
	protected Set<String> counterDescriptors() {
		return counterDescriptors;
	}

	@Override
	protected Set<String> colorKeywords() {
		return colorKeywords;
	}

	@Override
	protected Set<String> valueKeywords() {
		return valueKeywords;
	}

	@Override
	protected boolean supportsAtComponent() {
		return true;
	}

	private static final Map<String, Processor> TOKEN_HOOKS = new HashMap<>();
	
	static {
		TOKEN_HOOKS.put("/", new Processor() {

			@Override
			public String process(StringStream stream, State state) {
		        if (stream.eat("*").length() == 0) return "";
		        state.tokenize = new TokenCComment();
		        return state.tokenize.process(stream, state);
			}
			
		});
	}
	
	@Override
	protected Map<String, Processor> tokenHooks() {
		return TOKEN_HOOKS;
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "gss");
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-gss");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("gss");
	}
}