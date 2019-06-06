package io.onedev.commons.jsyntax.xml;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class XmlTokenizer extends AbstractTokenizer<XmlTokenizer.State> {

	static class Config {
		Set<String> autoSelfClosers;
		Set<String> implicitlyClosed;
		Map<String, Set<String>> contextGrabbers;
		Set<String> doNotIndent;
		boolean allowUnquoted;
		boolean allowMissing;
		boolean caseFold;
		boolean matchClosing;
	}
	
	static Config htmlConfig = new Config();
	static Config xmlConfig = new Config();
	
	static {
		htmlConfig.autoSelfClosers = Sets.newHashSet("area", "base", "br", "col", "command", 
				"embed", "frame", "hr", "img", "input", "keygen", "link", "meta", "param", 
				"source", "track", "wbr", "menuitem");
		htmlConfig.implicitlyClosed = Sets.newHashSet("dd", "li", "optgroup", "option", "p",
                "rp", "rt", "tbody", "td", "tfoot", "th", "tr");
		htmlConfig.contextGrabbers = Maps.newHashMap();
		htmlConfig.contextGrabbers.put("dd", Sets.newHashSet("dd", "dt"));
		htmlConfig.contextGrabbers.put("dt", Sets.newHashSet("dd", "dt"));
		htmlConfig.contextGrabbers.put("li", Sets.newHashSet("li"));
		htmlConfig.contextGrabbers.put("option", Sets.newHashSet("option", "optgroup"));
		htmlConfig.contextGrabbers.put("optgroup", Sets.newHashSet("optgroup"));
		htmlConfig.contextGrabbers.put("p", Sets.newHashSet("address", "article", "aside", "blockquote", "dir",
		          "div", "dl", "fieldset", "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6",
		          "header", "hgroup", "hr", "menu", "nav", "ol", "p", "pre", "section", "table", "ul"));
		htmlConfig.contextGrabbers.put("rp", Sets.newHashSet("rp", "rt"));
		htmlConfig.contextGrabbers.put("rt", Sets.newHashSet("rp", "rt"));
		htmlConfig.contextGrabbers.put("tbody", Sets.newHashSet("tbody", "tfoot"));
		htmlConfig.contextGrabbers.put("td", Sets.newHashSet("td", "th"));
		htmlConfig.contextGrabbers.put("tfoot", Sets.newHashSet("tbody"));
		htmlConfig.contextGrabbers.put("th", Sets.newHashSet("td", "th"));
		htmlConfig.contextGrabbers.put("thead", Sets.newHashSet("tbody", "tfoot"));
		htmlConfig.contextGrabbers.put("tr", Sets.newHashSet("tr"));
		htmlConfig.doNotIndent = Sets.newHashSet("pre");
		htmlConfig.allowUnquoted = true;
		htmlConfig.allowMissing = true;
		htmlConfig.caseFold = true;
		htmlConfig.matchClosing = true;
		
		xmlConfig.autoSelfClosers = Sets.newHashSet();
		xmlConfig.implicitlyClosed = Sets.newHashSet();
		xmlConfig.contextGrabbers = Maps.newHashMap();
		xmlConfig.doNotIndent = Sets.newHashSet();
		xmlConfig.allowUnquoted = false;
		xmlConfig.allowMissing = false;
		xmlConfig.caseFold = false;
		xmlConfig.matchClosing = true;
	}
	
	final Config config;
	
	/**
	 * Provide a default constructor to be used by {@link TokenizerRegistry}
	 */
	public XmlTokenizer() {
		this(false, null);
	}
	
	public XmlTokenizer(boolean htmlMode, Boolean allowMissing) {
		config = htmlMode ? htmlConfig : xmlConfig;
		if (allowMissing != null)
			config.allowMissing = allowMissing;
	}
	
	interface Processor {
		String process(StringStream stream, State state);
	}
	
	interface StateInterface {
		StateInterface process(String type, StringStream stream, State state);
	}
	
	class Context {
		Context prev;
		String tagName;
		int indent;
		boolean startOfLine;
		boolean noIndent;
		
		Context(State state, String tagName, boolean startOfLine) {
			this.prev = state.context;
			this.tagName = tagName;
			this.indent = state.indented;
			this.startOfLine = startOfLine;
			if (config.doNotIndent.contains(tagName) || (state.context != null && state.context.noIndent))
				this.noIndent = true;
			else
				this.noIndent = false;
		}
	}
	
	public static class State {
		Processor tokenize;
		Context context;
		StateInterface state;
		String tagName;
		String type;
		String setStyle;
		int indented;
		int tagStart;
		
		State(Processor tokenize, Context context, StateInterface state, String tagName, int indented, int tagStart) {
			this.tokenize = tokenize;
			this.context = context;
			this.state = state;
			this.tagName = tagName;
			this.indented = indented;
			this.tagStart = tagStart;
		}
		
		public int getTagStart() {
			return tagStart;
		}
		
		public boolean isContextNull() {
			return context == null;
		}
		
		public boolean isInText() {
			return tokenize instanceof InText;
		}
	}
	
	private static Pattern[] pattern = new Pattern[8];
	
	static {
		pattern[0] = Pattern.compile("[\\w\\._\\-]");
		pattern[1] = Pattern.compile("[\\w\\._\\-]");
		pattern[2] = Pattern.compile("[a-fA-F\\d]");
		pattern[3] = Pattern.compile("[\\d]");
		pattern[4] = Pattern.compile("[\\w\\.\\-:]");
		pattern[5] = Pattern.compile("[^&<]");
		pattern[6] = Pattern.compile("[\'\"]");
		pattern[7] = Pattern.compile("^[^\\s\\u00a0=<>\"\']*[^\\s\\u00a0=<>\"\'\\/]");
	}
	
	class InText implements Processor {
		
		String chain(StringStream stream, State state, Processor parser) {
			state.tokenize = parser;
			return parser.process(stream, state);
		}

		@Override
		public String process(StringStream stream, State state) {
			String ch = stream.next();
			if (ch.equals("<")) {
				if (!stream.eat("!").isEmpty()) {
					if (!stream.eat("[").isEmpty()) {
						if (stream.match("CDATA["))
							return chain(stream, state, new InBlock("atom", "]]>"));
						else
							return "";
					} else if (stream.match("--")) {
						return chain(stream, state, new InBlock("comment", "-->"));
					} else if (stream.match("DOCTYPE", true, true)) {
						stream.eatWhile(pattern[0]);
						return chain(stream, state, new Doctype(1));
					} else {
						return "";
					}
				} else if (!stream.eat("?").isEmpty()) {
					stream.eatWhile(pattern[1]);
					state.tokenize = new InBlock("meta", "?>");
					return "meta";
				} else {
					state.type = !stream.eat("/").isEmpty() ? "closeTag" : "openTag";
					state.tokenize = new InTag();
					return "tag bracket";
				}
			} else if (ch.equals("&")) {
				boolean ok;
				if (!stream.eat("#").isEmpty()) {
					if (!stream.eat("x").isEmpty()) {
						ok = stream.eatWhile(pattern[2]) && !stream.eat(";").isEmpty();
					} else {
						ok = stream.eatWhile(pattern[3]) && !stream.eat(";").isEmpty();
					}
				} else {
					ok = stream.eatWhile(pattern[4]) && !stream.eat(";").isEmpty();
				}
				return ok ? "atom" : "error";
			} else {
				stream.eatWhile(pattern[5]);
				return "";
			}
		}
	}
	
	class InTag implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			String ch = stream.next();
			if (ch.equals(">") || (ch.equals("/") && !stream.eat(">").isEmpty())) {
				state.tokenize = new InText();
				state.type = ch.equals(">") ? "endTag" : "selfcloseTag";
				return "tag bracket";
			} else if (ch.equals("=")) {
				state.type = "equals";
				return "";
			} else if (ch.equals("<")) {
				state.tokenize = new InText();
				state.state = new BaseState();
				state.tagName = "";
				state.tagStart = 0;
				String next = state.tokenize.process(stream, state);
				return next + " tag error";
			} else if (pattern[6].matcher(ch).matches()) {
				state.tokenize = new InAttribute(ch);
				return state.tokenize.process(stream, state);
			} else {
				stream.match(pattern[7]);
				return "word";
			}
		}
	}
	
	class InAttribute implements Processor {
		String quote;
		
		public InAttribute(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state) {
			while (!stream.eol()) {
				if (stream.next().equals(quote)) {
					state.tokenize = new InTag();
					break;
				}
			}
			return "string";
		}
	}
	
	class InBlock implements Processor {
		String style;
		String terminator;
		
		public InBlock(String style, String terminator) {
			this.style = style;
			this.terminator = terminator;
		}

		@Override
		public String process(StringStream stream, State state) {
			while (!stream.eol()) {
				if (stream.match(terminator)) {
					state.tokenize = new InText();
					break;
				}
				stream.next();
			}
			return style;
		}
	}
	
	class Doctype implements Processor {
		int depth;
		
		public Doctype(int depth) {
			this.depth = depth;
		}

		@Override
		public String process(StringStream stream, State state) {
			String ch;
			while (!(ch = stream.next()).isEmpty()) {
				if (ch.equals("<")) {
					state.tokenize = new Doctype(depth + 1);
					return state.tokenize.process(stream, state);
				} else if (ch.equals(">")) {
					if (depth == 1) {
						state.tokenize = new InText();
						break;
					} else {
						state.tokenize = new Doctype(depth - 1);
						return state.tokenize.process(stream, state);
					}
				}
			}
			return "meta";
		}
	}
	
	static void popContext(State state) {
		if (state.context != null) {
			state.context = state.context.prev;
		}
	}
	
	void maybePopContext(State state, String nextTagName) {
		String parentTagName;
		while (true) {
			if (state.context == null) {
				return;
			}
			parentTagName = state.context.tagName;
			if (!config.contextGrabbers.containsKey(parentTagName) ||
					!config.contextGrabbers.get(parentTagName).contains(nextTagName)) {
				return;
			}
			popContext(state);
		}
	}
	
	class BaseState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (type.equals("openTag")) {
				state.tagStart = stream.column();
				return new TagNameState();
			} else if (type.equals("closeTag")) {
				return new CloseTagNameState();
			} else {
				return new BaseState();
			}
		}
	}
	
	class TagNameState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (type.equals("word")) {
				state.tagName = stream.current();
				state.setStyle = "tag";
				return new AttrState();
			} else {
				state.setStyle = "error";
				return new TagNameState();
			}
		}
	}
	
	class CloseTagNameState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (type.equals("word")) {
				String tagName = stream.current();
				if (state.context != null && !state.context.tagName.equals(tagName) &&
						config.implicitlyClosed.contains(state.context.tagName)) {
					popContext(state);
				}
				if ((state.context != null && state.context.tagName.equals(tagName)) ||
						config.matchClosing == false) {
					state.setStyle = "tag";
					return new CloseState();
				} else {
					state.setStyle = "tag error";
					return new CloseStateErr();
				}
			} else {
				state.setStyle = "error";
				return new CloseStateErr();
			}
		}
	}
	
	class CloseState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (!type.equals("endTag")) {
				state.setStyle = "error";
				return new CloseState();
			}
			popContext(state);
			return new BaseState();
		}
	}
	
	class CloseStateErr implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			state.setStyle = "error";
			return new CloseState().process(type, stream, state);
		}
	}
	
	class AttrState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (type.equals("word")) {
				state.setStyle = "attribute";
				return new AttrEqState();
			} else if (type.equals("endTag") || type.equals("selfcloseTag")) {
				String tagName = state.tagName;
				int tagStart = state.tagStart;
				state.tagName = "";
				state.tagStart = 0;
				if (type.equals("selfcloseTag") || config.autoSelfClosers.contains(tagName)) {
					maybePopContext(state, tagName);
				} else {
					maybePopContext(state, tagName);
					state.context = new Context(state, tagName, tagStart == state.indented);
				}
				return new BaseState();
			}
			state.setStyle = "error";
			return new AttrState();
		}
	}
	
	class AttrEqState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (type.equals("equals"))
				return new AttrValueState();
			if (!config.allowMissing)
				state.setStyle = "error";
			return new AttrState().process(type, stream, state);
		}
	}
	
	class AttrValueState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (type.equals("string"))
				return new AttrContinuedState();
			if (type.equals("word") && config.allowUnquoted) {
				state.setStyle = "string";
				return new AttrState();
			}
			state.setStyle = "error";
			return new AttrState().process(type, stream, state);
		}
	}
	
	class AttrContinuedState implements StateInterface {

		@Override
		public StateInterface process(String type, StringStream stream, State state) {
			if (type.equals("string"))
				return new AttrContinuedState();
			return new AttrState().process(type, stream, state);
		}
	}
	
	public void skipAttribute(State state) {
		if (state.state instanceof AttrValueState)
			state.state = new AttrState();
	}
	
	public String getTagName(State state) {
		return state.tagName;
	}

	@Override
	public boolean accept(String fileName) {
		// this tokenizer should only process xml files to avoid conflicting with HtmlMixedTokenizer
		return acceptExtensions(fileName, "xml", "xsl", "xsd");
	}

	@Override
	public State startState() {
		return new State(new InText(), null, new BaseState(), "", 0, 0);
	}

	@Override
	public String token(StringStream stream, State state) {
		if (state.tagName.isEmpty() && stream.sol())
			state.indented = stream.indentation();
		
		if (stream.eatSpace())
			return "";
		state.type = "";
		String style = state.tokenize.process(stream, state);
		if ((!style.isEmpty() || !state.type.isEmpty()) && !style.equals("comment")) {
			state.setStyle = "";
			state.state = state.state.process(state.type.isEmpty() ? style : state.type, stream, state);
			if (!state.setStyle.isEmpty())
				style = state.setStyle.equals("error") ? style + " error" : state.setStyle;
		}
		return style;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && (mime.equals("text/xml") || mime.equals("application/xml"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("xml");
	}
}
