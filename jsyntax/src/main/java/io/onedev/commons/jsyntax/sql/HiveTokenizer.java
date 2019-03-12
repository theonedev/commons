package io.onedev.commons.jsyntax.sql;

import java.util.Set;
import java.util.regex.Pattern;

public class HiveTokenizer extends AbstractSqlTokenizer {

	static final Set<String> atoms = wordsOf("false true null unknown");
	static final Set<String> builtin = wordsOf("bool boolean long timestamp tinyint smallint bigint int float double date datetime unsigned string array struct map uniontype");
	static final Set<String> keywords = wordsOf("select alter $elem$ $key$ $value$ add after all analyze and archive as asc before between binary both bucket buckets by cascade case cast change cluster clustered clusterstatus collection column columns comment compute concatenate continue create cross cursor data database databases dbproperties deferred delete delimited desc describe directory disable distinct distribute drop else enable end escaped exclusive exists explain export extended external false fetch fields fileformat first format formatted from full function functions grant group having hold_ddltime idxproperties if import in index indexes inpath inputdriver inputformat insert intersect into is items join keys lateral left like limit lines load local location lock locks mapjoin materialized minus msck no_drop nocompress not of offline on option or order out outer outputdriver outputformat overwrite partition partitioned partitions percent plus preserve procedure purge range rcfile read readonly reads rebuild recordreader recordwriter recover reduce regexp rename repair replace restrict revoke right rlike row schema schemas semi sequencefile serde serdeproperties set shared show show_database sort sorted ssl statistics stored streamtable table tables tablesample tblproperties temporary terminated textfile then tmp to touch transform trigger true unarchive undo union uniquejoin unlock update use using utc utc_tmestamp view when where while with");
	static final Pattern operatorChars = Pattern.compile("^[*+\\-%<>!=]");
	static final Set<String> dateSQL = wordsOf("date timestamp");
	static final Set<String> support = wordsOf("ODBCdotTable doubleQuote binaryNumber hexNumber");
	
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
	public Set<String> dateSQL() {
		return dateSQL;
	}

	@Override
	public Set<String> support() {
		return support;
	}

	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-hive");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("hive");
	}
}
