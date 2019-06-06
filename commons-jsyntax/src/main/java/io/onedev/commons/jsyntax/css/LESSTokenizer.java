package io.onedev.commons.jsyntax.css;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.StringStream;

public class LESSTokenizer extends AbstractCssTokenizer {

	@Override
	protected Set<String> mediaTypes() {
		return mediaTypes;
	}

	@Override
	protected Set<String> mediaFeatures() {
		return mediaFeatures;
	}

	@Override
	protected Set<String> mediaValueKeywords() {
		return mediaValueKeywords;
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
	protected Set<String> colorKeywords() {
		return colorKeywords;
	}

	@Override
	protected Set<String> valueKeywords() {
		return valueKeywords;
	}

	@Override
	protected Set<String> fontProperties() {
		return fontProperties;
	}

	@Override
	protected boolean allowNested() {
		return true;
	}

	private static final Map<String, Processor> TOKEN_HOOKS = new HashMap<>();
	
	private static Pattern PATTERN1 = Pattern.compile("^(charset|document|font-face|import|(-(moz|ms|o|webkit)-)?keyframes|media|namespace|page|supports)\\b");
	
	private static Pattern PATTERN2 = Pattern.compile("^[\\w\\\\\\-]");
	
	private static Pattern PATTERN3 = Pattern.compile("^\\s*:");
	
	static {
		TOKEN_HOOKS.put("/", new Processor() {

			@Override
			public String process(StringStream stream, State state) {
				if (stream.eat("/").length() != 0) {
					stream.skipToEnd();
		            return "comment comment";
				} else if (stream.eat("*").length() != 0) {
		            state.tokenize = new TokenCComment();
		            return state.tokenize.process(stream, state);
				} else {
		            return "operator operator";
				}
			}
			
		});
		TOKEN_HOOKS.put("@", new Processor() {

			@Override
			public String process(StringStream stream, State state) {
		        if (stream.eat("{").length() != 0) return " interpolation";
		        if (!stream.match(PATTERN1, false).isEmpty()) 
		        	return "";
		        stream.eatWhile(PATTERN2);
		        if (!stream.match(PATTERN3, false).isEmpty())
		        	return "variable-2 variable-definition";
		        return "variable-2 variable";
			}
			
		});
		TOKEN_HOOKS.put("&", new Processor() {

			@Override
			public String process(StringStream stream, State state) {
		        return "atom atom";
			}
			
		});
	}
	
	@Override
	protected Map<String, Processor> tokenHooks() {
		return TOKEN_HOOKS;
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "less");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-less");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("less");
	}
}