package io.onedev.commons.jsyntax.sql;

import java.util.Set;
import java.util.regex.Pattern;

public class GqlTokenizer extends AbstractSqlTokenizer {

	static final Set<String> atoms = wordsOf("false true");
	static final Set<String> builtin = wordsOf("blob datetime first key __key__ string integer double boolean null");
	static final Set<String> keywords = wordsOf("ancestor and asc by contains desc descendant distinct from group has in is limit offset on order select superset where");
	static final Pattern operatorChars = Pattern.compile("^[*+\\-%<>!=]");
	
	@Override
	public Set<String> atoms() {
		return atoms;
	}

	@Override
	public Set<String> builtin() {
		return builtin;
	}

	@Override
	public Set<String> keywords() {
		return keywords;
	}

	@Override
	public Pattern operatorChars() {
		return operatorChars;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-gql");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("gql");
	}
}
