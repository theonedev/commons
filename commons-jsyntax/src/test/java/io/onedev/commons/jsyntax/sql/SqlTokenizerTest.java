package io.onedev.commons.jsyntax.sql;

import org.junit.Test;

import io.onedev.commons.jsyntax.AbstractTokenizerTest;

public class SqlTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void testSql() {
		verify(new SqlTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-sql");
		verify(new SqlTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-sql");
	}
	
	@Test
	public void testPlsql() {
		verify(new PlsqlTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-plsql");
		verify(new PlsqlTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-plsql");
	}
	
	@Test
	public void testPgsql() {
		verify(new PgsqlTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-pgsql");
		verify(new PgsqlTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-pgsql");
	}
	
	@Test
	public void testMysql() {
		verify(new MysqlTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-mysql");
		verify(new MysqlTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-mysql");
	}
	
	@Test
	public void testMssql() {
		verify(new MssqlTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-mssql");
		verify(new MssqlTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-mssql");
	}
	
	@Test
	public void testMariadb() {
		verify(new MariadbTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-mariadb");
		verify(new MariadbTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-mariadb");
	}
	
	@Test
	public void testHive() {
		verify(new HiveTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-hive");
		verify(new HiveTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-hive");
	}
	
	@Test
	public void testGql() {
		verify(new GqlTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-gql");
		verify(new GqlTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-gql");
	}
	
	@Test
	public void testCassandra() {
		verify(new CassandraTokenizer(), new String[] {"sql/sql.js"}, "test.sql", "text/x-cassandra");
		verify(new CassandraTokenizer(), new String[] {"sql/sql.js"}, "test2.sql", "text/x-cassandra");
	}
}
