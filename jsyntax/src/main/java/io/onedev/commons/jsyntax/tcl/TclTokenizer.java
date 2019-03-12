package io.onedev.commons.jsyntax.tcl;


import java.util.Set;
import java.util.regex.Pattern;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class TclTokenizer extends AbstractTokenizer<TclTokenizer.State> {

	static class State {
		Processor tokenize;
		boolean beforeParams;
		boolean inParams;

		public State(Processor tokenize, boolean beforeParams, boolean inParams) {
			this.tokenize = tokenize;
			this.beforeParams = beforeParams;
			this.inParams = inParams;
		}
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}

	static Set<String> keywords = wordsOf("Tcl safe after append array auto_execok auto_import auto_load "
			+ "auto_mkindex auto_mkindex_old auto_qualify auto_reset bgerror "
			+ "binary break catch cd close concat continue dde eof encoding error "
			+ "eval exec exit expr fblocked fconfigure fcopy file fileevent filename "
			+ "filename flush for foreach format gets glob global history http if "
			+ "incr info interp join lappend lindex linsert list llength load lrange "
			+ "lreplace lsearch lset lsort memory msgcat namespace open package parray "
			+ "pid pkg::create pkg_mkIndex proc puts pwd re_syntax read regex regexp "
			+ "registry regsub rename resource return scan seek set socket source split "
			+ "string subst switch tcl_endOfWord tcl_findLibrary tcl_startOfNextWord "
			+ "tcl_wordBreakAfter tcl_startOfPreviousWord tcl_wordBreakBefore tcltest "
			+ "tclvars tell time trace unknown unset update uplevel upvar variable " + "vwait");

	static Set<String> functions = wordsOf("if elseif else and not or eq ne in ni for foreach while switch");


	static Pattern isOperatorChar = Pattern.compile("[+\\-*&%=<>!?^\\/\\|]");
	static Pattern[] pattern = new Pattern[6];

	static {
		pattern[0] = Pattern.compile("[\\[\\]{}\\(\\),;\\.]");
		pattern[1] = Pattern.compile(" *\\[ *\\[");
		pattern[2] = Pattern.compile("[$_a-z0-9A-Z\\.{:]");
		pattern[3] = Pattern.compile("[\\w\\$_{}\\xa1-\\uffff]");
		pattern[4] = Pattern.compile("\\d");
		pattern[5] = Pattern.compile("[\\w\\.]");
	}

	protected Pattern isOperatorChar() {
		return isOperatorChar;
	}

	class tokenBase implements Processor {
		@Override
		public String process(StringStream stream, State state) {
			boolean beforeParams = state.beforeParams;
			state.beforeParams = false;
			String ch = stream.next();
			if ((ch.equals("\"") || ch.equals("'")) && state.inParams) {
				state.tokenize = new tokenString(ch);
				return state.tokenize.process(stream, state);
			} else if (pattern[0].matcher(ch).matches()) {
				if (ch.equals("(") && beforeParams)
					state.inParams = true;
				else if (ch.equals(")"))
					state.inParams = false;
				return "";
			} else if (pattern[4].matcher(ch).matches()) {
				stream.eatWhile(pattern[5]);
				return "number";
			} else if (ch.equals("#")) {
				if (stream.eat("*").length() != 0) {
					state.tokenize = new tokenComment();
					return state.tokenize.process(stream, state);
				}
				if (ch.equals("#") && (!stream.match(pattern[1]).isEmpty())) {
					state.tokenize = new tokenUnparsed();
					return state.tokenize.process(stream, state);
				}
				stream.skipToEnd();
				return "comment";
			} else if (ch.equals("\"")) {
		
				return "comment";
			} else if (ch.equals("$")) {
				stream.eatWhile(Pattern.compile("[$_a-z0-9A-Z\\.{:]"));
				stream.eatWhile(Pattern.compile("}"));
				state.beforeParams = true;
				return "builtin";
			} else if (isOperatorChar.matcher(ch).matches()) {
				stream.eatWhile(isOperatorChar());
				return "comment";
			} else {
				stream.eatWhile(pattern[3]);
				String word = stream.current().toLowerCase();
				if ((!keywords.isEmpty()) && keywords.contains(word))		
					return "keyword";
				if ((!functions.isEmpty()) && functions.contains(word)) {
					state.beforeParams = true;
					return "keyword";
				}
				return "";
			}
		}

	}

	class tokenString implements Processor {
		private String quote;

		public tokenString(String quote) {
			this.quote = quote;
		}

		@Override
		public String process(StringStream stream, State state) {
			// TODO Auto-generated method stub
			boolean escaped = false;
			String next;
			boolean end = false;

			while ((next = stream.next()).length()!=0) {
				if (next.equals(quote) && !escaped) {
					end = true;
					break;
				}
				escaped = !escaped && next.equals("\\");
			}
			if (end) state.tokenize = new tokenBase();
			return "string";
		}
	}

	class tokenComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			boolean maybeEnd = false;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals("#") && maybeEnd) {
					state.tokenize = new tokenBase();
					break;
				}
				maybeEnd = ch.equals("*");
			}
			return "comment";
		}
	}

	class tokenUnparsed implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			int maybeEnd = 0;
			String ch;
			while ((ch = stream.next()).length() != 0) {
				if (ch.equals("#") && maybeEnd == 2) {
					state.tokenize = new tokenBase();
					break;
				}
				if (ch.equals("]"))
					maybeEnd++;
				else if (!ch.equals(" "))
					maybeEnd = 0;
			}
			return "meta";
		}
	}

	@Override
	public boolean accept(String fileName) {	
		return acceptExtensions(fileName, "tcl");
	}

	@Override
	public State startState() {	
		return new State(new tokenBase(), false, false);
	}

	@Override
	public String token(StringStream stream, State state) {
		
		if (stream.eatSpace())return "";
		return state.tokenize.process(stream, state);
		
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-tcl");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("tcl");
	}
}