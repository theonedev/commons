package io.onedev.commons.jsyntax.sql;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CassandraTokenizer extends AbstractSqlTokenizer {

	static final Set<String> atoms = wordsOf("false true infinity NaN");
	static final Set<String> builtin = wordsOf("ascii bigint blob boolean counter decimal double float frozen inet int list map static text timestamp timeuuid tuple uuid varchar varint");
	static final Set<String> keywords = wordsOf("add all allow alter and any apply as asc authorize batch begin by clustering columnfamily compact consistency count create custom delete desc distinct drop each_quorum exists filtering from grant if in index insert into key keyspace keyspaces level limit local_one local_quorum modify nan norecursive nosuperuser not of on one order password permission permissions primary quorum rename revoke schema select set storage superuser table three to token truncate ttl two type unlogged update use user users using values where with writetime");
	static final Pattern operatorChars = Pattern.compile("^[<>=]");
	static final Set<String> dateSQL = Sets.newHashSet();
	static final Set<String> client = Sets.newHashSet();
	static final Set<String> support = wordsOf("commentSlashSlash decimallessFloat");
	static final Map<String, Processor> hooks = Maps.newHashMap();
	
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
	public Set<String> client() {
		return client;
	}

	@Override
	public Map<String, Processor> hooks() {
		return hooks;
	}

	@Override
	public Pattern operatorChars() {
		return operatorChars;
	}

	@Override
	public Set<String> dateSQL() {
		return dateSQL;
	}

	@Override
	public Set<String> support() {
		return support;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-cassandra");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("cassandra");
	}
}
