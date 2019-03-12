package io.onedev.commons.jsyntax.python;

import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

public class PythonTokenizer extends AbstractPythonTokenizer {
	
	final Pattern singleOperators;
	final Pattern identifiers;
	final Pattern stringPrefixes;
	final Pattern keywords;
	final Pattern builtins;
	final boolean py3;
	
	/**
	 * Provide a default constructor
	 */
	public PythonTokenizer() {
		this(true);
	}
	
	public PythonTokenizer(boolean py3) {
		this.py3 = py3;
		Set<String> myKeywords = Sets.newHashSet();
		myKeywords.addAll(commonKeywords);
		Set<String> myBuiltins = Sets.newHashSet();
		myBuiltins.addAll(commonBuiltins);
		if (py3) {
			singleOperators = Pattern.compile("^[\\+\\-\\*\\/%&|\\^~<>!@]");
			identifiers = Pattern.compile("^[_A-Za-z\u00A1-\uFFFF][_A-Za-z0-9\u00A1-\uFFFF]*");
			myKeywords.addAll(Sets.newHashSet("nonlocal", "False", "True", "None", "async", "await"));
			myBuiltins.addAll(Sets.newHashSet("ascii", "bytes", "exec", "print"));
			stringPrefixes = Pattern.compile("^(([rbuf]|(br))?('{3}|\"{3}|['\"]))", Pattern.CASE_INSENSITIVE);
		} else {
			singleOperators = Pattern.compile("^[\\+\\-\\*\\/%&|\\^~<>!]");
			identifiers = Pattern.compile("^[_A-Za-z][_A-Za-z0-9]*");
			myKeywords.addAll(Sets.newHashSet("exec", "print"));
			myBuiltins.addAll(Sets.newHashSet("apply", "basestring", "buffer", "cmp", "coerce", "execfile",
                    "file", "intern", "long", "raw_input", "reduce", "reload",
                    "unichr", "unicode", "xrange", "False", "True", "None"));
			stringPrefixes = Pattern.compile("^(([rub]|(ur)|(br))?('{3}|\"{3}|['\"]))", Pattern.CASE_INSENSITIVE);
		}
		keywords = wordRegexp(myKeywords);
		builtins = wordRegexp(myBuiltins);
	}

	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "py", "bzl", "pyw", "BUILD");
	}
	
	@Override
	Pattern singleOperators() {
		return singleOperators;
	}

	@Override
	Pattern identifiers() {
		return identifiers;
	}

	@Override
	Pattern keywords() {
		return keywords;
	}

	@Override
	Pattern builtins() {
		return builtins;
	}

	@Override
	Pattern stringPrefixes() {
		return stringPrefixes;
	}

	@Override
	boolean py3() {
		return py3;
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-python");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("python");
	}
}
