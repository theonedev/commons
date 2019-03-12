package io.onedev.commons.jsyntax.powershell;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class PowerShellTokenizer extends AbstractTokenizer<PowerShellTokenizer.State> {

	static final String notCharacterOrDash = "(?=[^A-Za-z\\d\\-_]|$)";
	static final Pattern varNames = Pattern.compile("[\\w\\-:]");
	static final Pattern keywords = buildRegexp(Lists.newArrayList(
			"begin|break|catch|continue|data|default|do|dynamicparam",
			"else|elseif|end|exit|filter|finally|for|foreach|from|function|if|in",
			"param|process|return|switch|throw|trap|try|until|where|while"), null, notCharacterOrDash);
	
	static final Pattern punctuation = Pattern.compile("[\\[\\]{},;`\\.]|@[({]");
	static final String wordOperators = buildRegexpString(Lists.newArrayList(
			"f",
			"b?not",
			"[ic]?split", "join",
			"is(not)?", "as",
			"[ic]?(eq|ne|[gl][te])",
			"[ic]?(not)?(like|match|contains)",
			"[ic]?replace",
			"b?(and|or|xor)"), "-", null);
	static final String symbolOperators = "[+\\-*\\/%]=|\\+\\+|--|\\.\\.|[+\\-*&^%:=!|\\/]|<(?!#)|(?!#)>";
	static final Pattern operators = buildRegexp(Lists.newArrayList(wordOperators, symbolOperators), null, "");
	
	static final Pattern numbers = Pattern.compile("^((0x[\\da-f]+)|((\\d+\\.\\d+|\\d\\.|\\.\\d+|\\d+)(e[\\+\\-]?\\d+)?))[ld]?([kmgtp]b)?", Pattern.CASE_INSENSITIVE);
	
	static final Pattern identifiers = Pattern.compile("^[A-Za-z\\_][A-Za-z\\-\\_\\d]*\\b");
	
	static final String symbolBuiltins = "[A-Z]:|%|\\?";
	static final String namedBuiltins = buildRegexpString(Lists.newArrayList(
			   "Add-(Computer|Content|History|Member|PSSnapin|Type)",
			    "Checkpoint-Computer",
			    "Clear-(Content|EventLog|History|Host|Item(Property)?|Variable)",
			    "Compare-Object",
			    "Complete-Transaction",
			    "Connect-PSSession",
			    "ConvertFrom-(Csv|Json|SecureString|StringData)",
			    "Convert-Path",
			    "ConvertTo-(Csv|Html|Json|SecureString|Xml)",
			    "Copy-Item(Property)?",
			    "Debug-Process",
			    "Disable-(ComputerRestore|PSBreakpoint|PSRemoting|PSSessionConfiguration)",
			    "Disconnect-PSSession",
			    "Enable-(ComputerRestore|PSBreakpoint|PSRemoting|PSSessionConfiguration)",
			    "(Enter|Exit)-PSSession",
			    "Export-(Alias|Clixml|Console|Counter|Csv|FormatData|ModuleMember|PSSession)",
			    "ForEach-Object",
			    "Format-(Custom|List|Table|Wide)",
			    "Get-(Acl|Alias|AuthenticodeSignature|ChildItem|Command|ComputerRestorePoint|Content|ControlPanelItem|Counter|Credential"
			      + "|Culture|Date|Event|EventLog|EventSubscriber|ExecutionPolicy|FormatData|Help|History|Host|HotFix|Item|ItemProperty|Job"
			      + "|Location|Member|Module|PfxCertificate|Process|PSBreakpoint|PSCallStack|PSDrive|PSProvider|PSSession|PSSessionConfiguration"
			      + "|PSSnapin|Random|Service|TraceSource|Transaction|TypeData|UICulture|Unique|Variable|Verb|WinEvent|WmiObject)",
			    "Group-Object",
			    "Import-(Alias|Clixml|Counter|Csv|LocalizedData|Module|PSSession)",
			    "ImportSystemModules",
			    "Invoke-(Command|Expression|History|Item|RestMethod|WebRequest|WmiMethod)",
			    "Join-Path",
			    "Limit-EventLog",
			    "Measure-(Command|Object)",
			    "Move-Item(Property)?",
			    "New-(Alias|Event|EventLog|Item(Property)?|Module|ModuleManifest|Object|PSDrive|PSSession|PSSessionConfigurationFile"
			      + "|PSSessionOption|PSTransportOption|Service|TimeSpan|Variable|WebServiceProxy|WinEvent)",
			    "Out-(Default|File|GridView|Host|Null|Printer|String)",
			    "Pause",
			    "(Pop|Push)-Location",
			    "Read-Host",
			    "Receive-(Job|PSSession)",
			    "Register-(EngineEvent|ObjectEvent|PSSessionConfiguration|WmiEvent)",
			    "Remove-(Computer|Event|EventLog|Item(Property)?|Job|Module|PSBreakpoint|PSDrive|PSSession|PSSnapin|TypeData|Variable|WmiObject)",
			    "Rename-(Computer|Item(Property)?)",
			    "Reset-ComputerMachinePassword",
			    "Resolve-Path",
			    "Restart-(Computer|Service)",
			    "Restore-Computer",
			    "Resume-(Job|Service)",
			    "Save-Help",
			    "Select-(Object|String|Xml)",
			    "Send-MailMessage",
			    "Set-(Acl|Alias|AuthenticodeSignature|Content|Date|ExecutionPolicy|Item(Property)?|Location|PSBreakpoint|PSDebug" +
			               "|PSSessionConfiguration|Service|StrictMode|TraceSource|Variable|WmiInstance)",
			    "Show-(Command|ControlPanelItem|EventLog)",
			    "Sort-Object",
			    "Split-Path",
			    "Start-(Job|Process|Service|Sleep|Transaction|Transcript)",
			    "Stop-(Computer|Job|Process|Service|Transcript)",
			    "Suspend-(Job|Service)",
			    "TabExpansion2",
			    "Tee-Object",
			    "Test-(ComputerSecureChannel|Connection|ModuleManifest|Path|PSSessionConfigurationFile)",
			    "Trace-Command",
			    "Unblock-File",
			    "Undo-Transaction",
			    "Unregister-(Event|PSSessionConfiguration)",
			    "Update-(FormatData|Help|List|TypeData)",
			    "Use-Transaction",
			    "Wait-(Event|Job|Process)",
			    "Where-Object",
			    "Write-(Debug|Error|EventLog|Host|Output|Progress|Verbose|Warning)",
			    "cd|help|mkdir|more|oss|prompt",
			    "ac|asnp|cat|cd|chdir|clc|clear|clhy|cli|clp|cls|clv|cnsn|compare|copy|cp|cpi|cpp|cvpa|dbp|del|diff|dir|dnsn|ebp",
			    "echo|epal|epcsv|epsn|erase|etsn|exsn|fc|fl|foreach|ft|fw|gal|gbp|gc|gci|gcm|gcs|gdr|ghy|gi|gjb|gl|gm|gmo|gp|gps",
			    "group|gsn|gsnp|gsv|gu|gv|gwmi|h|history|icm|iex|ihy|ii|ipal|ipcsv|ipmo|ipsn|irm|ise|iwmi|iwr|kill|lp|ls|man|md",
			    "measure|mi|mount|move|mp|mv|nal|ndr|ni|nmo|npssc|nsn|nv|ogv|oh|popd|ps|pushd|pwd|r|rbp|rcjb|rcsn|rd|rdr|ren|ri",
			    "rjb|rm|rmdir|rmo|rni|rnp|rp|rsn|rsnp|rujb|rv|rvpa|rwmi|sajb|sal|saps|sasv|sbp|sc|select|set|shcm|si|sl|sleep|sls",
			    "sort|sp|spjb|spps|spsv|start|sujb|sv|swmi|tee|trcm|type|where|wjb|write"), "", "");
	static final String variableBuiltins = buildRegexpString(Lists.newArrayList(
			"[$?^_]|Args|ConfirmPreference|ConsoleFileName|DebugPreference|Error|ErrorActionPreference|ErrorView|ExecutionContext",
		    "FormatEnumerationLimit|Home|Host|Input|MaximumAliasCount|MaximumDriveCount|MaximumErrorCount|MaximumFunctionCount",
		    "MaximumHistoryCount|MaximumVariableCount|MyInvocation|NestedPromptLevel|OutputEncoding|Pid|Profile|ProgressPreference",
		    "PSBoundParameters|PSCommandPath|PSCulture|PSDefaultParameterValues|PSEmailServer|PSHome|PSScriptRoot|PSSessionApplicationName",
		    "PSSessionConfigurationName|PSSessionOption|PSUICulture|PSVersionTable|Pwd|ShellId|StackTrace|VerbosePreference",
		    "WarningPreference|WhatIfPreference",
		    "Event|EventArgs|EventSubscriber|Sender",
		    "Matches|Ofs|ForEach|LastExitCode|PSCmdlet|PSItem|PSSenderInfo|This",
		    "true|false|null"), "\\$", "");
	
	static final Pattern builtins = buildRegexp(Lists.newArrayList(symbolBuiltins, namedBuiltins, variableBuiltins), null, notCharacterOrDash);
	
	static final Map<String, Pattern> grammar = Maps.newLinkedHashMap();
	static {
		grammar.put("keyword", keywords);
		grammar.put("number", numbers);
		grammar.put("operator", operators);
		grammar.put("builtin", builtins);
		grammar.put("punctuation", punctuation);
		grammar.put("identifier", identifiers);
	}
	
	@FunctionalInterface
	static interface Processor {
	    String process(StringStream stream, State state);
	}
	
	@FunctionalInterface
	static interface Return {
		boolean shouldReturnFrom(State state);
	}
	
	static class Element {
		Processor tokenize;
		Return ret;
		
		public Element(Processor tokenize, Return ret) {
			this.tokenize = tokenize;
			this.ret = ret;
		}
	}
	
	static class State {
		Processor tokenize;
		String startQuote;
		int bracketNesting;
		Stack<Element> returnStack;
		
		public State() {
			this.tokenize = tokenBase;
			this.startQuote = "";
			this.bracketNesting = 0;
			this.returnStack = new Stack<Element>();
		}
	}
	
	static Pattern buildRegexp(List<String> patterns, String prefix, String suffix) {
		if (prefix == null) prefix = "^";
		if (suffix == null) suffix = "\\b";
		return Pattern.compile(prefix + "(" + StringUtils.join(patterns, "|") + ")" + suffix, Pattern.CASE_INSENSITIVE);
	}
	
	static String buildRegexpString(List<String> patterns, String prefix, String suffix) {
		if (prefix == null) prefix = "^";
		if (suffix == null) suffix = "\\b";
		return prefix + "(" + StringUtils.join(patterns, "|") + ")" + suffix;
	}
	
	static final Pattern patternForBase[] = new Pattern[2];
	static {
		patternForBase[0] = Pattern.compile("[\"']");
		patternForBase[1] = Pattern.compile("[({]");
	}
	
	static final Processor tokenBase = (stream, state) -> {
	    Element parent = state.returnStack.isEmpty() ? null : state.returnStack.peek();
	    if (parent != null && parent.ret.shouldReturnFrom(state)) {
	        state.tokenize = parent.tokenize;
	        state.returnStack.pop();
	        return state.tokenize.process(stream, state);
	    }
	    if (stream.eatSpace()) {
	        return "";
	    }
	    if (!stream.eat("(").isEmpty()) {
	        state.bracketNesting += 1;
	        return "punctuation";
	    }
	    if (!stream.eat(")").isEmpty()) {
	        state.bracketNesting -= 1;
	        return "punctuation";
	    }
	    for (String key : grammar.keySet()) {
	        if (!stream.match(grammar.get(key)).isEmpty()) {
	            return key;
	        }
	    }
	    String ch = stream.next();
	    if (ch.equals("'")) {
	        return tokenSingleQuoteString().process(stream, state);
	    }
	    if (ch.equals("$")) {
	        return tokenVariable().process(stream, state);
	    }
	    if (ch.equals("\"")) {
	        return tokenDoubleQuoteString().process(stream, state);
	    }
	    if (ch.equals("<") && !stream.eat("#").isEmpty()) {
	        state.tokenize = tokenComment();
	        return state.tokenize.process(stream, state);
	    }
	    if (ch.equals("#")) {
	        stream.skipToEnd();
	        return "comment";
	    }
	    if (ch.equals("@")) {
	        String quoteMatch = stream.eat(patternForBase[0]);
	        if (!quoteMatch.isEmpty() && stream.eol()) {
	            state.tokenize = tokenMultiString();
	            state.startQuote = quoteMatch.charAt(0) + "";
	            return state.tokenize.process(stream, state);
	        }
	        else if (patternForBase[1].matcher(stream.peek()).matches()) {
	            return "punctuation";
	        }
	        else if (varNames.matcher(stream.peek()).matches()) {
	            return tokenVariable().process(stream, state);
	        }
	    }
	    return "error";
	};
	
	static Processor tokenSingleQuoteString() {
		return (stream, state) -> {
		    String ch;
		    while (!(ch = stream.peek()).isEmpty()) {
		        stream.next();
		        if (ch.equals("'") && !!stream.eat("'").isEmpty()) {
		            state.tokenize = tokenBase;
		            return "string";
		        }
		    }
		    return "error";
		};
	}
	
	static Processor tokenDoubleQuoteString() {
		return (stream, state) -> {
		    String ch;
		    while (!(ch = stream.peek()).isEmpty()) {
		        if (ch.equals("$")) {
		            state.tokenize = tokenStringInterpolation();
		            return "string";
		        }
		        stream.next();
		        if (ch.equals("`")) {
		            stream.next();
		            continue;
		        }
		        if (ch.equals("\"") && !!stream.eat("\"").isEmpty()) {
		            state.tokenize = tokenBase;
		            return "string";
		        }
		    }
		    return "error";
		};
	}
	
	static Processor tokenStringInterpolation() {
		return (stream, state) -> {
			return tokenInterpolation(stream, state, tokenDoubleQuoteString());
		};
	}
	
	static Processor tokenMultiStringReturn() {
		return (stream, state) -> {
			state.tokenize = tokenMultiString();
			state.startQuote = "\"";
			return state.tokenize.process(stream, state);
		};
	}
	
	static Processor tokenHereStringInterpolation() {
		return (stream, state) -> {
			return tokenInterpolation(stream, state, tokenMultiStringReturn());
		};
	}
	
	static String tokenInterpolation(StringStream stream, State state, Processor parentTokenize) {
	    if (stream.match("$(")) {
	        final int savedBracketNesting = state.bracketNesting;
	        state.returnStack.push(new Element(parentTokenize, (_state) -> {
	        	return _state.bracketNesting == savedBracketNesting;
	        }));
	        state.tokenize = tokenBase;
	        state.bracketNesting += 1;
	        return "punctuation";
	    }
	    else {
	        stream.next();
	        state.returnStack.push(new Element(parentTokenize, (_state) -> {
	        	return true;
	        }));
	        state.tokenize = tokenVariable();
	        return state.tokenize.process(stream, state);
	    }
	}
	
	static Processor tokenComment() {
		return (stream, state) -> {
		    boolean maybeEnd = false;
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (maybeEnd && ch.equals(">")) {
		            state.tokenize = tokenBase;
		            break;
		        }
		        maybeEnd = (ch.equals("#"));
		    }
		    return "comment";
		};
	}
	
	static Processor tokenVariable() {
		return (stream, state) -> {
		    String ch = stream.peek();
		    if (!stream.eat("{").isEmpty()) {
		        state.tokenize = tokenVariableWithBraces();
		        return state.tokenize.process(stream, state);
		    }
		    else if (!ch.isEmpty() && varNames.matcher(ch).matches()) {
		        stream.eatWhile(varNames);
		        state.tokenize = tokenBase;
		        return "variable-2";
		    }
		    else {
		        state.tokenize = tokenBase;
		        return "error";
		    }
		};
	}
	
	static Processor tokenVariableWithBraces() {
		return (stream, state) -> {
		    String ch;
		    while (!(ch = stream.next()).isEmpty()) {
		        if (ch.equals("}")) {
		            state.tokenize = tokenBase;
		            break;
		        }
		    }
		    return "variable-2";
		};
	}
	
	static Processor tokenMultiString() {
		return (stream, state) -> {
		    String quote = state.startQuote;
		    if (stream.sol() && !stream.match(Pattern.compile(quote + "@")).isEmpty()) {
		        state.tokenize = tokenBase;
		    }
		    else if (quote.equals("\"")) {
		        while (!stream.eol()) {
		            String ch = stream.peek();
		            if (ch.equals("$")) {
		                state.tokenize = tokenHereStringInterpolation();
		                return "string";
		            }
		            stream.next();
		            if (ch.equals("`")) {
		                stream.next();
		            }
		        }
		    }
		    else {
		        stream.skipToEnd();
		    }
		    return "string";
		};
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "ps1", "psd1", "psm1");
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("application/x-powershell");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("powershell");
	}

	@Override
	public State startState() {
		return new State();
	}

	@Override
	public String token(StringStream stream, State state) {
		return state.tokenize.process(stream, state);
	}
}
