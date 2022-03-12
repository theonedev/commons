package io.onedev.commons.jsyntax.gas;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class GasTokenizer extends AbstractTokenizer<GasTokenizer.State> {
	
	static final Pattern word = Pattern.compile("\\w");
	
	static final Pattern digit = Pattern.compile("\\d");
	
	static final Pattern alphanumber = Pattern.compile("[0-9a-fA-F]");
	
	static final Set<String> builtinDirectives = Sets.newHashSet(
		    ".abort",
		    ".align",
		    ".altmacro",
		    ".ascii",
		    ".asciz",
		    ".balign",
		    ".balignw",
		    ".balignl",
		    ".bundle_align_mode",
		    ".bundle_lock",
		    ".bundle_unlock",
		    ".byte",
		    ".cfi_startproc",
		    ".comm",
		    ".data",
		    ".def",
		    ".desc",
		    ".dim",
		    ".double",
		    ".eject",
		    ".else",
		    ".elseif",
		    ".end",
		    ".endef",
		    ".endfunc",
		    ".endif",
		    ".equ",
		    ".equiv",
		    ".eqv",
		    ".err",
		    ".error",
		    ".exitm",
		    ".extern",
		    ".fail",
		    ".file",
		    ".fill",
		    ".float",
		    ".func",
		    ".global",
		    ".gnu_attribute",
		    ".hidden",
		    ".hword",
		    ".ident",
		    ".if",
		    ".incbin",
		    ".include",
		    ".int",
		    ".internal",
		    ".irp",
		    ".irpc",
		    ".lcomm",
		    ".lflags",
		    ".line",
		    ".linkonce",
		    ".list",
		    ".ln",
		    ".loc",
		    ".loc_mark_labels",
		    ".local",
		    ".long",
		    ".macro",
		    ".mri",
		    ".noaltmacro",
		    ".nolist",
		    ".octa",
		    ".offset",
		    ".org",
		    ".p2align",
		    ".popsection",
		    ".previous",
		    ".print",
		    ".protected",
		    ".psize",
		    ".purgem",
		    ".pushsection",
		    ".quad",
		    ".reloc",
		    ".rept",
		    ".sbttl",
		    ".scl",
		    ".section",
		    ".set",
		    ".short",
		    ".single",
		    ".size",
		    ".skip",
		    ".sleb128",
		    ".space",
		    ".stab",
		    ".string",
		    ".struct",
		    ".subsection",
		    ".symver",
		    ".tag",
		    ".text",
		    ".title",
		    ".type",
		    ".uleb128",
		    ".val",
		    ".version",
		    ".vtable_entry",
		    ".vtable_inherit",
		    ".warning",
		    ".weak",
		    ".weakref",
		    ".word");
	
	static final Map<String, String> registers = new HashMap<>();
	
	static {
	    registers.put("r0", "variable");
	    registers.put("r1", "variable");
	    registers.put("r2", "variable");
	    registers.put("r3", "variable");
	    registers.put("r4", "variable");
	    registers.put("r5", "variable");
	    registers.put("r6", "variable");
	    registers.put("r7", "variable");
	    registers.put("r8", "variable");
	    registers.put("r9", "variable");
	    registers.put("r10", "variable");
	    registers.put("r11", "variable");
	    registers.put("r12", "variable");

	    registers.put("sp", "variable-2");
	    registers.put("lr", "variable-2");
	    registers.put("pc", "variable-2");
	    registers.put("r13", registers.get("sp"));
	    registers.put("r14", registers.get("lr"));
	    registers.put("r15", registers.get("pc"));
	}

	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class State {
		Processor tokenize;
		
		State(Processor tokenize) {
			this.tokenize = tokenize;
		}
	}
	
	class ClikeComment implements Processor {

		@Override
		public String process(StringStream stream, State state) {
		    boolean maybeEnd = false;
		    String ch;
		    while ((ch = stream.next()).length() != 0) {
		    	if (ch.equals("/") && maybeEnd) {
		    		state.tokenize = null;
		    		break;
		    	}
		    	maybeEnd = (ch.equals("*"));
		    }
		    return "comment";
		}
		
	}
		
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "asm", "ld", "s");
	}

	@Override
	public State startState() {
		return new State(null);
	}

	boolean nextUntilUnescaped(StringStream stream, String end) {
		boolean escaped = false; 
		String next;
		while ((next = stream.next()).length() != 0) {
			if (next.equals(end) && !escaped) {
				return false;
		    }
		    escaped = !escaped && next.equals("\\");
		}
		return escaped;
	}
	
	@Override
	public String token(StringStream stream, State state) {
		if (state.tokenize != null) {
			return state.tokenize.process(stream, state);
        }
		
		if (stream.eatSpace())
			return "";
		
		String cur;
		String ch = stream.next();
		
	    if (ch.equals("/")) {
	    	if (stream.eat("*").length() != 0) {
	            state.tokenize = new ClikeComment();
	            return state.tokenize.process(stream, state);
	    	}
	    }
		
	    if (ch.equals("\"")) {
	    	nextUntilUnescaped(stream, "\"");
	    	return "string";
	    }
	    
	    if (ch.equals(".")) {
	    	stream.eatWhile(word);
	        cur = stream.current().toLowerCase();
	        if (builtinDirectives.contains(cur))
	        	return "builtin";
	        else
	        	return "";
	    }
	    
	    if (ch.equals("=")) {
	    	stream.eatWhile(word);
	    	return "tag";
	    }
	 
	    if (ch.equals("{")) {
	    	return "braket";
	    }

	    if (ch.equals("}")) {
	    	return "braket";
	    }

	    if (digit.matcher(ch).matches()) {
	    	if (ch.equals("0") && stream.eat("x").length() != 0) {
	    		stream.eatWhile(alphanumber);
	    		return "number";
	        }
	        stream.eatWhile(digit);
	        return "number";
	    }

        if (word.matcher(ch).matches()) {
        	stream.eatWhile(word);
        	if (stream.eat(":").length() != 0) {
        		return "tag";
        	}
        	cur = stream.current().toLowerCase();
        	return "";
        }

        return "";
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && (mime.equals("text/x-gas") || mime.equals("text/x-asm"));
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && (mode.equals("asm") || mode.equals("gas"));
	}
}
