package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.StringStream;

public class CeylonTokenizer extends ClikeTokenizer {

	private static final Set<String> KEYWORDS = wordsOf(
			"abstracts alias assembly assert assign break case catch class continue dynamic else" +
            " exists extends finally for function given if import in interface is let module new" +
            " nonempty object of out outer package return satisfies super switch then this throw" +
            " try value void while");

	private static final Set<String> BLOCK_KEYWORDS = wordsOf("case catch class dynamic else finally for "
			+ "function if interface module new object switch try while");
	
	private static final Set<String> DEF_KEYWORDS = wordsOf("class dynamic function interface module object "
			+ "package value");
	
	private static final Set<String> BUILTIN = wordsOf("abstract actual aliased annotation by default "
			+ "deprecated doc final formal late license native optional sealed see serializable "
			+ "shared suppressWarnings tagged throws variable");
	
	private static final Pattern IS_PUNCTUATION_CHAR = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.`]");
	
	private static final Pattern IS_OPERATOR_CHAR = Pattern.compile("[+\\-*&%=<>!?|^~:\\/]");
	
	private static final Pattern NUMBER_START = Pattern.compile("[\\d#$]");
	
	private static final Pattern NUMBER = Pattern.compile(
			"^(?:#[\\da-fA-F_]+|\\$[01_]+|[\\d_]+[kMGTPmunpf]?|[\\d_]+\\.[\\d_]+(?:[eE][-+]?\\d+|[kMGTPmunpf]|)|)", Pattern.CASE_INSENSITIVE);
	
	private static final Set<String> ATOMS = wordsOf("true false null larger smaller equal empty finished");

	private static final Map<String, Processor> HOOKS = Maps.newHashMap();
	
	private static final Pattern HOOK_PATTERN1 = Pattern.compile("[\\w\\$_]");
	
	private static final Pattern HOOK_PATTERN2 = Pattern.compile("[\\w\\$_\\xa1-\\uffff]");

	static {
		HOOKS.put("@", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
				stream.eatWhile(HOOK_PATTERN1);
				return "meta";
			}
			
		});
		HOOKS.put("\"", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		          state.tokenize = tokenCeylonString(stream.match("\"\"")? "triple":"single");
		          return state.tokenize.process(stream, state, style);
			}
			
		});
		HOOKS.put("`", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		          if (state.stringTokenizer == null || !stream.match("`")) return "";
		          state.tokenize = state.stringTokenizer;
		          state.stringTokenizer = null;
		          return state.tokenize.process(stream, state, style);
			}
			
		});
		HOOKS.put("'", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		        stream.eatWhile(HOOK_PATTERN2);
		        return "atom";
			}
			
		});
		HOOKS.put("token", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		          if ((style.equals("variable") || style.equals("variable-3")) && state.prevToken.equals(".")) {
		        	  return "variable-2";
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
        char first = word.charAt(0);
        return (first == Character.toUpperCase(first) && first != Character.toLowerCase(first));
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
	protected Set<String> builtin() {
		return BUILTIN;
	}

	@Override
	protected Pattern isPunctuationChar() {
		return IS_PUNCTUATION_CHAR;
	}

	@Override
	protected Pattern isOperatorChar() {
		return IS_OPERATOR_CHAR;
	}

	@Override
	protected Pattern numberStart() {
		return NUMBER_START;
	}

	@Override
	protected Pattern number() {
		return NUMBER;
	}

	@Override
	protected boolean multiLineStrings() {
		return true;
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
	protected boolean indentSwitch() {
		return false;
	}

	@Override
	protected boolean styleDefs() {
		return false;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "ceylon");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-ceylon");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("ceylon");
	}
}