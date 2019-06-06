package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.StringStream;

public class KotlinTokenizer extends ClikeTokenizer {

	private static final Set<String> KEYWORDS = wordsOf(
			/*keywords*/
			"package as typealias class interface this super val " +
    	    "var fun for is in This throw return " +
    	    "break continue object if else while do try when !in !is as? " +

    	    /*soft keywords*/
    	    "file import where by get set abstract enum open inner override private public internal " +
    	    "protected catch finally out final vararg reified dynamic companion constructor init " +
    	    "sealed field property receiver param sparam lateinit data inline noinline tailrec " +
    	    "external annotation crossinline const operator infix");
	
	private static final Set<String> TYPES = wordsOf(
			/* package java.lang */
    	    "Boolean Byte Character CharSequence Class ClassLoader Cloneable Comparable " +
    	    "Compiler Double Exception Float Integer Long Math Number Object Package Pair Process " +
    	    "Runtime Runnable SecurityManager Short StackTraceElement StrictMath String " +
    	    "StringBuffer System Thread ThreadGroup ThreadLocal Throwable Triple Void");
	
	private static final Set<String> BLOCK_KEYWORDS = wordsOf("catch class do else finally for if where try while enum");
	
	private static final Set<String> DEF_KEYWORDS = wordsOf("class val var object package interface fun");
	
	private static final Set<String> ATOMS = wordsOf("true false null this");
	
	private static Map<String, Processor> HOOKS = Maps.newHashMap();

	static {
		HOOKS.put("\"", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
				state.tokenize = tokenKotlinString(stream.match("\"\""));
    	        return state.tokenize.process(stream, state, style);
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
	protected boolean indentSwitch() {
		return false;
	}

	@Override
	protected boolean indentStatements() {
		return false;
	}

	@Override
	protected boolean multiLineStrings() {
		return true;
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
	protected Set<String> atoms() {
		return ATOMS;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "kt");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-kotlin");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("kotlin");
	}
}