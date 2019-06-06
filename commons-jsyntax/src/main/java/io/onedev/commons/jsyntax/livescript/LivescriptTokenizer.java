package io.onedev.commons.jsyntax.livescript;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class LivescriptTokenizer extends AbstractTokenizer<LivescriptTokenizer.State> {

	static final Pattern identifier = Pattern
			.compile("(?![\\d\\s])[$\\w\\xAA-\\uFFDC](?:(?!\\s)[$\\w\\xAA-\\uFFDC]|-[A-Za-z])*");
	static final Pattern keywordend = Pattern.compile("(?![$\\w]|-[A-Za-z]|\\s*:(?![:=]))");
	static final Rule stringfill = new Rule(Pattern.compile(".+"), "string", "", "", false);
	static final Map<String, List<Rule>> Rules = Maps.newHashMap();
	static final Map<String, Rule> u = Maps.newHashMap();

	static {
		Rules.put("start", Lists.newArrayList(new Rule(Pattern.compile("/\\*"), "comment.doc", "comment", "", false),
				new Rule(Pattern.compile("#.*"), "comment", "", "", false),
				new Rule(Pattern.compile(
						"(?:t(?:h(?:is|row|en)|ry|ypeof!?)|c(?:on(?:tinue|st)|a(?:se|tch)|lass)|i(?:n(?:stanceof)?|mp(?:ort(?:\\s+all)?|lements)|[fs])|d(?:e(?:fault|lete|bugger)|o)|f(?:or(?:\\s+own)?|inally|unction)|s(?:uper|witch)|e(?:lse|x(?:tends|port)|val)|a(?:nd|rguments)|n(?:ew|ot)|un(?:less|til)|w(?:hile|ith)|o[fr]|return|break|let|var|loop)"
								+ keywordend),
						"keyword", "", "", false),
				new Rule(Pattern.compile("(?:true|false|yes|no|on|off|null|void|undefined)" + keywordend),
						"constant.language", "", "", false),
				new Rule(Pattern
						.compile("(?:p(?:ackage|r(?:ivate|otected)|ublic)|i(?:mplements|nterface)|enum|static|yield)"
								+ keywordend),
						"invalid.illegal", "", "", false),
				new Rule(Pattern.compile(
						"(?:R(?:e(?:gExp|ferenceError)|angeError)|S(?:tring|yntaxError)|E(?:rror|valError)|Array|Boolean|Date|Function|Number|Object|TypeError|URIError)"
								+ keywordend),
						"language.support.class", "", "", false),
				new Rule(Pattern
						.compile("(?:is(?:NaN|Finite)|parse(?:Int|Float)|Math|JSON|(?:en|de)codeURI(?:Component)?)"
								+ keywordend),
						"language.support.function", "", "", false),
				new Rule(Pattern.compile("(?:t(?:hat|il|o)|f(?:rom|allthrough)|it|by|e)" + keywordend),
						"variable.language", "", "", false),
				new Rule(Pattern.compile(identifier + "\\s*:(?![:=])"), "identifier", "", "", false),
				new Rule(Pattern.compile("" + identifier), "variable", "", "", false),
				new Rule(Pattern.compile("(?:\\.{3}|\\s+\\?)"), "keyword.operator", "", "", false),
				new Rule(Pattern.compile("(?:@+|::|\\.\\.)"), "keyword.variable", "key", "", false),
				new Rule(Pattern.compile("\\.\\s*"), "keyword.operator", "key", "", false),
				new Rule(Pattern.compile("\\\\\\S[^\\s,;)}\\]]*"), "string", "", "", false),
				new Rule(Pattern.compile("\'\'\'"), "string.doc", "qdoc", "", false),
				new Rule(Pattern.compile("\"\"\""), "string.doc", "qqdoc", "", false),
				new Rule(Pattern.compile("\'"), "string", "qstring", "", false),
				new Rule(Pattern.compile("\""), "string", "qqstring", "", false),
				new Rule(Pattern.compile("`"), "string", "js", "", false),
				new Rule(Pattern.compile("<\\["), "string", "words", "", false),
				new Rule(Pattern.compile("//"), "string.regex", "heregex", "", false),
				/*new Rule(Pattern.compile(
						"\\/(?:[^[\\/\\n\\\\]*(?:(?:\\\\.|\\[[^\\]\\n\\\\]*(?:\\\\.[^\\]\\n\\\\]*)*\\])[^[\\/\\n\\\\]*)*)\\/[gimy$]{0,4}"),
						"string.regex", "key", "", false),*/
				new Rule(Pattern.compile(
						"(?:0x[\\da-fA-F][\\da-fA-F_]*|(?:[2-9]|[12]\\d|3[0-6])r[\\da-zA-Z][\\da-zA-Z_]*|(?:\\d[\\d_]*(?:\\.\\d[\\d_]*)?|\\.\\d[\\d_]*)(?:e[+-]?\\d[\\d_]*)?[\\w$]*)"),
						"constant.numeric", "", "", false),
				new Rule(Pattern.compile("[\\(\\{\\[]"), "lparen", "", "", false),
				new Rule(Pattern.compile("[)}\\]]"), "rparen", "key", "", false),
				new Rule(Pattern.compile("\\S+"), "keyword.operator", "", "", false),
				new Rule(Pattern.compile("\\s+"), "text", "", "", false)));
		Rules.put("heregex",
				Lists.newArrayList(new Rule(Pattern.compile(".*?//[gimy$?]{0,4}"), "string.regex", "start", "", false),
						new Rule(Pattern.compile("\\s*#\\{"), "string.regex", "", "", false),
						new Rule(Pattern.compile("\\s+(?:#.*)?"), "comment.regex", "", "", false),
						new Rule(Pattern.compile("\\S+"), "string.regex", "", "", false)));
		Rules.put("key",
				Lists.newArrayList(new Rule(Pattern.compile("[.?@!]+"), "keyword.operator", "", "", false),
						new Rule(Pattern.compile("" + identifier), "identifier", "start", "", false),
						new Rule(Pattern.compile(""), "text", "start", "", false)));
		Rules.put("comment", Lists.newArrayList(new Rule(Pattern.compile(".*?\\*/"), "comment.doc", "start", "", false),
				new Rule(Pattern.compile(".+"), "comment.doc", "", "", false)));
		Rules.put("qdoc",
				Lists.newArrayList(new Rule(Pattern.compile(".*?'''"), "string", "key", "", false), stringfill));
		Rules.put("qqdoc",
				Lists.newArrayList(new Rule(Pattern.compile(".*?\"\"\""), "string", "key", "", false), stringfill));
		Rules.put("qstring",
				Lists.newArrayList(new Rule(Pattern.compile("[^\\\\\']*(?:\\\\.[^\\\\\']*)*\'"),
						"string", "key", "", false), stringfill));
		Rules.put("qqstring", Lists.newArrayList(
				new Rule(Pattern.compile("[^\\\\\"]*(?:\\\\.[^\\\\\"]*)*\""), "string", "key", "", false),
				stringfill));
		Rules.put("js", Lists.newArrayList(
				new Rule(Pattern.compile("[^\\\\\"]*(?:\\\\.[^\\\\\"]*)*\""), "string", "key", "", false),
				stringfill));
		Rules.put("words",
				Lists.newArrayList(new Rule(Pattern.compile(".*?\\]>"), "string", "key", "", false), stringfill));
	}
	static {
		u.put("qdoc", new Rule(Pattern.compile(".*?\'\'\'"), "string", "key", "", false));
		u.put("qqdoc", new Rule(Pattern.compile(".*?\"\"\""), "string", "key", "", false));
		u.put("qstring", new Rule(Pattern.compile("[^\\\\\']*(?:\\\\.[^\\\\\']*)*\'"), "string", "key",
				"", false));
		u.put("qqstring",
				new Rule(Pattern.compile("[^\\\\\"]*(?:\\\\.[^\\\\\"]*)*\""), "string", "key", "", false));
		u.put("js",
				new Rule(Pattern.compile("[^\\\\\"]*(?:\\\\.[^\\\\\"]*)*\""), "string", "key", "", false));
		u.put("words", new Rule(Pattern.compile(".*?\\]>"), "string", "key", "", false));
	}

	class State {
		String next;
		Context lastToken;

		public State(String next, Context lastToken) {
			this.next = next;
			this.lastToken = lastToken;
		}

	}

	class Context {
		String style;
		String content;

		public Context(String style, String content) {
			this.style = style;
			this.content = content;
		}
	}

	public static class Rule {
		Pattern regex;
		String token;
		String next;
		String push;
		boolean pop;

		public Rule(Pattern regex, String token, String next, String push, boolean pop) {
			this.regex = regex;
			this.token = token;
			this.next = next;
			this.push = push;
			this.pop = pop;
		}
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	public String tokenBase(StringStream stream, State state) {
		String next_rule;
		if (!state.next.isEmpty()) {
			next_rule = state.next;
		} else {
			next_rule = "start";
		}
		if (!next_rule.isEmpty()) {
			state.next = state.next;
			List<Rule> nr = Rules.get(next_rule);
			if (nr.size() > 0) {
				for (int i$ = 0; i$ < nr.size(); ++i$) {
					Rule r = nr.get(i$);
					if (r.regex != null && !stream.match(r.regex).isEmpty()) {
						state.next = (!r.next.isEmpty() ? r.next : state.next);
						return r.token;
					}
				}
				stream.next();
				return "error";
			}
			Rule r = u.get(next_rule);
			if (!stream.match(r.regex).isEmpty()) {
				if (r.regex != null && !stream.match(r.regex).isEmpty()) {
					state.next = r.next;
					return r.token;
				} else {
					stream.next();
					return "error";
				}
			}
		}
		stream.next();
		return "error";
	}

	@Override
	public boolean accept(String fileName) {
		// TODO Auto-generated method stub
		return acceptExtensions(fileName, "livescript");
	}

	@Override
	public State startState() {
		// TODO Auto-generated method stub
		return new State("start", new Context("", ""));
	}

	static final Pattern pattern[] = new Pattern[1];
	static {
		pattern[0] = Pattern.compile("\\.");
	}

	@Override
	public String token(StringStream stream, State state) {
		String style = "";
		while (stream.pos() == stream.start())
			style = tokenBase(stream, state);
		state.lastToken.style = style;
		state.lastToken.content = stream.current();
		Matcher mat = pattern[0].matcher(style);
		return mat.replaceAll(" ");
	}

	@Override
	public boolean acceptMime(String mime) {
		// TODO Auto-generated method stub
		return mime != null && mime.equals("text/x-livescript");
	}

	@Override
	public boolean acceptMode(String mode) {
		// TODO Auto-generated method stub
		return mode != null && mode.equals("livescript");
	}

}
