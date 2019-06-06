package io.onedev.commons.jsyntax.ecl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;


public class EclTokenzier extends AbstractTokenizer<EclTokenzier.State> {

	static final Set<String> keyword = words("abs acos allnodes ascii asin asstring atan atan2 ave case choose choosen choosesets clustersize combine correlation cos cosh count covariance cron dataset dedup define denormalize distribute distributed distribution ebcdic enth error evaluate event eventextra eventname exists exp failcode failmessage fetch fromunicode getisvalid global graph group hash hash32 hash64 hashcrc hashmd5 having if index intformat isvalid iterate join keyunicode length library limit ln local log loop map matched matchlength matchposition matchtext matchunicode max merge mergejoin min nolocal nonempty normalize parse pipe power preload process project pull random range rank ranked realformat recordof regexfind regexreplace regroup rejected rollup round roundup row rowdiff sample set sin sinh sizeof soapcall sort sorted sqrt stepped stored sum table tan tanh thisnode topn tounicode transfer trim truncate typeof ungroup unicodeorder variance which workunit xmldecode xmlencode xmltext xmlunicode");
	static final Set<String> variable = words("apply assert build buildindex evaluate fail keydiff keypatch loadxml nothor notify output parallel sequential soapcall wait");
	static final Set<String> variable_2 = words("__compressed__ all and any as atmost before beginc++ best between case const counter csv descend encrypt end endc++ endmacro except exclusive expire export extend false few first flat from full function group header heading hole ifblock import in interface joined keep keyed last left limit load local locale lookup macro many maxcount maxlength min skew module named nocase noroot noscan nosort not of only opt or outer overwrite packed partition penalty physicallength pipe quote record relationship repeat return right scan self separator service shared skew skip sql store terminator thor threshold token transform trim true type unicodeorder unsorted validate virtual whole wild within xml xpath ");
	static final Set<String> variable_3 = words("ascii big_endian boolean data decimal ebcdic integer pattern qstring real record rule set of string token udecimal unicode unsigned varstring varunicode");
	static final Set<String> builtin = words("checkpoint deprecated failcode failmessage failure global independent onwarning persist priority recovery stored success wait when");
	static final Set<String> blockKeywords = words("catch class do else finally for if switch try while");
	static final Set<String> atoms = words("true false null");
	static final Pattern isOperatorChar = Pattern.compile("[+\\-*&%=<>!?|\\/]");
	
	static final Map<String,Processor> hooks = new HashMap<String,Processor>();
	static {
		hooks.put("#",new  metaHook() );
	}
	class State{
		Processor tokenize;
		boolean startOfLine;
		Context context;
		String curPunc;
		public State(Processor tokenize,boolean startOfLine,Context context,String curPunc) {
			this.tokenize = tokenize;
			this.startOfLine = startOfLine;
			this.context = context;
			this.curPunc = curPunc;
		}
	}
	class Context{
		String type;
		boolean align;
		Context prev;
		public Context(String type,boolean align,Context prev) {
			this.type = type;
			this.align = align;
			this.prev = prev;
		}
	}
	
	static interface Processor{
		String processor(StringStream stream,State state);
	}
	
	static Set<String> words(String str) {
		Set<String> s = Sets.newHashSet();
		String[] arr = str.split(" ");
		for (String o : arr) {
			s.add(o);
		}
		return s;
	}
	
	static class metaHook implements Processor {

		@Override
		public String processor(StringStream stream, State state) {
		    if (!state.startOfLine) return "";
		    stream.skipToEnd();
		    return "meta";
		}
	}

	public static boolean isNumeric(String str){
		  for (int i = str.length();--i>=0;){   
			  if (!Character.isDigit(str.charAt(i))){
				  return false;
			  }
		  }
		  return true;
	 }
	
	static final Pattern pattern[] = new Pattern[4];
	static {
	    pattern[0] = Pattern.compile("[\\[\\]{}\\(\\),;\\:\\.]");
	    pattern[1] = Pattern.compile("\\d");
	    pattern[2] = Pattern.compile("[\\w\\.]");
	    pattern[3] = Pattern.compile("[\\w\\$_]");
	}
	class tokenBase implements Processor{
	
		@Override
		public String processor(StringStream stream, State state) {
			   String ch=stream.next() ;
			  if (hooks.get(ch)!=null) {
			      String result = hooks.get(ch).processor(stream, state);;
			       if (!result.equals(""))
			    	   return result;
			   }
			    if (ch.equals("\"") || ch.equals("'")) {
			        state.tokenize =new tokenString(ch);
			        return state.tokenize.processor(stream, state);
			    }
			    if (pattern[0].matcher(ch).matches()) {
			       state.curPunc = ch;
			        return "";
			    }
			    if (pattern[1].matcher(ch).matches()) {
			        stream.eatWhile(pattern[2]);
			        return "number";
			    }
			    if (ch.equals("/")) {
			        if (!stream.eat("*").isEmpty()) {	
			            state.tokenize = new tokenComment();
			            return new tokenComment().processor(stream, state);
			        }
			        if (!stream.eat("/").isEmpty()) {
			            stream.skipToEnd();
			            return "comment";
			        }
			    }
			    if (isOperatorChar.matcher(ch).matches()) {
			        stream.eatWhile(isOperatorChar);
			        return "operator";
			    }
			    stream.eatWhile(pattern[3]);
			    String cur = stream.current().toLowerCase();
			    if (keyword.contains(cur)) {
			        if (blockKeywords.contains(cur))
			        	state.curPunc = "newstatement";
			        return "keyword";
			    }
			    else if (variable.contains(cur)) {
			        if (blockKeywords.contains(cur))
			        	state.curPunc = "newstatement";
			        return "variable";
			    }
			    else if (variable_2.contains(cur)) {
			        if (blockKeywords.contains(cur))
			        	state.curPunc = "newstatement";
			        return "variable-2";
			    }
			    else if (variable_3.contains(cur)) {
			        if (blockKeywords.contains(cur))
			        	state.curPunc = "newstatement";
			        return "variable-3";
			    }
			    else if (builtin.contains(cur)) {
			        if (blockKeywords.contains(cur)) 
			        	state.curPunc = "newstatement";
			        return "builtin";
			    }
			    else {
			        int i = cur.length() - 1;
			     
			        while (i >= 0 &&(isNumeric(cur.substring(i, i+1))||cur.substring(i, i+1).equals("_"))) -- i;
			        if (i > 0) {
			            String cur2 = cur.substring(0, i + 1);
			            if (variable_3.contains(cur2)) {
			                if (blockKeywords.contains(cur2)) 
			                	state.curPunc = "newstatement";
			                return "variable-3";
			            }
			        }
			    }
			    if (atoms.contains(cur)) return "atom";
			    return "";
		}
		
	}
	
	
	class tokenString implements Processor{
		String quote;
		public tokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String processor(StringStream stream, State state) {
			 boolean escaped = false, end = false;
			 String next;
		        while (!(next = stream.next()).isEmpty()) {
		            if (next.equals(quote) && !escaped) {
		                end = true;
		                break;
		            }
		            escaped = !escaped && next.equals("\\");
		        }
		        if (end || !escaped) state.tokenize = new tokenBase();
		        return "string";
		}
		
	}


class tokenComment implements Processor{

	@Override
	public String processor(StringStream stream, State state) {
		 boolean maybeEnd = false;
		 String ch;
		    while (!(ch=stream.next()).isEmpty()) {
		        if (ch.equals("/") && maybeEnd) {
		            state.tokenize =new tokenBase();
		            break;
		        }
		        maybeEnd = (ch.equals("*"));
		    }
		    return "comment";
	}
	
}
Context pushContext(State state,String type) {
    return state.context = new Context( type, true, state.context);
}

Context popContext(State state) {
    return state.context = state.context.prev;
}

	
	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "ecl");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State(null, true, new Context("top", false, null), "");
	}

	@Override
	public String token(StringStream stream, State state) {
		   Context ctx = state.context;
		    if (stream.sol()) {
		        state.startOfLine = true;
		     }
		    if (stream.eatSpace()) return "";
		    state.curPunc = "";
			String style;
			if (state.tokenize != null)
				style = state.tokenize.processor(stream, state);
			else
				style = new tokenBase().processor(stream, state);
		    
		    if (style.equals("comment") || style.equals("meta"))
		    	return style;
		    if ((state.curPunc.equals(";") || state.curPunc.equals(":")) && ctx.type.equals("statement"))
		    	popContext(state);
		    else if (state.curPunc.equals("{")) 
		    	pushContext(state,  "}");
		    else if (state.curPunc.equals("["))
		    	pushContext(state,  "]");
		    else if (state.curPunc.equals("("))
		    	pushContext(state,  ")");
		    else if (state.curPunc.equals("}")) {
		        while (ctx.type.equals("statement"))
		        	ctx = popContext(state);
		        if (ctx.type.equals("}"))
		        	ctx = popContext(state);
		        while (ctx.type.equals("statement"))
		        	ctx = popContext(state);
		    }
		    else if (state.curPunc.equals(ctx.type)) 
		    	popContext(state);
		    else if (ctx.type.equals("}") || ctx.type.equals("top") || (ctx.type.equals("statement") && state.curPunc.equals("newstatement"))) 
		    	 pushContext(state,  "statement");
		    	state.startOfLine = false;
		    return style;
		
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-ecl");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("ecl");
	}
	
}