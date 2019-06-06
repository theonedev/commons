package io.onedev.commons.jsyntax.clike;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.StringStream;

public class ScalaTokenizer extends ClikeTokenizer {

	private static Set<String> KEYWORDS = wordsOf(
			/* scala */
			"abstract case catch class def do else extends final finally for forSome if "
			+ "implicit import lazy match new null object override package private protected return "
			+ "sealed super this throw trait try type val var while with yield _ : = => <- <: "
			+ "<% >: # @ "
			+

			/* package scala */
			"assert assume require print println printf readLine readBoolean readByte readShort "
			+ "readChar readInt readLong readFloat readDouble "
			+ ":: #:: ");

	private static Set<String> TYPES = wordsOf(
			"AnyVal App Application Array BufferedIterator BigDecimal BigInt Char Console Either "
			+ "Enumeration Equiv Error Exception Fractional Function IndexedSeq Int Integral Iterable "
			+ "Iterator List Map Numeric Nil NotNull Option Ordered Ordering PartialFunction PartialOrdering "
			+ "Product Proxy Range Responder Seq Serializable Set Specializable Stream StringBuilder "
			+ "StringContext Symbol Throwable Traversable TraversableOnce Tuple Unit Vector "
			+

			/* package java.lang */
			"Boolean Byte Character CharSequence Class ClassLoader Cloneable Comparable "
			+ "Compiler Double Exception Float Integer Long Math Number Object Package Pair Process "
			+ "Runtime Runnable SecurityManager Short StackTraceElement StrictMath String "
			+ "StringBuffer System Thread ThreadGroup ThreadLocal Throwable Triple Void");

	private static Set<String> BLOCK_KEYWORDS = wordsOf("catch class do else finally for forSome if match switch try while");
	
	private static Set<String> DEF_KEYWORDS = wordsOf("class def object package trait type val var");

	private static Set<String> ATOMS = Sets.newHashSet("true", "false", "null");

	private static Map<String, Processor> HOOKS = Maps.newHashMap();

	private static Pattern PATTERN1 = Pattern.compile("[\\w\\$_]");
	
	private static Pattern PATTERN2 = Pattern.compile("[\\w\\$_\\xa1-\\uffff]");

	static {
		HOOKS.put("@", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
				stream.eatWhile(PATTERN1);
				return "meta";
			}

		});
		HOOKS.put("\"", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		        if (!stream.match("\"\"")) 
		        	return "";
		        state.tokenize = new TokenTripleString();
		        return state.tokenize.process(stream, state, style);
			}
			
		});
		HOOKS.put("'", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		        stream.eatWhile(PATTERN2);
		        return "atom";
			}
			
		});
		HOOKS.put("=", new Processor() {

			@Override
			public String process(StringStream stream, State state, String style) {
		        Context cx = state.context;
		        if (cx.type.equals("}") && cx.align!=null&&cx.align && stream.eat(">").length() != 0) {
		        	state.context = new Context(cx.indented, cx.column, cx.type, cx.info, null, cx.prev);
		    	    return "operator";
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
	protected boolean indentStatements() {
		return false;
	}

	@Override
	protected boolean indentSwitch() {
		return false;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return HOOKS;
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "scala");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-scala");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("scala");
	}
}