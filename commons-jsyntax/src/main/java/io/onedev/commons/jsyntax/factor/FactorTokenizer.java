package io.onedev.commons.jsyntax.factor;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.addon.Simple;


public class FactorTokenizer extends Simple {
	private static final Map<String, List<Rule>> states = Maps.newHashMap();;
	static {
		states.put("start", Lists.newArrayList(
				new Rule(Pattern.compile("#?!.*"), Lists.newArrayList("comment"), "", "", false),
				new Rule(Pattern.compile("\"\"\""), Lists.newArrayList("string"), "string3", "", false),
				new Rule(Pattern.compile("\""), Lists.newArrayList("string",""), "string", "", false),
				new Rule(Pattern.compile("(?:[+-]?)(?:0x[\\d,a-f]+)|(?:0o[0-7]+)|(?:0b[0,1]+)|(?:\\d+.?\\d*)"), Lists.newArrayList("number"), "", "", false),
				new Rule(Pattern.compile("(\\:)(\\s+)(\\S+)(\\s+)(\\()"), Lists.newArrayList("keyword", "", "def", "", "keyword"), "stack", "", false),
				new Rule(Pattern.compile("USING\\:"), Lists.newArrayList("keyword"), "vocabulary", " ", false),
				new Rule(Pattern.compile("(USE\\:|IN\\:)(\\s+)(\\S+)"), Lists.newArrayList("keyword", "", "variable-2"), "", "", false),			
				new Rule(Pattern.compile("<\\S+>"), Lists.newArrayList("builtin"), "", "", false),
				new Rule(Pattern.compile(";|t|f|if|\\.|\\[|\\]|\\{|\\}|MAIN:"), Lists.newArrayList("keyword"), "", "", false),
				new Rule(Pattern.compile("\\S+"), Lists.newArrayList("variable"), "", "", false),
				new Rule(Pattern.compile("."), Lists.newArrayList(""), "", "", false)
				));
		states.put("vocabulary",  Lists.newArrayList(
				new Rule(Pattern.compile(";"), Lists.newArrayList("keyword"), "start", "", false),
				new Rule(Pattern.compile("\\S+"), Lists.newArrayList("variable-2"), "", "", false),
				new Rule(Pattern.compile("."), Lists.newArrayList(""), "", "", false)
				));
		states.put("string",  Lists.newArrayList(
				new Rule(Pattern.compile("(?:[^\\\\]|\\\\.)*?\""), Lists.newArrayList("string"), "start", "", false),
				new Rule(Pattern.compile(".*"), Lists.newArrayList("string"), "", "", false)	
				));
		states.put("string3",  Lists.newArrayList(
				new Rule(Pattern.compile("(?:[^\\\\]|\\\\.)*?\"\"\""), Lists.newArrayList("string"), "start", "", false),
				new Rule(Pattern.compile(".*"), Lists.newArrayList("string"), "", "", false)	
				));
		states.put("stack",  Lists.newArrayList(
				new Rule(Pattern.compile("\\)"), Lists.newArrayList("meta"), "start", "", false),
				new Rule(Pattern.compile("--"), Lists.newArrayList("meta"), "", "", false),
				new Rule(Pattern.compile("\\S+"), Lists.newArrayList("variable-3"), "", "", false),
				new Rule(Pattern.compile("."), Lists.newArrayList(""), "", "", false)	
				));
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "factor");
	}

	@Override
	public Map<String, List<Rule>> getStates() {
		return states;
	}

	@Override
	public boolean acceptMime(String mime) {
		// TODO Auto-generated method stub
		return mime != null && mime.equals("text/x-factor");
	}

	@Override
	public boolean acceptMode(String mode) {
		// TODO Auto-generated method stub
		return mode != null && mode.equals("factor");
	}

}
