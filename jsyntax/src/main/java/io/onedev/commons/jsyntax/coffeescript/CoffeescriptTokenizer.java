package io.onedev.commons.jsyntax.coffeescript;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;


public class CoffeescriptTokenizer extends AbstractTokenizer<CoffeescriptTokenizer.State> {
	static final Pattern operators = Pattern.compile("^(?:->|=>|\\+[+=]?|-[\\-=]?|\\*[\\*=]?|\\/[\\/=]?|[=!]=|<[><]?=?|>>?=?|%=?|&=?|\\|=?|\\^=?|\\~|!|\\?|(or|and|\\|\\||&&|\\?)=)");
	static final Pattern delimiters = Pattern.compile("^(?:[()\\[\\]{},:`=;]|\\.\\.?\\.?)");
	static final Pattern identifiers = Pattern.compile("^[_A-Za-z$][_A-Za-z$0-9]*");
	static final Pattern atProp = Pattern.compile("^@[_A-Za-z$][_A-Za-z$0-9]*");
	static final Pattern stringPrefixes = Pattern.compile("^('{3}|\\\"{3}|['\\\"])");
	static final Pattern regexPrefixes = Pattern.compile("^(\\/{3}|\\/)");
	static final Pattern constants = wordRegexp (Lists.newArrayList("Infinity", "NaN", "undefined", "null", "true", "false", "on", "off", "yes", "no"));
	static final ArrayList<String> commonConstants  = Lists.newArrayList("Infinity", "NaN", "undefined", "null", "true", "false", "on", "off", "yes", "no");
	static final Pattern wordOperators  = wordRegexp (Lists.newArrayList("and", "or", "not","is", "isnt", "in","instanceof","typeof"));
	static final Pattern indentKeywords =wordRegexp( Lists.newArrayList("for", "while", "loop", "if", "unless", "else","switch", "try", "catch", "finally", "class"));
	static final ArrayList<String> commonKeywords  = Lists.newArrayList("break", "by", "continue", "debugger", "delete","do", "in", "of", "new", "return", "then","this", "@", "throw", "when", "until", "extends");
	static final Pattern keywords  = wordRegexp (Lists.newArrayList("for", "while", "loop", "if", "unless", "else","switch", "try", "catch", "finally", "class","break", "by", "continue", "debugger", "delete","do", "in", "of", "new", "return", "then","this", "@", "throw", "when", "until", "extends"));
	 final boolean singleLineStringErrors;
	
	 public CoffeescriptTokenizer() {
		this(false);
	}
	 public CoffeescriptTokenizer(boolean singleLineStringErrors) {
			this.singleLineStringErrors = singleLineStringErrors;
		}
	class State{
		Processor tokenize;
		boolean prop;
		boolean dedent ;
		Scope scope;
		public State(Processor tokenize,boolean prop,boolean dedent ,Scope scope) {
			this.tokenize = tokenize;
			this.prop = prop;
			this.dedent = dedent ;
			this.scope = scope;
		}
	}
	class Scope{
		boolean align;
		int offset;
		String type;
		Scope prev;
		public Scope(boolean align,int offset,String type,Scope prev) {
			this.align =align;
			this.offset = offset;
			this.type = type;
			this.prev = prev;
		}
	}
	
	static interface Processor{
		String process(StringStream stream, State state);
	}
	static Pattern wordRegexp(ArrayList<String> words) {
		return Pattern.compile("^((" + StringUtils.join(words, ")|(") + "))\\b" );
	}
	static final Pattern pattern[] = new Pattern[8];
	static {
	    pattern[0] = Pattern.compile("^-?[0-9\\.]");
	    pattern[1] = Pattern.compile("^-?\\d*\\.\\d+(e[\\+\\-]?\\d+)?", Pattern.CASE_INSENSITIVE);
	    pattern[2] = Pattern.compile("^-?\\d+\\.\\d*");
	    pattern[3] = Pattern.compile("^-?\\.\\d+");
	    pattern[4] = Pattern.compile("^-?0x[0-9a-f]+", Pattern.CASE_INSENSITIVE);
	    pattern[5] = Pattern.compile("^-?[1-9]\\d*(e[\\+\\-]?\\d+)?");
	    pattern[6] = Pattern.compile("^-?0(?![\\dx])", Pattern.CASE_INSENSITIVE);
	    pattern[7] = Pattern.compile("^.*\\/");
	}
	class tokenBase implements Processor{

		@Override
		public String process(StringStream stream, State state) {
			 if (stream.sol()) {
			        if (state.scope.align ) state.scope.align = false;
			        int scopeOffset = state.scope.offset;
			        if (stream.eatSpace()) {
			            int lineOffset = stream.indentation();
			            if (lineOffset > scopeOffset && state.scope.type.equals("coffee")) {
			                return "indent";
			            }
			            else if (lineOffset < scopeOffset) {
			                return "dedent";
			            }
			            return "";
			        }
			        else {
			            if (scopeOffset > 0) {
			                dedent(stream, state);
			            }
			        }
			    }
			    if (stream.eatSpace()) {
			        return "";
			    }
			    String ch = stream.peek();
			    if (stream.match("####")) {
			        stream.skipToEnd();
			        return "comment";
			    }
			    if (stream.match("###")) {
			        state.tokenize = new longComment();
			        return state.tokenize.process(stream, state);
			    }
			    if (ch.equals("#")) {
			        stream.skipToEnd();
			        return "comment";
			    }
			    if (!stream.match(pattern[0],false).isEmpty()) {
			        boolean floatLiteral = false;
			        if (!stream.match(pattern[1]).isEmpty()) {
			            floatLiteral = true;
			        }
			        if (!stream.match(pattern[2]).isEmpty()) {
			            floatLiteral = true;
			        }
			        if (!stream.match(pattern[3]).isEmpty()) {
			            floatLiteral = true;
			        }
			        if (floatLiteral) {
			            if (stream.peek().equals(".")) {
			                stream.backUp(1);
			            }
			            return "number";
			        }
			        boolean intLiteral = false;
			        if (!stream.match(pattern[4]).isEmpty()) {
			            intLiteral = true;
			        }
			        if (!stream.match(pattern[5]).isEmpty()) {
			            intLiteral = true;
			        }
			        if (!stream.match(pattern[6]).isEmpty()) {
			            intLiteral = true;
			        }
			        if (intLiteral) {
			            return "number";
			        }
			    }
			    if (!stream.match(stringPrefixes).isEmpty()) {
			        state.tokenize = new tokenFactory(stream.current(), false, "string");
			        return state.tokenize.process(stream, state);
			    }

			    if (!stream.match(operators).isEmpty() || !stream.match(wordOperators).isEmpty()) {
			        return "operator";
			    }
			    if (!stream.match(regexPrefixes).isEmpty()) {
			        if (stream.current().equals("/") || !stream.match(pattern[7],false).isEmpty()) {
			            state.tokenize = new tokenFactory(stream.current(), true, "string-2");
			            return state.tokenize.process(stream, state);
			        }
			        else {
			            stream.backUp(1);
			        }
			    }
			    if (!stream.match(delimiters).isEmpty()) {
			        return "punctuation";
			    }
			    if (!stream.match(constants).isEmpty()) {
			        return "atom";
			    }
			    if (!stream.match(atProp).isEmpty() || state.prop && !stream.match(identifiers).isEmpty()) {
			        return "property";
			    }
			    if (!stream.match(keywords).isEmpty()) {
			        return "keyword";
			    }
			    if (!stream.match(identifiers).isEmpty()) {
			        return "variable";
			    }
			    stream.next();
			    return "error";
		}
		
	}
	static final Pattern pattern1[] = new Pattern[2];
	static {
	    pattern1[0] = Pattern.compile("[^'\"\\/\\\\]");
	    pattern1[1] = Pattern.compile("['\"\\/]");
	}
	class tokenFactory implements Processor{
		String delimiter;
		boolean singleline;
		String outclass;
		public tokenFactory(String delimiter,boolean singleline,String outclass) {
			this.delimiter = delimiter;
			this.singleline = singleline;
			this.outclass = outclass;
		}
		@Override
		public String process(StringStream stream, State state) {
			 while (!stream.eol()) {
		            stream.eatWhile(pattern1[0]);
		            if (!stream.eat("\\").isEmpty()) {
		                stream.next();
		                if (singleline && stream.eol()) {
		                    return outclass;
		                }
		            }
		            else if (stream.match(delimiter)) {
		                state.tokenize = new tokenBase();
		                return outclass;
		            }
		            else {
		                stream.eat(pattern1[1]);
		            }
		        }
		        if (singleline) {
		            if (singleLineStringErrors) {
		                outclass = "error";
		            }
		            else {
		                state.tokenize = new tokenBase();
		            }
		        }
		        return outclass;
		}
		
	}

	static final Pattern pattern2[] = new Pattern[1];
	static {
	    pattern2[0] = Pattern.compile("[^#]");
	}
	class longComment implements Processor{

		@Override
		public String process(StringStream stream, State state) {
			 while (!stream.eol()) {
			        stream.eatWhile(pattern2[0]);
			        if (stream.match("###")) {
			            state.tokenize = new tokenBase();
			            break;
			        }
			        stream.eatWhile("#");
			    }
			    return "comment";
		}
		
	}
	 public void indent(StringStream stream, State state, String type) {
	   
		 type =! type.isEmpty()? type: "coffee";
	    int offset = 0;
	    boolean align = false;
	    for (Scope scope = state.scope; scope!=null; scope = scope.prev) {
	        if (scope.type.equals("coffee") || scope.type.equals("}")) {
	            offset = scope.offset ;//+ conf.indentUnit;
	            break;
	        }
	    }
	    if (type !=  "coffee") {
	        align = false;
	    }
	    else if (state.scope.align ) {
	        state.scope.align =false;
	    }
	    state.scope.offset = offset;
	    state.scope.align = align;

	 }

	public boolean dedent(StringStream stream, State state) {
		 if (state.scope.prev==null) return false;
		    if (state.scope.type.equals("coffee")) {
		        int _indent = stream.indentation();
		        boolean matched = false;
		        for (Scope scope = state.scope;scope!=null;scope = scope.prev) {
		            if (_indent == scope.offset) {
		                matched = true;
		                break;
		            }
		        }
		        if (!matched) {
		            return true;
		        }
		        while (state.scope.prev!=null && state.scope.offset != _indent) {
		            state.scope = state.scope.prev;
		        }
		        return false;
		    }
		    else {
		        state.scope = state.scope.prev;
		        return false;
		    }
	}
	public String tokenLexer(StringStream stream, State state) {
		   String style = state.tokenize.process(stream, state);
		    String current = stream.current();
		    if (current.equals("return")) {
		        state.dedent = true;
		    }
		    if (((current.equals("->") || current.equals("=>")) && stream.eol()) || style.equals("indent")) {
		        indent(stream, state,"");
		    }
		    int delimiter_index = "[({".indexOf(current);
		    if (delimiter_index != -1) {
		        indent(stream, state, "])}".substring(delimiter_index, delimiter_index +1));
		    }
		    if (indentKeywords.matcher(current).matches()) {
		        indent(stream, state,"");
		    }
		    if (current.equals("then")) {
		        dedent(stream, state);
		    }
		    if (style.equals("dedent")) {
		        if (dedent(stream, state)) {
		            return "error";
		        }
		    }
		    delimiter_index = "])}".indexOf(current);
		    if (delimiter_index != -1) {
		        while (state.scope.type.equals("coffee") && state.scope.prev!=null)
		        	state.scope = state.scope.prev;
		        if (state.scope.type.equals(current)) state.scope = state.scope.prev;
		    }
		    if (state.dedent && stream.eol()) {
		        if (state.scope.type.equals("coffee") && state.scope.prev!=null)
		        	state.scope = state.scope.prev;
		        state.dedent = false;
		    }
		    return style;
	}
	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "coffee");
	}

	@Override
	public boolean acceptMime(String mime) {
		// TODO Auto-generated method stub
		return mime != null && mime.equals("text/x-coffeescript");
	}

	@Override
	public boolean acceptMode(String mode) {
		// TODO Auto-generated method stub
		return mode != null && mode.equals("coffeescript");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(new tokenBase(), false, false, new Scope(false, 0, "coffee", null));
	}

	@Override
	public String token(StringStream stream, State state) {
		 Scope fillAlign = state.scope.align ? state.scope:null;
		    if (fillAlign!=null && stream.sol()) fillAlign.align = false;
		    String style = tokenLexer(stream, state);
		    if (!style.isEmpty() && !style.equals("comment")) {
		        if (fillAlign!=null)
		        	fillAlign.align = true;
		        state.prop = style.equals("punctuation") && stream.current().equals(".") ;
		        } 
		    return style;
	}

}
