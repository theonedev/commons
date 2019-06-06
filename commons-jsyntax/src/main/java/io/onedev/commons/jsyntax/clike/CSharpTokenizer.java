package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.StringStream;

public class CSharpTokenizer extends ClikeTokenizer {

	private static Set<String> KEYWORDS = wordsOf("abstract as async await base break case catch checked class const continue"
			+ " default delegate do else enum event explicit extern finally fixed for"
			+ " foreach goto if implicit in interface internal is lock namespace new"
			+ " operator out override params private protected public readonly ref return sealed"
			+ " sizeof stackalloc static struct switch this throw try typeof unchecked"
			+ " unsafe using virtual void volatile while add alias ascending descending dynamic from get"
			+ " global group into join let orderby partial remove select set value var yield");

	private static Set<String> TYPES = wordsOf("Action Boolean Byte Char DateTime DateTimeOffset Decimal Double"
			+ " Func Guid Int16 Int32 Int64 Object SByte Single String Task TimeSpan UInt16 UInt32"
			+ " UInt64 bool byte char decimal double short int long object"
			+ " sbyte float string ushort uint ulong");
	
	private static Set<String> BLOCK_KEYWORDS = wordsOf("catch class do else finally for foreach if struct switch try while");
	
	private static Set<String> DEF_KEYWORDS = wordsOf("class interface namespace struct var");

	private static Set<String> ATOMS = Sets.newHashSet("true", "false", "null");

	private static Map<String, Processor> HOOKS = Maps.newHashMap();

	private static Pattern PATTERN = Pattern.compile("[\\w\\$_]");

	static {
		HOOKS.put("@", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
				if (stream.eat("\"").length() != 0) {
					state.tokenize = new TokenAtString();
					return state.tokenize.process(stream, state, style);
				}
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
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "cs");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-csharp");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("csharp");
	}
}