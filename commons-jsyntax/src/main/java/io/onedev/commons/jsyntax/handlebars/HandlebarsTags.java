package io.onedev.commons.jsyntax.handlebars;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.addon.Simple;

public class HandlebarsTags extends Simple {
	
	static final Map<String, List<Rule>> states = Maps.newHashMap();
	static {
		states.put("start", Lists.newArrayList(
				new Rule(Pattern.compile("\\{\\{!--"), Lists.newArrayList("comment"), "", "dash_comment", false),
				new Rule(Pattern.compile("\\{\\{!"), Lists.newArrayList("comment"), "", "comment", false),
				new Rule(Pattern.compile("\\{\\{"), Lists.newArrayList("tag"), "", "handlebars", false)));
		states.put("handlebars", Lists.newArrayList(
				new Rule(Pattern.compile("\\}\\}"), Lists.newArrayList("tag"), "", "", true),
				new Rule(Pattern.compile("\"(?:[^\\\\\"]|\\\\.)*\"?"), Lists.newArrayList("string"), "", "", false),
				new Rule(Pattern.compile("'(?:[^\\\\']|\\\\.)*'?"), Lists.newArrayList("string"), "", "", false),
				new Rule(Pattern.compile(">|[#\\/]([A-Za-z_]\\w*)"), Lists.newArrayList("keyword"), "", "", false),
				new Rule(Pattern.compile("(?:else|this)\\b"), Lists.newArrayList("keyword"), "", "", false),
				new Rule(Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE), Lists.newArrayList("number"), "", "", false),
				new Rule(Pattern.compile("=|~|@|true|false"), Lists.newArrayList("atom"), "", "", false),
				new Rule(Pattern.compile("(?:\\.\\.\\/)*(?:[A-Za-z_][\\w\\.]*)+"), Lists.newArrayList("variable-2"), "", "", false)));
		states.put("dash_comment", Lists.newArrayList(
				new Rule(Pattern.compile("--\\}\\}"), Lists.newArrayList("comment"), "", "", true),
				new Rule(Pattern.compile("."), Lists.newArrayList("comment"), "", "", false)));
		states.put("comment", Lists.newArrayList(
				new Rule(Pattern.compile("\\}\\}"), Lists.newArrayList("comment"), "", "", true),
				new Rule(Pattern.compile("."), Lists.newArrayList("comment"), "", "", false)));
	}

	@Override
	public boolean accept(String fileName) {
		return false;
	}

	@Override
	public boolean acceptMime(String mime) {
		return false;
	}

	@Override
	public boolean acceptMode(String mode) {
		return false;
	}

	@Override
	public Map<String, List<Rule>> getStates() {
		return states;
	}
}
