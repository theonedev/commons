package io.onedev.commons.jsyntax.sql;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

public class MssqlTokenizer extends AbstractSqlTokenizer {

	static final Set<String> atoms = wordsOf("false true null unknown");
	static final Set<String> builtin = wordsOf("bigint numeric bit smallint decimal smallmoney int tinyint money float real char varchar text nchar nvarchar ntext binary varbinary image cursor timestamp hierarchyid uniqueidentifier sql_variant xml table ");
	static final Set<String> keywords = wordsOf(sqlKeywords + "begin trigger proc view index for add constraint key primary foreign collate clustered nonclustered declare exec");
	static final Pattern operatorChars = Pattern.compile("^[*+\\-%<>!=]");
	static final Set<String> dateSQL = wordsOf("date datetimeoffset datetime2 smalldatetime datetime time");
	static final Set<String> client = wordsOf("charset clear connect edit ego exit go help nopager notee nowarning pager print prompt quit rehash source status system tee");
	static final Map<String, Processor> hooks = Maps.newHashMap();
	static {
		hooks.put("@", hookVar);
	}
	
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
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-mssql");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("mssql");
	}
}
