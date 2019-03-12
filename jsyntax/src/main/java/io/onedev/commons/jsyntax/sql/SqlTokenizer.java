package io.onedev.commons.jsyntax.sql;

import java.util.Set;
import java.util.regex.Pattern;

public class SqlTokenizer extends AbstractSqlTokenizer {

	static final Set<String> atoms = wordsOf("false true null unknown");
	static final Set<String> builtin = wordsOf("bool boolean bit blob enum long longblob longtext medium mediumblob mediumint mediumtext time timestamp tinyblob tinyint tinytext text bigint int int1 int2 int3 int4 int8 integer float float4 float8 double char varbinary varchar varcharacter precision real date datetime year unsigned signed decimal numeric");
	static final Set<String> keywords = wordsOf(sqlKeywords + "begin");
	static final Pattern operatorChars = Pattern.compile("^[*+\\-%<>!=]");
	static final Set<String> support = wordsOf("ODBCdotTable doubleQuote binaryNumber hexNumber");
	static final Set<String> dateSQL = wordsOf("date time timestamp");
	
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
	public Set<String> support() {
		return support;
	}

	@Override
	public Set<String> dateSQL() {
		return dateSQL;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-sql");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("sql");
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "sql");
	}
	
}
