package io.onedev.commons.codeassist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.antlr.v4.runtime.Lexer;

import com.google.common.base.Optional;

import io.onedev.commons.codeassist.grammar.AlternativeSpec;
import io.onedev.commons.codeassist.grammar.ElementSpec;
import io.onedev.commons.codeassist.grammar.Grammar;
import io.onedev.commons.codeassist.grammar.TerminalElementSpec;
import io.onedev.commons.codeassist.parser.EarleyParser;
import io.onedev.commons.codeassist.parser.ParseExpect;
import io.onedev.commons.codeassist.parser.TerminalExpect;
import io.onedev.commons.utils.Range;
import io.onedev.commons.utils.StringUtils;

public abstract class CodeAssist implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Grammar grammar;
	
	private final boolean findAllPaths;
	
	public CodeAssist(Grammar grammar, boolean findAllPaths) {
		this.grammar = grammar;
		this.findAllPaths = findAllPaths;
	}
	
	/**
	 * Code assist constructor
	 * @param lexerClass
	 * 			lexer class to be used to lex code. Other required information such as 
	 * 			grammar files and token file will be derived from the lexer class
	 */
	public CodeAssist(Class<? extends Lexer> lexerClass, boolean findAllPaths) {
		this(new Grammar(lexerClass), findAllPaths);
	}

	/**
	 * Code assist constructor.
	 * 
	 * @param lexerClass
	 * 			lexer class to be used to lex code
	 * @param grammarFiles
	 * 			grammar files in class path, relative to class path root
	 * @param tokenFile
	 * 			generated tokens file in class path, relative to class path root
	 */
	public CodeAssist(Class<? extends Lexer> lexerClass, String grammarFiles[], String tokenFile, boolean findAllPaths) {
		this(new Grammar(lexerClass, grammarFiles, tokenFile), findAllPaths);
	}
	
	/*
	 * Get mandatory literals after specified element. For instance a method may have 
	 * below rule:
	 * methodName '(' argList ')'
	 * The mandatories after element methodName will be '('. When a method name is 
	 * suggested, we should add '(' and moves caret after '(' to avoid unnecessary
	 * key strokes
	 */
	private List<String> getMandatoriesAfter(ParseExpect parseExpect, ElementSpec elementSpec) {
		List<String> literals = new ArrayList<>();
		if (!elementSpec.isMultiple() || elementSpec.scanMandatories().isSpaceSeparator()) {
			AlternativeSpec alternativeSpec = parseExpect.getState().getAlternativeSpec();
			int specIndex = alternativeSpec.getElements().indexOf(elementSpec);
			if (specIndex == alternativeSpec.getElements().size()-1) {
				parseExpect = parseExpect.getParent();
				if (parseExpect != null) {
					elementSpec = parseExpect.getElementSpec();
					return getMandatoriesAfter(parseExpect, elementSpec);
				}
			} else {
				elementSpec = alternativeSpec.getElements().get(specIndex+1);
				if (!elementSpec.isOptional()) {
					LiteralScan scan = elementSpec.scanMandatories();
					literals = scan.getLiterals();
					if (!scan.isStop())
						literals.addAll(getMandatoriesAfter(parseExpect, elementSpec));
				}
			}
		} 
		return literals;
	}	
	
	public List<String> getHints(InputStatus inputStatus, String ruleName) {
		String text = inputStatus.getContentBeforeCaret();
		EarleyParser parser = new EarleyParser(grammar, ruleName, text);
		
		Set<String> hints = new LinkedHashSet<>();
		for (TerminalExpect terminalExpect: parser.buildParseExpects(findAllPaths)) 
			hints.addAll(getHints(terminalExpect));
		return new ArrayList<>(hints);
	}

	public List<InputCompletion> suggest(InputStatus inputStatus, String ruleName) {
		List<ExtendedInputSuggestion> extendedSuggestions = new ArrayList<>();
		
		String text = inputStatus.getContentBeforeCaret();
		EarleyParser parser = new EarleyParser(grammar, ruleName, text);
		
		String inputContent = inputStatus.getContent();
		for (TerminalExpect terminalExpect: parser.buildParseExpects(findAllPaths)) {
			List<InputSuggestion> inputSuggestions = suggest(terminalExpect);
			if (inputSuggestions == null) {
				// no suggestions, let's see if we can provide some default suggestions 
				inputSuggestions = new ArrayList<>();
				TerminalElementSpec expectedElementSpec = terminalExpect.getElementSpec();
				String matchWith = terminalExpect.getUnmatchedText();
				for (String prefix: expectedElementSpec.scanPrefixes().getLiterals()) {
					if (prefix.toLowerCase().startsWith(matchWith.toLowerCase())) {
						Range match = new Range(0, matchWith.length());
						Optional<String> description = describe(terminalExpect, prefix);
						if (description != null)
							inputSuggestions.add(new InputSuggestion(prefix, -1, description.orNull(), match));
					}						
				}
			}
			
			int replaceBegin = inputStatus.getCaret() - terminalExpect.getUnmatchedText().length();
			int replaceEnd = inputStatus.getCaret();
			String contentBeforeReplaceBegin = inputContent.substring(0, replaceBegin);
			String contentAfterReplaceBegin = inputContent.substring(replaceBegin);
			
			TerminalElementSpec elementSpec = terminalExpect.getElementSpec();

			int endOfMatch = elementSpec.getEndOfMatch(grammar, contentAfterReplaceBegin);
			if (endOfMatch == -1) {
				LiteralScan scan = elementSpec.scanMandatories();
				if (scan.isStop() && !scan.getLiterals().isEmpty()) { 
					/*
					 * Below logic checks if there is an empty fence, for instance "". If fence spec 
					 * does not match an empty fence, we insert a space to see if it matches 
					 */
					String fenceOpen = scan.getLiterals().iterator().next();
					if (contentAfterReplaceBegin.startsWith(fenceOpen)) {
						String content = fenceOpen + " " + contentAfterReplaceBegin.substring(fenceOpen.length());
						endOfMatch = elementSpec.getEndOfMatch(grammar, content);
						if (endOfMatch != -1)
							endOfMatch--;
					}
				}
			}
			if (endOfMatch + replaceBegin > replaceEnd)
				replaceEnd = endOfMatch + replaceBegin;
			
			List<String> mandatoryList = getMandatoriesAfter(terminalExpect, elementSpec);
			for (InputSuggestion inputSuggestion: inputSuggestions) {
				String replaceContent = inputSuggestion.getContent();
				Range replaceRange = new Range(replaceBegin, replaceEnd);
				if (grammar.canAppend(contentBeforeReplaceBegin, replaceContent)) { 
					String mandatories = "";
					if (elementSpec.matches(grammar, replaceContent) && replaceEnd == inputContent.length()) {
						String content = replaceContent;
						for (String mandatory: mandatoryList) {
							if (grammar.canAppend(content, mandatory))
								content += mandatory;
							else if (grammar.canAppend(content + " ", mandatory))
								content += " " + mandatory;
							else
								break;
						}
						mandatories = content.substring(replaceContent.length());
					} 
					extendedSuggestions.add(new ExtendedInputSuggestion(replaceRange, replaceContent, 
							inputSuggestion.getCaret(), inputSuggestion.getDescription(), 
							inputSuggestion.getMatch(), mandatories));
				} else if (grammar.canAppend(contentBeforeReplaceBegin + " ", replaceContent)) {
					extendedSuggestions.add(new ExtendedInputSuggestion(replaceRange, " ", 1, 
							"space", null, ""));
				}
			}
			
		}
		
		Map<String, List<ExtendedInputSuggestion>> contentSuggestions = new LinkedHashMap<>();
		for (ExtendedInputSuggestion suggestion: extendedSuggestions) {
			String contentBeforeReplaceBegin = inputStatus.getContent().substring(0, suggestion.getReplaceRange().getFrom());
			if (suggestion.getContent().equals(" ") 
					&& (contentBeforeReplaceBegin.length() == 0 || contentBeforeReplaceBegin.endsWith(" "))) {
				// do not suggest redundant spaces even if grammar allows
				continue;
			}
			String content = inputStatus.replace(suggestion.getReplaceRange(), suggestion.getContent());
			List<ExtendedInputSuggestion> value = contentSuggestions.get(content);
			if (value == null) {
				value = new ArrayList<>();
				contentSuggestions.put(content, value);
			}
			value.add(suggestion);
		}
		
		List<InputCompletion> inputCompletions = new ArrayList<>();
		
		Set<String> duplicateContents = new HashSet<>();
		
		for (Map.Entry<String, List<ExtendedInputSuggestion>> entry: contentSuggestions.entrySet()) {
			Set<String> mandatoriesSet = new HashSet<>();
			for (ExtendedInputSuggestion suggestion: entry.getValue())
				mandatoriesSet.add(suggestion.getMandatories());

			for (ExtendedInputSuggestion completion: entry.getValue()) {
				String replaceContent = completion.getContent();
				if (mandatoriesSet.size() == 1)  
					replaceContent += mandatoriesSet.iterator().next();
				String content = inputStatus.replace(completion.getReplaceRange(), replaceContent);
				if (!duplicateContents.contains(content)) {
					duplicateContents.add(content);
					int caret = completion.getCaret();
					if (caret == -1)
						caret = content.length();
					else
						caret += completion.getReplaceRange().getFrom();
					inputCompletions.add(new InputCompletion(completion.getContent(), content, caret, 
							completion.getDescription(), completion.getMatch()));
				}
			}
			
		}
		
		return inputCompletions;
	}
	
	public Grammar getGrammar() {
		return grammar;
	}

	@Nullable
	protected abstract List<InputSuggestion> suggest(TerminalExpect terminalExpect);
	
	protected List<String> getHints(TerminalExpect terminalExpect) {
		return new ArrayList<>();
	}

	/**
	 * Wrap specified literal of specified terminal element as suggestion.
	 * 
	 * @param terminalExpect
	 * 			the node 
	 * @param suggestedLiteral
	 * 			a proposed literal of above terminal element defined in grammar
	 * @return
	 * 			suggestion of the literal, or <tt>null</tt> to suppress this suggestion
	 */
	@Nullable
	protected Optional<String> describe(TerminalExpect terminalExpect, String suggestedLiteral) {
		if (StringUtils.isNotBlank(suggestedLiteral)) 
			return Optional.absent();
		else 
			return Optional.of("space");
	}
	
}
