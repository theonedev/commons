package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class NescTokenizer extends ClikeTokenizer {

	private static final Set<String> KEYWORDS = wordsOf(C_KEYWORDS,
			"as atomic async call command component components configuration event generic " +
            "implementation includes interface module new norace nx_struct nx_union post provides " +
            "signal task uses abstract extends");
	
	private static final Set<String> BLOCK_KEYWORDS = wordsOf("case do else for if switch while struct");
	
	private static final Set<String> ATOMS = wordsOf("null true false");
	
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
	protected Set<String> atoms() {
		return ATOMS;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "nesc");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-nesc");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("nesc");
	}
}