package io.onedev.commons.codeassist;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;

import io.onedev.commons.codeassist.grammar.Grammar;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.utils.LinearRange;

public abstract class FenceAware {
	
	private final Grammar grammar;
	
	private final String open;
	
	private final String close;
	
	public FenceAware(Grammar grammar, String open, String close) {
		this.grammar = grammar;
		this.open = open;
		this.close = close;
	}
	
	public List<InputSuggestion> suggest(TerminalExpect terminalExpect) {
		String matchWith = terminalExpect.getUnmatchedText();

		// Ignore this case as we can always get all the fenced suggestions when 
		// matchWith is empty
		if (matchWith.length() != 0 && matchWith.trim().length() == 0)
			return new ArrayList<>();
		
		String unfencedMatchWith = matchWith;
		if (matchWith.startsWith(open))
			unfencedMatchWith = unfencedMatchWith.substring(open.length());
		
		unfencedMatchWith = unfencedMatchWith.trim();
		
		List<InputSuggestion> suggestions = match(unfencedMatchWith);
		if (suggestions != null) {
			List<InputSuggestion> fencedSuggestions = new ArrayList<>();
			
			for (InputSuggestion suggestion: suggestions) {
				String content = suggestion.getContent();
				int caret = suggestion.getCaret();
				LinearRange match = suggestion.getMatch();
				if (!content.startsWith(open)) { 
					content = open + content + close;
					if (caret != -1) 
						caret += open.length();
					if (match != null)
						match = new LinearRange(match.getFrom()+open.length(), match.getTo()+open.length());
				}
				if (terminalExpect.getElementSpec().matches(grammar, content))				
					fencedSuggestions.add(new InputSuggestion(content, caret, suggestion.getDescription(), match));
			}
			return fencedSuggestions;
		} else {
			suggestions = new ArrayList<>();
			CollectionUtils.addIgnoreNull(suggestions, suggestToFence(terminalExpect, unfencedMatchWith));
			return suggestions;
		}
	}
	
	protected String getFencingDescription() {
		return null;
	}
	
	@Nullable 
	protected InputSuggestion suggestToFence(TerminalExpect terminalExpect, String unfencedMatchWith) {
		if (unfencedMatchWith.length() != 0) {
			String content = open + unfencedMatchWith + close;
			if (terminalExpect.getElementSpec().matches(grammar, content)) {
				LinearRange match = new LinearRange(1, content.length()-1);
				return new InputSuggestion(content, -1, getFencingDescription(), match);
			} else {
				return null;
			}
		} else {
			return new InputSuggestion(open, -1, null, null);
		}
	}
	
	/**
	 * Match with provided string to give a list of suggestions
	 * 
	 * @param unfencedMatchWith
	 * 			string with fencing literals removed 
	 * @return
	 * 			a list of suggestions. Return <tt>null</tt> to tell CodeAssist to return a default suggestion to 
	 * 			wrap your input with fences 
	 */
	@Nullable
	protected abstract List<InputSuggestion> match(String unfencedMatchWith);
}
