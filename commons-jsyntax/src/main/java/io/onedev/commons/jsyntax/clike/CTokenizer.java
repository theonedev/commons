package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CTokenizer extends ClikeTokenizer {

	private static Set<String> TYPES = Sets.newHashSet("bool", "_Complex", "_Bool", "float_t", "double_t", "intptr_t", 
			"intmax_t", "int8_t", "int16_t", "int32_t", "int64_t", "uintptr_t", "uintmax_t", 
			"uint8_t", "uint16_t", "uint32_t", "uint64_t");
	
	static {
		TYPES.addAll(C_TYPES);
	}

	private static Set<String> BLOCK_KEYWORDS = Sets.newHashSet("case", "do", "else", "for", "if", "switch", "while", "struct");
	
	private static Set<String> DEF_KEYWORDS = Sets.newHashSet("struct");

	private static Set<String> ATOMS = Sets.newHashSet("null", "true", "false");

	private static Map<String, Processor> HOOKS = Maps.newHashMap();
	
	static {
		HOOKS.put("#", new CppHook());
		HOOKS.put("*", new PointerHook());
	}

	@Override
	protected Set<String> keywords() {
		return C_KEYWORDS;
	}

	@Override
	protected boolean typesContains(String word) {
		return TYPES.contains(word);
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
		return acceptExtensions(fileName, "c", "h");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-csrc");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("c");
	}
}