package io.onedev.commons.jsyntax.lua;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class LuaTokenizer extends AbstractTokenizer<LuaTokenizer.State> {

	static Pattern builtins;
	static Pattern keywords;
	static Pattern indentTokens;
	static Pattern dedentTokens;
	static Pattern dedentPartial;
	
	static {
		builtins = wordRE(Lists.newArrayList("_G","_VERSION","assert","collectgarbage","dofile","error","getfenv","getmetatable","ipairs","load",
			    "loadfile","loadstring","module","next","pairs","pcall","print","rawequal","rawget","rawset","require",
			    "select","setfenv","setmetatable","tonumber","tostring","type","unpack","xpcall",
			    "coroutine.create","coroutine.resume","coroutine.running","coroutine.status","coroutine.wrap","coroutine.yield",
			    "debug.debug","debug.getfenv","debug.gethook","debug.getinfo","debug.getlocal","debug.getmetatable",
			    "debug.getregistry","debug.getupvalue","debug.setfenv","debug.sethook","debug.setlocal","debug.setmetatable",
			    "debug.setupvalue","debug.traceback",
			    "close","flush","lines","read","seek","setvbuf","write",
			    "io.close","io.flush","io.input","io.lines","io.open","io.output","io.popen","io.read","io.stderr","io.stdin",
			    "io.stdout","io.tmpfile","io.type","io.write",
			    "math.abs","math.acos","math.asin","math.atan","math.atan2","math.ceil","math.cos","math.cosh","math.deg",
			    "math.exp","math.floor","math.fmod","math.frexp","math.huge","math.ldexp","math.log","math.log10","math.max",
			    "math.min","math.modf","math.pi","math.pow","math.rad","math.random","math.randomseed","math.sin","math.sinh",
			    "math.sqrt","math.tan","math.tanh",
			    "os.clock","os.date","os.difftime","os.execute","os.exit","os.getenv","os.remove","os.rename","os.setlocale",
			    "os.time","os.tmpname",
			    "package.cpath","package.loaded","package.loaders","package.loadlib","package.path","package.preload",
			    "package.seeall",
			    "string.byte","string.char","string.dump","string.find","string.format","string.gmatch","string.gsub",
			    "string.len","string.lower","string.match","string.rep","string.reverse","string.sub","string.upper",
			    "table.concat","table.insert","table.maxn","table.remove","table.sort"));
		keywords = wordRE(Lists.newArrayList("and","break","elseif","false","nil","not","or","return",
                "true","function", "end", "if", "then", "else", "do", "while", "repeat", "until", "for", "in", "local"));
		indentTokens = wordRE(Lists.newArrayList("function", "do", "if", "repeat", "\\{", "\\("));
		dedentTokens = wordRE(Lists.newArrayList("end", "until", "\\)", "}"));
		dedentPartial = prefixRE(Lists.newArrayList("end", "until", "\\)", "}", "else", "elseif"));
	}
	
	final Pattern specials;
	
	public LuaTokenizer() {
		this(Lists.newArrayList());
	}
	
	public LuaTokenizer(List<String> specials) {
		this.specials = wordRE(specials);
	}
	
	static Pattern prefixRE(List<String> words) {
		return Pattern.compile("^(?:" + StringUtils.join(words, "|") + ")", Pattern.CASE_INSENSITIVE);
	}
	
	static Pattern wordRE(List<String> words) {
		return Pattern.compile("^(?:" + StringUtils.join(words, "|") + ")$", Pattern.CASE_INSENSITIVE);
	}
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class State {
		Processor cur;
		int basecol;
		int indentDepth;
		
		public State(Processor cur, int basecol, int indentDepth) {
			this.cur = cur;
			this.basecol = basecol;
			this.indentDepth = indentDepth;
		}
	}
	
	int readBracket(StringStream stream){
	    int level = 0;
	    while (!stream.eat("=").isEmpty()) ++level;
	    stream.eat("[").isEmpty();
	    return level;
	}
	
	static Pattern pattern[] = new Pattern[5];
	
	static {
	    pattern[0] = Pattern.compile("[\\[=]");
	    pattern[1] = Pattern.compile("\\d");
	    pattern[2] = Pattern.compile("[\\w.%]");
	    pattern[3] = Pattern.compile("[\\w_]");
	    pattern[4] = Pattern.compile("[\\w\\\\\\-_.]");
	}

	class Normal implements Processor {
		@Override
		public String process(StringStream stream, State state) {
		    String ch = stream.next();
		    if (ch.equals("-") && !stream.eat("-").isEmpty()) {
		        if (!stream.eat("[").isEmpty() && !stream.eat("[").isEmpty())
		        	return (state.cur = new Bracketed(readBracket(stream), "comment")).process(stream, state);
		        stream.skipToEnd();
		        return "comment";
		    }
		    if (ch.equals("\"") || ch.equals("'"))
		    	return (state.cur = new _String(ch)).process(stream, state);
		    if (ch.equals("[") && pattern[0].matcher(stream.peek()).matches())
		    	return (state.cur = new Bracketed(readBracket(stream), "string")).process(stream, state);
		    if (pattern[1].matcher(ch).matches()) {
		        stream.eatWhile(pattern[2]);
		        return "number";
		    }
		    if (pattern[3].matcher(ch).matches()) {
		        stream.eatWhile(pattern[4]);
		        return "variable";
		    }
			return "";
		}
	}

	class Bracketed implements Processor {
		int level;
		String style;
		
		public Bracketed(int level, String style) {
			this.level = level;
			this.style = style;
		}
		
		@Override
		public String process(StringStream stream, State state) {
	        int curlev = 0;
	        String ch;
	        while (!(ch = stream.next()).isEmpty()) {
	            if (curlev == 0) {
	                if (ch.equals("]")) curlev = 0;
	            }
	            else if (ch.equals("="))
	            	++curlev;
	            else if (ch.equals("]") && curlev == level) {
	                state.cur = new Normal();
	                break;
	            }
	            else curlev = 0;
	        }
	        return style;
		}
	}
	
	class _String implements Processor {
		String quote;
		
		public _String(String quote) {
			this.quote = quote;
		}
		
		@Override
		public String process(StringStream stream, State state) {
	        boolean escaped = false;
	        String ch;
	        while (!(ch = stream.next()).isEmpty()) {
	            if (ch.equals(quote) && !escaped) break ;
	            escaped = !escaped && ch.equals("\\");
	        }
	        if (!escaped) state.cur = new Normal();
	        return "string";
		}
		
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "lua");
	}

	@Override
	public State startState() {
		return new State(new Normal(), 0, 0);
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (stream.eatSpace()) return "";
	    String style = state.cur.process(stream, state);
	    String word = stream.current();
	    if (style.equals("variable")) {
	        if (keywords.matcher(word).matches()) style = "keyword";
	        else if (builtins.matcher(word).matches()) style = "builtin";
	        else if (specials.matcher(word).matches()) style = "variable-2";
	    }
	    if ((!style.equals("comment")) && (!style.equals("string"))) {
	        if (indentTokens.matcher(word).matches()) ++state.indentDepth;
	        else if (dedentTokens.matcher(word).matches()) --state.indentDepth;
	    }
	    return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-lua");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("lua");
	}
}
