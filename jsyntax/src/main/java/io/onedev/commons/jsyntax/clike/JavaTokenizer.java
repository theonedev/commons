package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.StringStream;

public class JavaTokenizer extends ClikeTokenizer {

	private static Set<String> KEYWORDS = Sets.newHashSet("abstract",
			"assert", "break", "case", "catch",
			"class", "const", "continue", "default", "do", 
			"else", "enum", "extends", "final", "finally", "float", "for",
			"goto", "if", "implements", "import", "instanceof", 
			"interface", "native", "new", "package", "private",
			"protected", "public", "return", "static", "strictfp",
			"super", "switch", "synchronized", "this", "throw", "throws",
			"transient", "try", "volatile", "while");
	
	private static Set<String> TYPES = Sets.newHashSet("byte", "short", "int", "long", "float", "double",
			"boolean", "char", "void", "Boolean", "Byte", "Character", "Double", "Float", "Integer", 
			"Long", "Number", "Object", "Short", "String", "StringBuffer", "StringBuilder", "Void");

	private static Set<String> BLOCK_KEYWORDS = Sets.newHashSet("catch",
			"class", "do", "else", "finally", "for", "if", "switch", "try",
			"while");
	
	private static Set<String> DEF_KEYWORDS = Sets.newHashSet("class", "interface", "package", "enum");

	private static Set<String> ATOMS = Sets.newHashSet("true", "false", "null");
	
	private static final Pattern NUMBER = Pattern.compile("^(?:0x[a-f\\d_]+|0b[01_]+|(?:[\\d_]+\\.?\\d*|\\.\\d+)(?:e[-+]?[\\d_]+)?)(u|ll?|l|f)?", Pattern.CASE_INSENSITIVE);

	private static Map<String, Processor> HOOKS = Maps.newHashMap();

	private static Pattern PATTERN = Pattern.compile("[\\w\\$_]");

	static {
		HOOKS.put("@", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
				stream.eatWhile(PATTERN);
				return "meta";
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
	protected Pattern number() {
		return NUMBER;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "java");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-java");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("java");
	}
}