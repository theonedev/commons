package io.onedev.commons.codeassist;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.onedev.commons.codeassist.grammar.Grammar;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.utils.LinearRange;
import io.onedev.commons.utils.StringUtils;

public abstract class FenceAware {
	
	private final Grammar grammar;
	
	private final char open;
	
	private final char close;
	
	private final String additionalCharsToEscape;
	
	public FenceAware(Grammar grammar, char open, char close, String additionalCharsToEscape) {
		this.grammar = grammar;
		this.open = open;
		this.close = close;
		this.additionalCharsToEscape = additionalCharsToEscape;
	}
	
	public FenceAware(Grammar grammar, char open, char close) {
		this(grammar, open, close, "");
	}
	
	public static String unfence(String value) {
		value = value.substring(1);
		return value.substring(0, value.length()-1);
	}
	
	public List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
		String matchWith = terminalExpect.getUnmatchedText();

		// Ignore this case as we can always get all the fenced suggestions when 
		// matchWith is empty
		if (matchWith.length() != 0 && matchWith.trim().length() == 0) 
			return new ArrayList<>();
		
		if (matchWith.startsWith(String.valueOf(open)))
			matchWith = matchWith.substring(1);
		
		matchWith = matchWith.trim();
		
		List<InputSuggestion> suggestions = match(StringUtils.unescape(matchWith));
		if (suggestions != null) {
			List<InputSuggestion> fencedSuggestions = new ArrayList<>();
			
			for (InputSuggestion suggestion: suggestions) {
				suggestion = suggestion.escape(String.valueOf(open) + String.valueOf(close) + additionalCharsToEscape);
				String content = suggestion.getContent();
				int caret = suggestion.getCaret();
				LinearRange match = suggestion.getMatch();
				content = open + content + close;
				if (caret != -1) 
					caret ++;
				if (match != null)
					match = new LinearRange(match.getFrom()+1, match.getTo()+1);
				if (terminalExpect.getElementSpec().matches(grammar, content))				
					fencedSuggestions.add(new InputSuggestion(content, caret, suggestion.getDescription(), match));
			}
			return fencedSuggestions;
		} else {
			suggestions = new ArrayList<>();
			InputSuggestion suggestion = suggestToFence(terminalExpect, matchWith);
			if (suggestion != null)
				suggestions.add(suggestion);
			return suggestions;
		}
	}
	
	protected String getFencingDescription() {
		return null;
	}
	
	private InputSuggestion suggestToFence(TerminalExpect terminalExpect, String matchWith) {
		if (matchWith.length() != 0) {
			String content = open + matchWith + close;
			if (terminalExpect.getElementSpec().matches(grammar, content))
				return new InputSuggestion(content, -1, getFencingDescription(), null);
			else
				return null;
		} else {
			return new InputSuggestion(String.valueOf(open), -1, null, null);
		}
	}
	
	/**
	 * Match with provided string to give a list of suggestions
	 * 
	 * @param matchWith
	 * 			string with fencing literals removed 
	 * @return
	 * 			a list of suggestions. Return <tt>null</tt> to tell CodeAssist to return a default suggestion to 
	 * 			wrap your input with fences 
	 */
	@Nullable
	protected abstract List<InputSuggestion> match(String matchWith);
}
