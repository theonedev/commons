package io.onedev.commons.jsyntax.dtd;

import java.util.Stack;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class DtdTokenizer extends AbstractTokenizer<DtdTokenizer.State>{

	static interface Processor{
		String process(StringStream stream,State state);
	}
	
	static class State{
		Processor tokenize;
//		boolean baseIndent;
		Stack<String> stack;
		String type;
		
		public State(Processor tokenize,Stack<String> stack,String type) {
			this.tokenize = tokenize;
//			this.baseIndent = (base!=0)?true:false;
			this.stack = stack;
			this.type = type;
		}
	}
	
	static String ret(State state,String style, String tp) {
		state.type = tp;
	    return style;
	}
	

	
	static final Pattern pattern[] = new Pattern[8];
	static {
	    pattern[0] = Pattern.compile("[\\-]");
	    pattern[1] = Pattern.compile("[\\w]");
	    pattern[2] = Pattern.compile("[\\w]");
	    pattern[3] = Pattern.compile("[\\(\\)\\[\\]\\-\\.,\\+\\?>]");
	    pattern[4] = Pattern.compile("[\\[\\]]");
	    pattern[5] = Pattern.compile("[a-zA-Z\\?\\+\\d]");
	    pattern[6] = Pattern.compile("\\?|\\+");
	    pattern[7] = Pattern.compile("[\\w\\\\\\-_%.{,]");
	}
	
	
	class tokenBase implements Processor{

		@Override
		public String process(StringStream stream, State state) {
		    String ch = stream.next();
		    
		    if (ch.equals("<") &&(!stream.eat("!").isEmpty())) {
		        if (stream.eatWhile(pattern[0])) {
		            state.tokenize = new tokenSGMLComment();
		            return state.tokenize.process(stream, state); //?
//		            return new tokenSGMLComment().process(stream, state); //?
//		            return "";
		        }
		        else if (stream.eatWhile(pattern[1])) 
		        	return ret(state,"keyword", "doindent");
		    }
		    else if (ch.equals("<") &&!stream.eat("?").isEmpty()) {
		        state.tokenize = new inBlock("meta", "?>");
		        return ret(state,"meta", ch);
		    }
		    else if (ch.equals("#") && stream.eatWhile(pattern[2])) return ret(state,"atom", "tag");
		    else if (ch.equals("|")) return ret(state,"keyword", "seperator");
		    else if (ch.matches(pattern[3].toString())) return ret(state,"" , ch);
		    else if (ch.matches(pattern[4].toString())) return ret(state,"rule", ch);
		    else if (ch.equals("\"") || ch.equals("'")) {
		        state.tokenize = new tokenString(ch);
		        return state.tokenize.process(stream, state);
		    }
		    else if (stream.eatWhile(pattern[5])) {
		        String sc = stream.current();
		        if (sc.substring(sc.length() -1, sc.length()).matches(pattern[6].toString()) !=  false ) 
		        	stream.backUp(1);
		        return ret(state,"tag", "tag");
		    }
		    else if (ch.equals("%") || ch.equals("*")) return ret(state,"number", "number");
		    else {
		        stream.eatWhile(pattern[7]);
		        return ret(state,"" , "" );
		    }
			return "";
		}	
	}
	
	
	class tokenSGMLComment implements Processor{

		@Override
		public String process(StringStream stream, State state) {
			 int dashes = 0;
			 String ch;
//			    while ((ch = stream.next()) != null ) { 
			 while (!(ch = stream.next()).isEmpty()) {
			        if (dashes >= 2 && ch.equals(">")) {
			            state.tokenize = new tokenBase();
			            break ;
			        }
			        dashes =(ch.equals("-")) ? dashes + 1 : 0;
			    }
			    return ret(state,"comment", "comment");
		}	
	}
	
	
	class tokenString implements Processor{
		String quote;
		
		public tokenString(String quote) {
			this.quote = quote;
		}
		
		@Override
		public String process(StringStream stream, State state) {
		    boolean escaped = false;
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (ch.equals(quote) && !escaped) {
		            state.tokenize = new tokenBase();
		            break ;
		        }
		        escaped = !escaped && ch.equals("\\");
		    }
		    return ret(state,"string", "tag");
		}
		
	}
	
	
	class inBlock implements Processor{
		String style, terminator;
		
		public inBlock(String style,String terminator) {
			this.style = style;
			this.terminator = terminator;
		}
		
		@Override
		public String process(StringStream stream, State state) {
			 while (!stream.eol()) {
				 if (stream.match(terminator)) {
					 state.tokenize = new tokenBase();
		             break ;
		         }
		         stream.next();
			 }
		     return style;
		}
		
	}
	

		
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName,"dtd");
	}


	@Override
	public State startState() {
		return new State(new tokenBase(),new Stack<String>(),"");
	}

	@Override
	public String token(StringStream stream, State state) {
		String context = "";
	    if (stream.eatSpace()) return "" ;
	    String style = state.tokenize.process(stream, state);
	    if(!state.stack.empty()) {
		    context = state.stack.get(state.stack.size() -1); //?
	    }

//	    String context = state.stack.get((state.stack.size()));
	    if(!state.stack.empty()) {
		    if (stream.current().equals("[") || state.type.equals("doindent") || state.type.equals("[")) state.stack.push("rule");
		    else if (state.type.equals("endtag")) state.stack.set(state.stack.size() -1, "endtag"); //?
		    else if (stream.current().equals("]") || state.type.equals("]") ||(state.type.equals(">") && context.equals("rule"))) state.stack.pop();
		    else if (state.type.equals("[")) state.stack.push("[");
	    }

	    return style;
	    
	}

	String electricChars = "]>";
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/xml-dtd");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("dtd");
	}

}
