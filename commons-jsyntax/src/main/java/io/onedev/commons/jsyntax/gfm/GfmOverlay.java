package io.onedev.commons.jsyntax.gfm;

import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;
import io.onedev.commons.jsyntax.TokenizerUtils;

public class GfmOverlay extends AbstractTokenizer<GfmOverlay.State> {

	static final Pattern urlRE = Pattern.compile("^((?:(?:aaas?|about|acap|adiumxtra|af[ps]|aim|apt|attachment|aw|beshare|bitcoin|bolo|callto|cap|chrome(?:-extension)?|cid|coap|com-eventbrite-attendee|content|crid|cvs|data|dav|dict|dlna-(?:playcontainer|playsingle)|dns|doi|dtn|dvb|ed2k|facetime|feed|file|finger|fish|ftp|geo|gg|git|gizmoproject|go|gopher|gtalk|h323|hcp|https?|iax|icap|icon|im|imap|info|ipn|ipp|irc[6s]?|iris(?:\\.beep|\\.lwz|\\.xpc|\\.xpcs)?|itms|jar|javascript|jms|keyparc|lastfm|ldaps?|magnet|mailto|maps|market|message|mid|mms|ms-help|msnim|msrps?|mtqp|mumble|mupdate|mvn|news|nfs|nih?|nntp|notes|oid|opaquelocktoken|palm|paparazzi|platform|pop|pres|proxy|psyc|query|res(?:ource)?|rmi|rsync|rtmp|rtsp|secondlife|service|session|sftp|sgn|shttp|sieve|sips?|skype|sm[bs]|snmp|soap\\.beeps?|soldat|spotify|ssh|steam|svn|tag|teamspeak|tel(?:net)?|tftp|things|thismessage|tip|tn3270|tv|udp|unreal|urn|ut2004|vemmi|ventrilo|view-source|webcal|wss?|wtai|wyciwyg|xcon(?:-userid)?|xfire|xmlrpc\\.beeps?|xmpp|xri|ymsgr|z39\\.50[rs]?):(?:\\/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}\\/)(?:[^\\s()<>]|\\([^\\s()<>]*\\))+(?:\\([^\\s()<>]*\\)|[^\\s`*!()\\[\\]{};:'\".,<>?«»“”‘’]))", Pattern.CASE_INSENSITIVE);
	final boolean gitHubSpice;
	
	public GfmOverlay(boolean gitHubSpice) {
		this.gitHubSpice = gitHubSpice;
	}
	
	public GfmOverlay() {
		this(true);
	}
	
	static class State {
		boolean code;
		boolean codeBlock;
		boolean ateSpace;
		int codeDepth;
		
		public State() {
			this.code = false;
			this.codeBlock = false;
			this.ateSpace = false;
			this.codeDepth = 0;
		}
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
	public State startState() {
		return new State();
	}
	
	static final Pattern pattern[] = new Pattern[5];
	static {
	    pattern[0] = Pattern.compile("^```+");
	    pattern[1] = Pattern.compile("^```+");
	    pattern[2] = Pattern.compile("^(?:[a-zA-Z0-9\\-_]+\\/)?(?:[a-zA-Z0-9\\-_]+@)?(?:[a-f0-9]{7,40}\\b)");
	    pattern[3] = Pattern.compile("^(?:[a-zA-Z0-9\\-_]+\\/)?(?:[a-zA-Z0-9\\-_]+)?#[0-9]+\\b");
	    pattern[4] = Pattern.compile("\\W");
	}

	@Override
	public String token(StringStream stream, State state) {
	    if (state.codeBlock) {
	        if (!stream.match(pattern[0]).isEmpty()) {
	            state.codeBlock = false;
	            return "";
	        }
	        stream.skipToEnd();
	        return "";
	    }
	    if (stream.sol()) {
	        state.code = false;
	    }
	    if (stream.sol() && !stream.match(pattern[1]).isEmpty()) {
	        stream.skipToEnd();
	        state.codeBlock = true;
	        return "";
	    }
	    if (stream.peek().equals("`")) {
	        stream.next();
	        int before = stream.pos();
	        stream.eatWhile("`");
	        int difference = 1 + stream.pos() - before;
	        if (!state.code) {
	            state.codeDepth = difference;
	            state.code = true;
	        }
	        else {
	            if (difference == state.codeDepth) {
	                state.code = false;
	            }
	        }
	        return "";
	    }
	    else if (state.code) {
	        stream.next();
	        return "";
	    }
	    if (stream.eatSpace()) {
	        state.ateSpace = true;
	        return "";
	    }
	    if (stream.sol() || state.ateSpace) {
	        state.ateSpace = false;
	        if (gitHubSpice) {
	            if (!stream.match(pattern[2]).isEmpty()) {
	                return "link";
	            }
	            else if (!stream.match(pattern[3]).isEmpty()) {
	                return "link";
	            }
	        }
	    }
	    if (!stream.match(urlRE).isEmpty()
	    		&& !TokenizerUtils.slice(stream.string(), stream.start() - 2, stream.start()).equals("](")
	    		&& (stream.start() == 0
	    		|| pattern[4].matcher(stream.string().charAt(stream.start() - 1) + "").matches())) {
	        return "link";
	    }
	    stream.next();
	    return "";
	}
}
