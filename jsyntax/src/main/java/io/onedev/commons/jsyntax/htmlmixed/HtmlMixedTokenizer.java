package io.onedev.commons.jsyntax.htmlmixed;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.Tokenizer;
import io.onedev.commons.jsyntax.TokenizerRegistry;
import io.onedev.commons.jsyntax.xml.XmlTokenizer;

public class HtmlMixedTokenizer extends AbstractTokenizer<HtmlMixedTokenizer.State> {
	
	static final XmlTokenizer htmlMode = new XmlTokenizer(true, null);
	
	public static class Info {
		String attr;
		Pattern pat;
		String mode;
		
		public Info(String attr, Pattern pat, String mode) {
			this.attr = attr;
			this.pat = pat;
			this.mode = mode;
		}
	}
	
	final Map<String, List<Info>> tags = Maps.newHashMap();
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	public static class State {
		Processor tokenize;
		String inTag;
		@SuppressWarnings("rawtypes")
		AbstractTokenizer localMode;
		Object localState;
		XmlTokenizer.State htmlState;
		
		public State(Processor tokenize) {
			this.tokenize = tokenize;
			this.inTag = "";
			
			this.localMode = null;
			this.localState = null;
			
			this.htmlState = htmlMode.startState();
		}
		
		public XmlTokenizer.State getXmlState() {
			if (localMode instanceof XmlTokenizer)
				return htmlState;
			return null;
		}
	}
	
	public HtmlMixedTokenizer(Map<String, List<Info>> _tags) {
		List<Info> infos = Lists.newArrayList();
		infos.add(new Info("lang", Pattern.compile("(javascript|babel)", Pattern.CASE_INSENSITIVE), "javascript"));
		infos.add(new Info("type", Pattern.compile("^(?:text|application)\\/(?:x-)?(?:java|ecma)script$|^$", Pattern.CASE_INSENSITIVE), "javascript"));
		infos.add(new Info("type", Pattern.compile("."), "text/plain"));
		infos.add(new Info("", null, "javascript"));
		tags.put("script", infos);
		infos = Lists.newArrayList();
		infos.add(new Info("lang", Pattern.compile("^css$", Pattern.CASE_INSENSITIVE), "css"));
		infos.add(new Info("type", Pattern.compile("^(text\\/)?(x-)?(stylesheet|css)$", Pattern.CASE_INSENSITIVE), "css"));
		infos.add(new Info("type", Pattern.compile("."), "text/plain"));
		infos.add(new Info("", null, "css"));
		tags.put("style", infos);
		for (String key : _tags.keySet()) {
			if (tags.containsKey(key)) {
				List<Info> lst = tags.get(key);
				List<Info> _lst = _tags.get(key);
				lst.addAll(_lst);
				tags.replace(key, lst);
			} else {
				tags.put(key, _tags.get(key));
			}
		}
	}
	
	public HtmlMixedTokenizer() {
		this(Maps.newHashMap());
	}
	
	private static Pattern start = Pattern.compile("<\\/?$");
	
	private String maybeBackup(StringStream stream, Pattern pat, String style) {
		String cur = stream.current();
		int close = pat.matcher(cur).find() ? pat.matcher(cur).start() : -1;
		if (close > -1) {
			stream.backUp(cur.length() - close);
		} else if (start.matcher(cur).matches()) {
			stream.backUp(cur.length());
			if (stream.match(pat, false).isEmpty())
				stream.match(cur);
		}
		return style;
	}
	
	static Map<String, Pattern> attrRegexpCache = Maps.newHashMap();
	
	static Pattern getAttrRegexp(String attr) {
		if (attrRegexpCache.containsKey(attr))
			return attrRegexpCache.get(attr);
		Pattern pat = Pattern.compile("\\s+" + attr + "\\s*=\\s*('|\")?([^'\"]+)('|\")?\\s*");
		attrRegexpCache.put(attr, pat);
		return pat;
	}
	
	private static Pattern attrValue = Pattern.compile("^\\s*(.*?)\\s*$");
	
	static String getAttrValue(String text, String attr) {
		Matcher match = getAttrRegexp(attr).matcher(text);
		if (match.find()) {
			match = attrValue.matcher(match.group(2));
			match.find();
			return match.group(1);
		}
		return "";
	}
	
	static Pattern getTagRegexp(String tagName, boolean anchored) {
		return Pattern.compile((anchored ? "^" : "") + "<\\/\\s*" + tagName + "\\s*>", Pattern.CASE_INSENSITIVE);
	}
	
	static String findMatchingMode(List<Info> tagInfo, String tagText) {
		for (Info spec : tagInfo) {
			if (spec.attr.isEmpty() || spec.pat.matcher(getAttrValue(tagText, spec.attr)).matches()) {
				return spec.mode;
			}
		}
		return "";
	}
	
	private static Pattern[] pattern = new Pattern[4];
	
	static {
		pattern[0] = Pattern.compile("\\btag\\b");
		pattern[1] = Pattern.compile("[<>\\s\\/]");
		pattern[2] = Pattern.compile(">$");
		pattern[3] = Pattern.compile("^([\\S]+) (.*)");
	}
	
	class Html implements Processor {

		@SuppressWarnings("rawtypes")
		@Override
		public String process(StringStream stream, State state) {
			String style = htmlMode.token(stream, state.htmlState);
			boolean tag = pattern[0].matcher(style).find();
			String tagName;
			if (tag && !pattern[1].matcher(stream.current()).matches() &&
					!(tagName = htmlMode.getTagName(state.htmlState).toLowerCase()).isEmpty() &&
					tags.containsKey(tagName)) {
				state.inTag = tagName + " ";
			} else if (!state.inTag.isEmpty() && tag && pattern[2].matcher(stream.current()).matches()) {
				Matcher match = pattern[3].matcher(state.inTag);
				match.find();
				state.inTag = "";
				String modeSpec = stream.current().equals(">") ? findMatchingMode(tags.get(match.group(1)), match.group(2)) : "null";
				state.tokenize = new Inner(getTagRegexp(match.group(1), true), getTagRegexp(match.group(1), false));
				Tokenizer mode = TokenizerRegistry.getTokenizerByMode(modeSpec);
				if (mode == null) {
					mode = TokenizerRegistry.getTokenizerByMime(modeSpec);
				}
				state.localMode = (AbstractTokenizer)mode;
				state.localState = state.localMode.startState();
			} else if (!state.inTag.isEmpty()) {
				state.inTag += stream.current();
				if (stream.eol())
					state.inTag += " ";
			}
			return style;
		}
	}
	
	class Inner implements Processor {
		Pattern endTagA;
		Pattern endTag;
		
		public Inner(Pattern endTagA, Pattern endTag) {
			this.endTagA = endTagA;
			this.endTag = endTag;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public String process(StringStream stream, State state) {
			if (!stream.match(endTagA, false).isEmpty()) {
				state.tokenize = new Html();
				state.localMode = null;
				state.localState = null;
				return "";
			}
			return maybeBackup(stream, endTag, state.localMode.token(stream, state.localState));
		}
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "htm", "html");
	}

	@Override
	public State startState() {
		return new State(new Html());
	}

	@Override
	public String token(StringStream stream, State state) {
		return state.tokenize.process(stream, state);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/html");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("htmlmixed");
	}
}
