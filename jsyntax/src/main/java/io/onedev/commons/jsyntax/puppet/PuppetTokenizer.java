package io.onedev.commons.jsyntax.puppet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class PuppetTokenizer extends AbstractTokenizer<PuppetTokenizer.State> {
	static final Pattern variable_regex  = Pattern.compile("(\\{)?([a-z][a-z0-9_]*)?((::[a-z][a-z0-9_]*)*::)?[a-zA-Z0-9_]+(})?");
	static final Map<String, String> words = new HashMap<String,String>();
	static {
		words.put("class", "keyword");
		words.put("define", "keyword");
		words.put("site", "keyword");
		words.put("node", "keyword");
		words.put("include", "keyword");
		words.put("import", "keyword");
		words.put("inherits", "keyword");
		words.put("case", "keyword");
		words.put("if", "keyword");
		words.put("else", "keyword");
		words.put("in", "keyword");
		words.put("and", "keyword");
		words.put("elsif", "keyword");
		words.put("default", "keyword");
		words.put("or", "keyword");
		words.put("false", "atom");
		words.put("true", "atom");
		words.put("running", "atom");
		words.put("present", "atom");
		words.put("absent", "atom");
		words.put("file", "atom");
		words.put("directory", "atom");
		words.put("undef", "atom");
		words.put("action", "builtin");
		words.put("augeas", "builtin");
		words.put("burst", "builtin");
		words.put("chain", "builtin");
		words.put("computer", "builtin");
		words.put("cron", "builtin");
		words.put("destination", "builtin");
		words.put("dport", "builtin");		
		words.put("exec", "builtin");
		words.put("file", "builtin");
		words.put("filebucket", "builtin");
		words.put("group", "builtin");
		words.put("host", "builtin");
		words.put("icmp", "builtin");
		words.put("iniface", "builtin");
		words.put("interface", "builtin");
		words.put("jump", "builtin");
		words.put("k5login", "builtin");
		words.put("limit", "builtin");
		words.put("log", "builtin");
		words.put("_level", "builtin");
		words.put("log_prefix", "builtin");
		words.put("macauthorization", "builtin");
		words.put("mailalias", "builtin");
		words.put("maillist", "builtin");
		words.put("mcx", "builtin");
		words.put("mount", "builtin");
		words.put("nagios_command", "builtin");
		words.put("nagios_contact", "builtin");
		words.put("nagios_contactgroup", "builtin");
		words.put("nagios_host", "builtin");
		words.put("nagios_hostdependency", "builtin");
		words.put("nagios_hostescalation", "builtin");
		words.put("nagios_hostextinfo", "builtin");
		words.put("nagios_hostgroup", "builtin");
		words.put("nagios_service", "builtin");
		words.put("nagios_servicedependency", "builtin");
		words.put("nagios_serviceescalation", "builtin");
		words.put("nagios_serviceextinfo", "builtin");
		words.put("nagios_servicegroup", "builtin");
		words.put("nagios_timeperiod", "builtin");
		words.put("name", "builtin");
		words.put("notify", "builtin");
		words.put("outiface", "builtin");
		words.put("package", "builtin");
		words.put("proto", "builtin");
		words.put("reject", "builtin");
		words.put("resources", "builtin");
		words.put("router", "builtin");
		words.put("schedule", "builtin");
		words.put("scheduled_task", "builtin");
		words.put("selboolean", "builtin");
		words.put("selmodule", "builtin");
		words.put("service", "builtin");
		words.put("source", "builtin");
		words.put("sport", "builtin");
		words.put("ssh_authorized_key", "builtin");
		words.put("sshkey", "builtin");
		words.put("stage", "builtin");
		words.put("state", "builtin");
		words.put("table", "builtin");
		words.put("tidy", "builtin");
		words.put("todest", "builtin");
		words.put("toports", "builtin");
		words.put("tosource", "builtin");
		words.put("user", "builtin");
		words.put("vlan", "builtin");
		words.put("yumrepo", "builtin");
		words.put("zfs", "builtin");
		words.put("zone", "builtin");
		words.put("zpool", "builtin");
	}
	static class State{
		String pending ;
		boolean continueString ;
		boolean inDefinition;
		boolean inInclude;
		public State(String pending ,boolean continueString ,boolean inDefinition,boolean inInclude) {
			this.pending = pending;
			this.continueString = continueString;
			this.inDefinition = inDefinition;
			this.inInclude = inInclude;
		}
	}
	public void define(String style, String string) {
		Set<String> words = Sets.newHashSet();
		String[] arr = string.split(" ");
		for (String o : arr) {
			words.add(o);
		}
	}
	String tokenString(StringStream stream, State state) {
	    String current="", prev="";
	    boolean found_var = false;
	    while (!stream.eol() && !(current = stream.next()).equals(state.pending)) {
	        if (current.equals("$") && !prev.equals("\\") && state.pending.equals("\"")) {
	            found_var = true;
	            break;
	        }
	        prev = current;
	    }
	    if (found_var) {
	        stream.backUp(1);
	    }
	    if (current.equals(state.pending)) {
	        state.continueString = false;
	    }
	    else {
	        state.continueString = true;
	    }
	    return "string";
	}
	static final Pattern pattern[] = new Pattern[19];
	static {
	    pattern[0] = Pattern.compile("[\\w]+");
	    pattern[1] = Pattern.compile("(\\s+)?\\w+\\s+=>.*");
	    pattern[2] = Pattern.compile("(\\s+)?[\\w:_]+(\\s+)?\\{");
	    pattern[3] = Pattern.compile("(\\s+)?[@]{1,2}[\\w:_]+(\\s+)?\\{");
	    pattern[4] = Pattern.compile("(\\s+)?[\\w:_]+(\\s+)?");
	    pattern[5] = Pattern.compile("\\s+\\{");
	    pattern[6] = Pattern.compile("(\\s+)?\\S+(\\s+)?");
	    pattern[7] = Pattern.compile("(\\s+)?\\w+\\(");
	    pattern[8] = Pattern.compile("(\\s+)?\\w+");
	    pattern[9] = Pattern.compile("[\\w]+");
	    pattern[10] = Pattern.compile("\\s+\\S+\\s+\\{");
	    pattern[11] = Pattern.compile("(^|\\s+)[A-Z][\\w:_]+");
	    pattern[12] = Pattern.compile("(^|\\s+)[A-Z][\\w:_]+");
	    pattern[13] = Pattern.compile("(\\s+)?[\\w:_]+");
	    pattern[14] = Pattern.compile("(\\s+)?[@]{1,2}");
	    pattern[15] = Pattern.compile(".*?\\/");
	    pattern[16] = Pattern.compile("[0-9]");
	    pattern[17] = Pattern.compile("[0-9]+");
	    pattern[18] = Pattern.compile("[\\w-]");
	}
	
	String tokenize(StringStream stream, State state) {
	    List<String> word = stream.match(pattern[0], false);
	    List<String> attribute = stream.match(pattern[1], false);
	    List<String> resource = stream.match(pattern[2], false);
	    List<String> special_resource = stream.match(pattern[3], false);
	    String ch = stream.next();
	    if (ch.equals("$")) {
	        if (!stream.match(variable_regex).isEmpty()) {
	            return state.continueString ? "variable-2" : "variable";
	        }
	        return "error";
	    }
	    if (state.continueString) {
	        stream.backUp(1);
	        return tokenString(stream, state);
	    }
	    if (state.inDefinition) {
	        if (!stream.match(pattern[4]).isEmpty()) {
	            return "def";
	        }
	        stream.match(pattern[5]);
	        state.inDefinition = false;
	    }
	    if (state.inInclude) {
	        stream.match(pattern[6]);
	        state.inInclude = false;
	        return "def";
	    }
	    if (!stream.match(pattern[7]).isEmpty()) {
	        stream.backUp(1);
	        return "def";
	    }
	    if (!attribute.isEmpty()) {
	        stream.match(pattern[8]);
	        return "tag";
	    }
	    String str;
	    str = word.size()>0?word.get(0):" ";
	    if (!word.isEmpty() && words.containsKey(str)) {
	        stream.backUp(1);
	        stream.match(pattern[9]);
	        if (!stream.match(pattern[10],false).isEmpty()) {
	            state.inDefinition = true;
	        }
	        if (word.equals("include")) {
	            state.inInclude = true;
	        }
	        return words.get(str);
	    }
	    if (pattern[11].matcher(str).matches()) {
	        stream.backUp(1);
	        stream.match(pattern[12]);
	        return "def";
	    }
	    if (!resource.isEmpty()) {
	        stream.match(pattern[13]);
	        return "def";
	    }
	    if (!special_resource.isEmpty()) {
	        stream.match(pattern[14]);
	        return "special";
	    }
	    if (ch.equals("#")) {
	        stream.skipToEnd();
	        return "comment";
	    }
	    if (ch.equals("'") || ch.equals("\"")) {
	        state.pending = ch;
	        return tokenString(stream, state);
	    }
	    if (ch.equals("{") || ch.equals("}")) {
	        return "bracket";
	    }
	    if (ch.equals("/")) {
	        stream.match(pattern[15]);
	        return "variable-3";
	    }
	    if (pattern[16].matcher(ch).matches()) {
	        stream.eatWhile(pattern[17]);
	        return "number";
	    }
	    if (ch.equals("=")) {
	        if (stream.peek() .equals(">")) {
	            stream.next();
	        }
	        return "operator";
	    }
	    stream.eatWhile(pattern[18]);
	    return "";
	}


	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "pp");
	}

	@Override
	public boolean acceptMime(String mime) {
		// TODO Auto-generated method stub
		return mime != null && mime.equals("text/x-puppet");
	}

	@Override
	public boolean acceptMode(String mode) {
		// TODO Auto-generated method stub
		return mode != null && mode.equals("puppet");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State("", false, false, false);
	}

	@Override
	public String token(StringStream stream, State state) {
	      if (stream.eatSpace()) 
	    	  return "";
	      return tokenize(stream, state);
	}

}
