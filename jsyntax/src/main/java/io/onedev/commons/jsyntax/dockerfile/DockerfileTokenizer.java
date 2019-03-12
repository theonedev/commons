package io.onedev.commons.jsyntax.dockerfile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.addon.Simple;

public class DockerfileTokenizer extends Simple {
	
	private static final Map<String, List<Rule>> states = Maps.newHashMap();
	private static final Set<String> instructions = Sets.newHashSet("from", "maintainer", "run", "cmd", "expose", "env",
            "add", "copy", "entrypoint", "volume", "user", "workdir", "onbuild");
	private static final String instructionRegex = "(" + StringUtils.join(instructions, "|") + ")";
	private static final Pattern instructionOnlyLine = Pattern.compile(instructionRegex + "\\s*$", Pattern.CASE_INSENSITIVE);
	private static final Pattern instructionWithArguments = Pattern.compile(instructionRegex + "(\\s+)", Pattern.CASE_INSENSITIVE);
	
	static {
		states.put("start", Lists.newArrayList(
				new Rule(Pattern.compile("#.*$"), Lists.newArrayList("comment"), "", "", false),
				new Rule(instructionOnlyLine, Lists.newArrayList("variable-2"), "", "", false),
				new Rule(instructionWithArguments, Lists.newArrayList("variable-2", ""), "arguments", "", false),
				new Rule(Pattern.compile("."), Lists.newArrayList(""), "", "", false)));
		states.put("arguments", Lists.newArrayList(
				new Rule(Pattern.compile("#.*$"), Lists.newArrayList("error"), "start", "", false),
				new Rule(Pattern.compile("[^#]+\\\\$"), Lists.newArrayList(""), "", "", false),
				new Rule(Pattern.compile("[^#]+"), Lists.newArrayList(""), "start", "", false),
				new Rule(Pattern.compile("$"), Lists.newArrayList(""), "start", "", false),
				new Rule(Pattern.compile("(?:)"), Lists.newArrayList(""), "start", "", false)));
	}

	@Override
	public boolean accept(String fileName) {
		return fileName != null && fileName.equals("Dockerfile");
	}

	@Override
	public Map<String, List<Rule>> getStates() {
		return states;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-dockerfile");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("dockerfile");
	}
}
