package io.onedev.commons.jsyntax.rust;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.addon.Simple;

public class RustTokenizer extends Simple {
	
	private static final Map<String, List<Rule>> states = Maps.newHashMap();;
	
	static {
		states.put("start", Lists.newArrayList(
				new Rule(Pattern.compile("b?\""), Lists.newArrayList("string"), "string", "", false),
				new Rule(Pattern.compile("b?r\""), Lists.newArrayList("string"), "string_raw", "", false),
				new Rule(Pattern.compile("b?r#+\""), Lists.newArrayList("string"), "string_raw_hash", "", false),
				new Rule(Pattern.compile("'(?:[^'\\\\]|\\\\(?:[nrt0'\"]|x[\\da-fA-F]{2}|u\\{[\\da-fA-F]{6}\\}))'"), Lists.newArrayList("string-2"), "", "", false),
				new Rule(Pattern.compile("b'(?:[^']|\\\\(?:['\\\\nrt0]|x[\\da-fA-F]{2}))'"), Lists.newArrayList("string-2"), "", "", false),
				new Rule(Pattern.compile("(?:(?:[0-9][0-9_]*)(?:(?:[Ee][+-]?[0-9_]+)|\\.[0-9_]+(?:[Ee][+-]?[0-9_]+)?)(?:f32|f64)?)|(?:0(?:b[01_]+|(?:o[0-7_]+)|(?:x[0-9a-fA-F_]+))|(?:[0-9][0-9_]*))(?:u8|u16|u32|u64|i8|i16|i32|i64|isize|usize)?"), Lists.newArrayList("number"), "", "", false),
				new Rule(Pattern.compile("(let(?:\\s+mut)?|fn|enum|mod|struct|type)(\\s+)([a-zA-Z_][a-zA-Z0-9_]*)"), Lists.newArrayList("keyword", "", "def"), "", "", false),
				new Rule(Pattern.compile("(?:abstract|alignof|as|box|break|continue|const|crate|do|else|enum|extern|fn|for|final|if|impl|in|loop|macro|match|mod|move|offsetof|override|priv|proc|pub|pure|ref|return|self|sizeof|static|struct|super|trait|type|typeof|unsafe|unsized|use|virtual|where|while|yield)\\b"), Lists.newArrayList("keyword"), "", "", false),
				new Rule(Pattern.compile("\\b(?:Self|isize|usize|char|bool|u8|u16|u32|u64|f16|f32|f64|i8|i16|i32|i64|str|Option)\\b"), Lists.newArrayList("atom"), "", "", false),
				new Rule(Pattern.compile("\\b(?:true|false|Some|None|Ok|Err)\\b"), Lists.newArrayList("builtin"), "", "", false),
				new Rule(Pattern.compile("\\b(fn)(\\s+)([a-zA-Z_][a-zA-Z0-9_]*)"), Lists.newArrayList("keyword", "", "def"), "", "", false),
				new Rule(Pattern.compile("#!?\\[.*\\]"), Lists.newArrayList("meta"), "", "", false),
				new Rule(Pattern.compile("\\/\\/.*"), Lists.newArrayList("comment"), "", "", false),
				new Rule(Pattern.compile("\\/\\*"), Lists.newArrayList("comment"), "comment", "", false),
				new Rule(Pattern.compile("[-+\\/*=<>!]+"), Lists.newArrayList("operator"), "", "", false),
				new Rule(Pattern.compile("[a-zA-Z_]\\w*!"), Lists.newArrayList("variable-3"), "", "", false),
				new Rule(Pattern.compile("[a-zA-Z_]\\w*"), Lists.newArrayList("variable"), "", "", false),
				new Rule(Pattern.compile("[\\{\\[\\(]"), Lists.newArrayList(""), "", "", false),
				new Rule(Pattern.compile("[\\}\\]\\)]"), Lists.newArrayList(""), "", "", false)));
		states.put("string", Lists.newArrayList(
				new Rule(Pattern.compile("\""), Lists.newArrayList("string"), "start", "", false),
				new Rule(Pattern.compile("(?:[^\\\\\"]|\\\\(?:.|$))*"), Lists.newArrayList("string"), "", "", false)));
		states.put("string_raw", Lists.newArrayList(
				new Rule(Pattern.compile("\""), Lists.newArrayList("string"), "start", "", false),
				new Rule(Pattern.compile("[^\"]*"), Lists.newArrayList("string"), "", "", false)));
		states.put("string_raw_hash", Lists.newArrayList(
				new Rule(Pattern.compile("\"#+"), Lists.newArrayList("string"), "start", "", false),
				new Rule(Pattern.compile("(?:[^\"]|\"(?!#))*"), Lists.newArrayList("string"), "", "", false)));
		states.put("comment", Lists.newArrayList(
				new Rule(Pattern.compile(".*?\\*\\/"), Lists.newArrayList("comment"), "start", "", false),
				new Rule(Pattern.compile(".*"), Lists.newArrayList("comment"), "", "", false)));
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "rs");
	}

	@Override
	public Map<String, List<Rule>> getStates() {
		return states;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-rustsrc");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("rust");
	}
}
