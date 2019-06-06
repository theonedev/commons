package io.onedev.commons.jsyntax.verilog;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.TokenizerUtils;

@SuppressWarnings("unchecked")
public class TLVerilogTokenizer extends VerilogTokenizer {

	static final int tlvIndentUnit = 3;
	static final Pattern tlvIdentMatch = Pattern.compile("^([~!@#\\$%\\^&\\*-\\+=\\?\\/\\\\\\|'\"<>]+)([\\d\\w_]*)");
	static final Pattern tlvFirstLevelIndentMatch = Pattern.compile("^[! ]  ");
	static final Pattern tlvLineIndentationMatch = Pattern.compile("^[! ] *");
	static final Pattern tlvCommentMatch = Pattern.compile("^\\/[\\/\\*]");

	static final Pattern pattern[] = new Pattern[10];
	static {
		pattern[0] = Pattern.compile("\\\\SV");
		pattern[1] = Pattern.compile("\\\\TLV");
		pattern[2] = Pattern.compile("^   ");
		pattern[3] = Pattern.compile("^.*?\\*\\/");
		pattern[4] = Pattern.compile("^\\t+");
		pattern[5] = Pattern.compile("^[\\[\\]{}\\(\\);\\:]+");
		pattern[6] = Pattern.compile("^[mM]4([\\+_])?[\\w\\d_]*");
		pattern[7] = Pattern.compile("^ +");
		pattern[8] = Pattern.compile("^[\\w\\d_]+");
		pattern[9] = Pattern.compile("^[mM]4([\\w\\d_]*)");
	}

	static final Map<String, String> tlvIdentifierStyle = Maps.newHashMap();
	static final Map<String, String> tlvScopePrefixChars = Maps.newHashMap();

	static final Map<String, Processor> hooks = Maps.newHashMap();

	static {
		tlvIdentifierStyle.put("|", "link");
		tlvIdentifierStyle.put(">", "property");
		tlvIdentifierStyle.put("$", "variable");
		tlvIdentifierStyle.put("$$", "variable");
		tlvIdentifierStyle.put("?$", "qualifier");
		tlvIdentifierStyle.put("?*", "qualifier");
		tlvIdentifierStyle.put("-", "hr");
		tlvIdentifierStyle.put("/", "property");
		tlvIdentifierStyle.put("/-", "property");
		tlvIdentifierStyle.put("@", "variable-3");
		tlvIdentifierStyle.put("@-", "variable-3");
		tlvIdentifierStyle.put("@++", "variable-3");
		tlvIdentifierStyle.put("@+=", "variable-3");
		tlvIdentifierStyle.put("@+=-", "variable-3");
		tlvIdentifierStyle.put("@--", "variable-3");
		tlvIdentifierStyle.put("@-=", "variable-3");
		tlvIdentifierStyle.put("%+", "tag");
		tlvIdentifierStyle.put("%-", "tag");
		tlvIdentifierStyle.put("%", "tag");
		tlvIdentifierStyle.put(">>", "tag");
		tlvIdentifierStyle.put("<<", "tag");
		tlvIdentifierStyle.put("<>", "tag");
		tlvIdentifierStyle.put("#", "tag");
		tlvIdentifierStyle.put("^", "attribute");
		tlvIdentifierStyle.put("^^", "attribute");
		tlvIdentifierStyle.put("^!", "attribute");
		tlvIdentifierStyle.put("*", "variable-2");
		tlvIdentifierStyle.put("**", "variable-2");
		tlvIdentifierStyle.put("\\", "keyword");
		tlvIdentifierStyle.put("\"", "comment");

		tlvScopePrefixChars.put("/", "beh-hier");
		tlvScopePrefixChars.put(">", "beh-hier");
		tlvScopePrefixChars.put("-", "phys-hier");
		tlvScopePrefixChars.put("|", "pipe");
		tlvScopePrefixChars.put("?", "when");
		tlvScopePrefixChars.put("@", "stage");
		tlvScopePrefixChars.put("\\", "keyword");
		
		hooks.put("token", (stream, state) -> {
		    String style = "undefined";
		    List<String> match;
		    if (stream.sol() && !(Boolean)state.attr.get("tlvInBlockComment")) {
		        if (stream.peek().equals("\\")) {
		            style = "def";
		            stream.skipToEnd();
		            if (pattern[0].matcher(stream.string()).matches()) {
		                state.attr.replace("tlvCodeActive", Boolean.FALSE);
		            }
		            else if (pattern[1].matcher(stream.string()).matches()) {
		            	state.attr.replace("tlvCodeActive", Boolean.TRUE);
		            }
		        }
		        int indented = 0;
		        if ((Boolean)state.attr.get("tlvCodeActive") && stream.pos() == 0 && !(match = stream.match(tlvLineIndentationMatch, false)).isEmpty()) {
		            indented = match.get(0).length();
		        }
		        int depth = indented / tlvIndentUnit;
		        
		        if (depth <= ((Stack<String>)state.attr.get("tlvIndentationStyle")).size()) {
		            boolean blankline = stream.string().length() == indented;
		            int chPos = depth * tlvIndentUnit;
		            if (chPos < stream.string().length()) {
		                String bodyString = TokenizerUtils.slice(stream.string(), chPos);
		                String ch = bodyString.charAt(0) + "";
		                Matcher mat = tlvIdentMatch.matcher(bodyString);
		                if (tlvScopePrefixChars.containsKey(ch) && (mat.find() && tlvIdentifierStyle.containsKey(mat.group(1)))) {
		                    indented += tlvIndentUnit;
		                    if (!(ch.equals("\\") && chPos > 0)) {
		                    	if (depth < ((Stack<String>)state.attr.get("tlvIndentationStyle")).size()) {
		                    		((Stack<String>)state.attr.get("tlvIndentationStyle")).set(depth, tlvScopePrefixChars.get(ch));
		                    	}
		                        depth++;
		                    }
		                }
		            }
		            if (!blankline) {
		                while (((Stack<String>)state.attr.get("tlvIndentationStyle")).size() > depth) {
		                	((Stack<String>)state.attr.get("tlvIndentationStyle")).pop();
		                }
		            }
		        }
		        state.attr.replace("tlvNextIndent", indented);
		    }
		    if ((Boolean)state.attr.get("tlvCodeActive")) {
		        boolean beginStatement = false;
		        if (!style.equals("undefined")) {
		            style += " " + tlvScopeStyle(state, 0, "scope-ident");
		        }
		        else if (((stream.pos() / tlvIndentUnit) < ((Stack<String>)state.attr.get("tlvIndentationStyle")).size())
		        		&& !(match = stream.match(stream.sol() ? tlvFirstLevelIndentMatch : pattern[2])).isEmpty()) {
		            style = "tlv-indent-" + (((stream.pos() % 2) == 0) ? "even" : "odd") + " " + tlvScopeStyle(state, stream.pos() - tlvIndentUnit, "indent");
		            if (match.get(0).charAt(0) == '!') {
		                style += " tlv-alert-line-prefix";
		            }
		            if (tlvIdentNext(stream)) {
		                style += " " + tlvScopeStyle(state, stream.pos(), "before-scope-ident");
		            }
		        }
		        else if ((Boolean)state.attr.get("tlvInBlockComment")) {
		            if (!stream.match(pattern[3]).isEmpty()) {
		                state.attr.replace("tlvInBlockComment", Boolean.FALSE);
		            }
		            else {
		                stream.skipToEnd();
		            }
		            style = "comment";
		        }
		        else if (!(match = stream.match(tlvCommentMatch)).isEmpty() && !(Boolean)state.attr.get("tlvInBlockComment")) {
		            if (match.get(0).equals("//")) {
		                stream.skipToEnd();
		            }
		            else {
		            	state.attr.replace("tlvInBlockComment", Boolean.TRUE);
		            }
		            style = "comment";
		        }
		        else if (!(match = stream.match(tlvIdentMatch)).isEmpty()) {
		            String prefix = match.get(1);
		            String mnemonic = match.size() > 2 ? match.get(2) : "";
		            if (tlvIdentifierStyle.containsKey(prefix) && (mnemonic.length() > 0 || stream.eol())) {
		                style = tlvIdentifierStyle.get(prefix);
		                if (stream.column() == 0) {
		                    style += " " + tlvScopeStyle(state, stream.column(), "scope-ident");
		                } 
		            } else {
		                stream.backUp(stream.current().length() - 1);
		                style = "tlv-default";
		            }
		        }
		        else if (!stream.match(pattern[4]).isEmpty()) {
		            style = "tlv-tab";
		        }
		        else if (!stream.match(pattern[5]).isEmpty()) {
		            style = "meta";
		        }
		        else if (!(match = stream.match(pattern[6])).isEmpty()) {
		            style = match.get(1).equals("+") ? "tlv-m4-plus" : "tlv-m4";
		        }
		        else if (!stream.match(pattern[7]).isEmpty()) {
		            if (stream.eol()) {
		                style = "error";
		            }
		            else {
		                style = "tlv-default";
		            }
		        }
		        else if (!stream.match(pattern[8]).isEmpty()) {
		            style = "number";
		        }
		        else {
		            stream.next();
		            style = "tlv-default";
		        }
		        if (beginStatement) {
		            style += " tlv-statement";
		        }
		    }
		    else {
		        if (!stream.match(pattern[9]).isEmpty()) {
		            style = "tlv-m4";
		        }
		    }
		    return style;
		});
	}

	static String tlvScopeStyle(State state, int indentation, String type) {
		int depth = indentation / tlvIndentUnit;
		if (depth < ((Stack<String>) state.attr.get("tlvIndentationStyle")).size())
			return "tlv-" + ((Stack<String>) state.attr.get("tlvIndentationStyle")).get(depth) + "-" + type;
		else
			return "tlv-" + type;
	}

	static boolean tlvIdentNext(StringStream stream) {
		List<String> match = stream.match(tlvIdentMatch, false);
		return match.size() > 2 && match.get(2).length() > 0;
	}

	@Override
	protected Map<String, Processor> hooks() {
		return hooks;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-tlv");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("tlverilog");
	}

	@Override
	public State startState() {
		State state = super.startState();
		state.attr.put("tlvIndentationStyle", new Stack<String>());
		state.attr.put("tlvCodeActive", Boolean.TRUE);
		state.attr.put("tlvNextIndent", Integer.valueOf(-1));
		state.attr.put("tlvInBlockComment", Boolean.FALSE);
		return state;
	}
}
