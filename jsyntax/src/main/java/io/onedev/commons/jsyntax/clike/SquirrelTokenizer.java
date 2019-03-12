package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class SquirrelTokenizer extends ClikeTokenizer {

	private static Set<String> KEYWORDS = wordsOf(
			"base break clone continue const default delete enum extends function in class " +
            "foreach local resume return this throw typeof yield constructor instanceof static");

	private static Set<String> BLOCK_KEYWORDS = wordsOf("case catch class else for foreach if switch try while");
	
	private static Set<String> DEF_KEYWORDS = wordsOf("function local class");
	
	private static Set<String> ATOMS = wordsOf("true false null");

	private static Map<String, Processor> HOOKS = Maps.newHashMap();

	static {
		HOOKS.put("#", new CppHook());
	}

	@Override
	protected Set<String> keywords() {
		return KEYWORDS;
	}

	@Override
	protected boolean typesContains(String word) {
		return C_TYPES.contains(word);
	}

	@Override
	protected Set<String> blockKeywords() {
		return BLOCK_KEYWORDS;
	}

	@Override
	protected Set<String> defKeywords() {
		return DEF_KEYWORDS;
	}

	@Override
	protected boolean typeFirstDefinitions() {
		return true;
	}

	@Override
	protected Set<String> atoms() {
		return ATOMS;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "nut");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-squirrel");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("squirrel");
	}
}