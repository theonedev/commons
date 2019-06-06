package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.StringStream;

public class CppTokenizer extends ClikeTokenizer {

	private static Set<String> KEYWORDS = Sets.newHashSet("asm",
			"dynamic_cast", "namespace", "reinterpret_cast", "try",
			"explicit", "new", "static_cast", "typeid", "catch",
			"operator", "template", "typename", "class", "friend",
			"private", "this", "using", "const_cast", "inline", "public",
			"throw", "virtual", "delete", "mutable", "protected",
			"alignas", "alignof", "constexpr", "decltype",
			"nullptr", "noexcept", "thread_local", "final",
			"static_assert", "override");

	static {
		KEYWORDS.addAll(C_KEYWORDS);
	}
	
	private static Set<String> TYPES = Sets.newHashSet("bool", "wchar_t");
	
	static {
		TYPES.addAll(C_TYPES);
	}

	private static Set<String> BLOCK_KEYWORDS = Sets.newHashSet("catch",
			"class", "do", "else", "finally", "for", "if", "struct",
			"switch", "try", "while");
	
	private static Set<String> DEF_KEYWORDS = Sets.newHashSet("class", "namespace", "struct", "enum", "union");

	private static Set<String> ATOMS = Sets.newHashSet("true", "false", "null");

	private static Map<String, Processor> HOOKS = Maps.newHashMap();

	static {
		HOOKS.put("#", new CppHook());
		HOOKS.put("*", new PointerHook());
		HOOKS.put("u", new Cpp11StringHook());
		HOOKS.put("U", new Cpp11StringHook());
		HOOKS.put("L", new Cpp11StringHook());
		HOOKS.put("R", new Cpp11StringHook());
		HOOKS.put("0", new Cpp14Literal());
		HOOKS.put("1", new Cpp14Literal());
		HOOKS.put("2", new Cpp14Literal());
		HOOKS.put("3", new Cpp14Literal());
		HOOKS.put("4", new Cpp14Literal());
		HOOKS.put("5", new Cpp14Literal());
		HOOKS.put("6", new Cpp14Literal());
		HOOKS.put("7", new Cpp14Literal());
		HOOKS.put("8", new Cpp14Literal());
		HOOKS.put("9", new Cpp14Literal());
		HOOKS.put("token", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		        if (style.equals("variable") && stream.peek().equals("(") &&
			            (state.prevToken.equals(";") || state.prevToken.length() == 0 ||
			             state.prevToken.equals("}")) && cppLooksLikeConstructor(stream.current())) {
		        	return "def";
		        } else {
		        	return "";
		        }
			}
			
		});
	}

	@Override
	protected Set<String> keywords() {
		return KEYWORDS;
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
	protected String namespaceSeparator() {
		return "::";
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "cpp", "c++", "cc", "cxx", "hpp",
				"h++", "hh", "hxx");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-c++src");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("cpp");
	}
}