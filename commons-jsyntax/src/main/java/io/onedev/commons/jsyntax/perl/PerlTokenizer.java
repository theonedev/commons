package io.onedev.commons.jsyntax.perl;

import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import io.onedev.commons.jsyntax.AbstractTokenizer;
import io.onedev.commons.jsyntax.StringStream;

public class PerlTokenizer extends AbstractTokenizer<PerlTokenizer.State> {

	static Map<String, int[]> PERL = Maps.newHashMap();
	
	static {
	    PERL.put("->", new int[] {4,0});
	    PERL.put("++", new int[] {4,0});
	    PERL.put("--", new int[] {4,0});
	    PERL.put("**", new int[] {4,0});
	    PERL.put("=~", new int[] {4,0});
	    PERL.put("!~", new int[] {4,0});
	    PERL.put("*", new int[] {4,0});
	    PERL.put("/", new int[] {4,0});
	    PERL.put("%", new int[] {4,0});
	    PERL.put("x", new int[] {4,0});
	    PERL.put("+", new int[] {4,0});
	    PERL.put("-", new int[] {4,0});
	    PERL.put(".", new int[] {4,0});
	    PERL.put("<<", new int[] {4,0});
	    PERL.put(">>", new int[] {4,0});
	    PERL.put("<", new int[] {4,0});
	    PERL.put(">", new int[] {4,0});
	    PERL.put("<=", new int[] {4,0});
	    PERL.put(">=", new int[] {4,0});
	    PERL.put("lt", new int[] {4,0});
	    PERL.put("gt", new int[] {4,0});
	    PERL.put("le", new int[] {4,0});
	    PERL.put("ge", new int[] {4,0});
	    PERL.put("==", new int[] {4,0});
	    PERL.put("!=", new int[] {4,0});
	    PERL.put("<=>", new int[] {4,0});
	    PERL.put("eq", new int[] {4,0});
	    PERL.put("ne", new int[] {4,0});
	    PERL.put("cmp", new int[] {4,0});
	    PERL.put("~~", new int[] {4,0});
	    PERL.put("&", new int[] {4,0});
	    PERL.put("|", new int[] {4,0});
	    PERL.put("^", new int[] {4,0});
	    PERL.put("&&", new int[] {4,0});
	    PERL.put("||", new int[] {4,0});
	    PERL.put("//", new int[] {4,0});
	    PERL.put("..", new int[] {4,0});
	    PERL.put("...", new int[] {4,0});
	    PERL.put("?", new int[] {4,0});
	    PERL.put(":", new int[] {4,0});
	    PERL.put("=", new int[] {4,0});
	    PERL.put("+=", new int[] {4,0});
	    PERL.put("-=", new int[] {4,0});
	    PERL.put("*=", new int[] {4,0});
	    PERL.put(",", new int[] {4,0});
	    PERL.put("=>", new int[] {4,0});
	    PERL.put("::", new int[] {4,0});
	    PERL.put("not", new int[] {4,0});
	    PERL.put("and", new int[] {4,0});
	    PERL.put("or", new int[] {4,0});
	    PERL.put("xor", new int[] {4,0});
	    PERL.put("BEGIN", new int[] {5,1});
	    PERL.put("END", new int[] {5,1});
	    PERL.put("PRINT", new int[] {5,1});
	    PERL.put("PRINTF", new int[] {5,1});
	    PERL.put("GETC", new int[] {5,1});
	    PERL.put("READ", new int[] {5,1});
	    PERL.put("READLINE", new int[] {5,1});
	    PERL.put("DESTROY", new int[] {5,1});
	    PERL.put("TIE", new int[] {5,1});
	    PERL.put("TIEHANDLE", new int[] {5,1});
	    PERL.put("UNTIE", new int[] {5,1});
	    PERL.put("STDIN", new int[] {5,0});
	    PERL.put("STDIN_TOP", new int[] {5,0});
	    PERL.put("STDOUT", new int[] {5,0});
	    PERL.put("STDOUT_TOP", new int[] {5,0});
	    PERL.put("STDERR", new int[] {5,0});
	    PERL.put("STDERR_TOP", new int[] {5,0});
	    PERL.put("$ARG", new int[] {5,0});
	    PERL.put("$_", new int[] {5,0});
	    PERL.put("@ARG", new int[] {5,0});
	    PERL.put("@_", new int[] {5,0});
	    PERL.put("$LIST_SEPARATOR", new int[] {5,0});
	    PERL.put("$\"", new int[] {5,0});
	    PERL.put("$PROCESS_ID", new int[] {5,0});
	    PERL.put("$PID", new int[] {5,0});
	    PERL.put("$$", new int[] {5,0});
	    PERL.put("$REAL_GROUP_ID", new int[] {5,0});
	    PERL.put("$GID", new int[] {5,0});
	    PERL.put("$(", new int[] {5,0});
	    PERL.put("$EFFECTIVE_GROUP_ID", new int[] {5,0});
	    PERL.put("$EGID", new int[] {5,0});
	    PERL.put("$)", new int[] {5,0});
	    PERL.put("$PROGRAM_NAME", new int[] {5,0});
	    PERL.put("$0", new int[] {5,0});
	    PERL.put("$SUBSCRIPT_SEPARATOR", new int[] {5,0});
	    PERL.put("$SUBSEP", new int[] {5,0});
	    PERL.put("$;", new int[] {5,0});
	    PERL.put("$REAL_USER_ID", new int[] {5,0});
	    PERL.put("$UID", new int[] {5,0});
	    PERL.put("$<", new int[] {5,0});
	    PERL.put("$EFFECTIVE_USER_ID", new int[] {5,0});
	    PERL.put("$EUID", new int[] {5,0});
	    PERL.put("$>", new int[] {5,0});
	    PERL.put("$a", new int[] {5,0});
	    PERL.put("$b", new int[] {5,0});
	    PERL.put("$COMPILING", new int[] {5,0});
	    PERL.put("$^C", new int[] {5,0});
	    PERL.put("$DEBUGGING", new int[] {5,0});
	    PERL.put("$^D", new int[] {5,0});
	    PERL.put("${^ENCODING}", new int[] {5,0});
	    PERL.put("$ENV", new int[] {5,0});
	    PERL.put("%ENV", new int[] {5,0});
	    PERL.put("$SYSTEM_FD_MAX", new int[] {5,0});
	    PERL.put("$^F", new int[] {5,0});
	    PERL.put("@F", new int[] {5,0});
	    PERL.put("${^GLOBAL_PHASE}", new int[] {5,0});
	    PERL.put("$^H", new int[] {5,0});
	    PERL.put("%^H", new int[] {5,0});
	    PERL.put("@INC", new int[] {5,0});
	    PERL.put("%INC", new int[] {5,0});
	    PERL.put("$INPLACE_EDIT", new int[] {5,0});
	    PERL.put("$^I", new int[] {5,0});
	    PERL.put("$^M", new int[] {5,0});
	    PERL.put("$OSNAME", new int[] {5,0});
	    PERL.put("$^O", new int[] {5,0});
	    PERL.put("${^OPEN}", new int[] {5,0});
	    PERL.put("$PERLDB", new int[] {5,0});
	    PERL.put("$^P", new int[] {5,0});
	    PERL.put("$SIG", new int[] {5,0});
	    PERL.put("%SIG", new int[] {5,0});
	    PERL.put("$BASETIME", new int[] {5,0});
	    PERL.put("$^T", new int[] {5,0});
	    PERL.put("${^TAINT}", new int[] {5,0});
	    PERL.put("${^UNICODE}", new int[] {5,0});
	    PERL.put("${^UTF8CACHE}", new int[] {5,0});
	    PERL.put("${^UTF8LOCALE}", new int[] {5,0});
	    PERL.put("$PERL_VERSION", new int[] {5,0});
	    PERL.put("$^V", new int[] {5,0});
	    PERL.put("${^WIN32_SLOPPY_STAT}", new int[] {5,0});
	    PERL.put("$EXECUTABLE_NAME", new int[] {5,0});
	    PERL.put("$^X", new int[] {5,0});
	    PERL.put("$1", new int[] {5,0});
	    PERL.put("$MATCH", new int[] {5,0});
	    PERL.put("$&", new int[] {5,0});
	    PERL.put("${^MATCH}", new int[] {5,0});
	    PERL.put("$PREMATCH", new int[] {5,0});
	    PERL.put("$`", new int[] {5,0});
	    PERL.put("${^PREMATCH}", new int[] {5,0});
	    PERL.put("$POSTMATCH", new int[] {5,0});
	    PERL.put("$'", new int[] {5,0});
	    PERL.put("${^POSTMATCH}", new int[] {5,0});
	    PERL.put("$LAST_PAREN_MATCH", new int[] {5,0});
	    PERL.put("$+", new int[] {5,0});
	    PERL.put("$LAST_SUBMATCH_RESULT", new int[] {5,0});
	    PERL.put("$^N", new int[] {5,0});
	    PERL.put("@LAST_MATCH_END", new int[] {5,0});
	    PERL.put("@+", new int[] {5,0});
	    PERL.put("%LAST_PAREN_MATCH", new int[] {5,0});
	    PERL.put("%+", new int[] {5,0});
	    PERL.put("@LAST_MATCH_START", new int[] {5,0});
	    PERL.put("@-", new int[] {5,0});
	    PERL.put("%LAST_MATCH_START", new int[] {5,0});
	    PERL.put("%-", new int[] {5,0});
	    PERL.put("$LAST_REGEXP_CODE_RESULT", new int[] {5,0});
	    PERL.put("$^R", new int[] {5,0});
	    PERL.put("${^RE_DEBUG_FLAGS}", new int[] {5,0});
	    PERL.put("${^RE_TRIE_MAXBUF}", new int[] {5,0});
	    PERL.put("$ARGV", new int[] {5,0});
	    PERL.put("@ARGV", new int[] {5,0});
	    PERL.put("ARGV", new int[] {5,0});
	    PERL.put("ARGVOUT", new int[] {5,0});
	    PERL.put("$OUTPUT_FIELD_SEPARATOR", new int[] {5,0});
	    PERL.put("$OFS", new int[] {5,0});
	    PERL.put("$,", new int[] {5,0});
	    PERL.put("$INPUT_LINE_NUMBER", new int[] {5,0});
	    PERL.put("$NR", new int[] {5,0});
	    PERL.put("$.", new int[] {5,0});
	    PERL.put("$INPUT_RECORD_SEPARATOR", new int[] {5,0});
	    PERL.put("$RS", new int[] {5,0});
	    PERL.put("$/", new int[] {5,0});
	    PERL.put("$OUTPUT_RECORD_SEPARATOR", new int[] {5,0});
	    PERL.put("$ORS", new int[] {5,0});
	    PERL.put("$\\", new int[] {5,0});
	    PERL.put("$OUTPUT_AUTOFLUSH", new int[] {5,0});
	    PERL.put("$|", new int[] {5,0});
	    PERL.put("$ACCUMULATOR", new int[] {5,0});
	    PERL.put("$^A", new int[] {5,0});
	    PERL.put("$FORMAT_FORMFEED", new int[] {5,0});
	    PERL.put("$^L", new int[] {5,0});
	    PERL.put("$FORMAT_PAGE_NUMBER", new int[] {5,0});
	    PERL.put("$%", new int[] {5,0});
	    PERL.put("$FORMAT_LINES_LEFT", new int[] {5,0});
	    PERL.put("$-", new int[] {5,0});
	    PERL.put("$FORMAT_LINE_BREAK_CHARACTERS", new int[] {5,0});
	    PERL.put("$:", new int[] {5,0});
	    PERL.put("$FORMAT_LINES_PER_PAGE", new int[] {5,0});
	    PERL.put("$=", new int[] {5,0});
	    PERL.put("$FORMAT_TOP_NAME", new int[] {5,0});
	    PERL.put("$^", new int[] {5,0});
	    PERL.put("$FORMAT_NAME", new int[] {5,0});
	    PERL.put("$~", new int[] {5,0});
	    PERL.put("${^CHILD_ERROR_NATIVE}", new int[] {5,0});
	    PERL.put("$EXTENDED_OS_ERROR", new int[] {5,0});
	    PERL.put("$^E", new int[] {5,0});
	    PERL.put("$EXCEPTIONS_BEING_CAUGHT", new int[] {5,0});
	    PERL.put("$^S", new int[] {5,0});
	    PERL.put("$WARNING", new int[] {5,0});
	    PERL.put("$^W", new int[] {5,0});
	    PERL.put("${^WARNING_BITS}", new int[] {5,0});
	    PERL.put("$OS_ERROR", new int[] {5,0});
	    PERL.put("$ERRNO", new int[] {5,0});
	    PERL.put("$!", new int[] {5,0});
	    PERL.put("%OS_ERROR", new int[] {5,0});
	    PERL.put("%ERRNO", new int[] {5,0});
	    PERL.put("%!", new int[] {5,0});
	    PERL.put("$CHILD_ERROR", new int[] {5,0});
	    PERL.put("$?", new int[] {5,0});
	    PERL.put("$EVAL_ERROR", new int[] {5,0});
	    PERL.put("$@", new int[] {5,0});
	    PERL.put("$OFMT", new int[] {5,0});
	    PERL.put("$#", new int[] {5,0});
	    PERL.put("$*", new int[] {5,0});
	    PERL.put("$ARRAY_BASE", new int[] {5,0});
	    PERL.put("$[", new int[] {5,0});
	    PERL.put("$OLD_PERL_VERSION", new int[] {5,0});
	    PERL.put("$]", new int[] {5,0});
	    PERL.put("if", new int[] {1,1});
	    PERL.put("elsif", new int[] {1,1});
	    PERL.put("else", new int[] {1,1});
	    PERL.put("while", new int[] {1,1});
	    PERL.put("unless", new int[] {1,1});
	    PERL.put("for", new int[] {1,1});
	    PERL.put("foreach", new int[] {1,1});
	    PERL.put("abs", new int[] {1,0});
	    PERL.put("accept", new int[] {1,0});
	    PERL.put("alarm", new int[] {1,0});
	    PERL.put("atan2", new int[] {1,0});
	    PERL.put("bind", new int[] {1,0});
	    PERL.put("binmode", new int[] {1,0});
	    PERL.put("bless", new int[] {1,0});
	    PERL.put("bootstrap", new int[] {1,0});
	    PERL.put("break", new int[] {1,0});
	    PERL.put("caller", new int[] {1,0});
	    PERL.put("chdir", new int[] {1,0});
	    PERL.put("chmod", new int[] {1,0});
	    PERL.put("chomp", new int[] {1,0});
	    PERL.put("chop", new int[] {1,0});
	    PERL.put("chown", new int[] {1,0});
	    PERL.put("chr", new int[] {1,0});
	    PERL.put("chroot", new int[] {1,0});
	    PERL.put("close", new int[] {1,0});
	    PERL.put("closedir", new int[] {1,0});
	    PERL.put("connect", new int[] {1,0});
	    PERL.put("continue", new int[] {1,1});
	    PERL.put("cos", new int[] {1,0});
	    PERL.put("crypt", new int[] {1,0});
	    PERL.put("dbmclose", new int[] {1,0});
	    PERL.put("dbmopen", new int[] {1,0});
	    PERL.put("default", new int[] {1,0});
	    PERL.put("defined", new int[] {1,0});
	    PERL.put("delete", new int[] {1,0});
	    PERL.put("die", new int[] {1,0});
	    PERL.put("do", new int[] {1,0});
	    PERL.put("dump", new int[] {1,0});
	    PERL.put("each", new int[] {1,0});
	    PERL.put("endgrent", new int[] {1,0});
	    PERL.put("endhostent", new int[] {1,0});
	    PERL.put("endnetent", new int[] {1,0});
	    PERL.put("endprotoent", new int[] {1,0});
	    PERL.put("endpwent", new int[] {1,0});
	    PERL.put("endservent", new int[] {1,0});
	    PERL.put("eof", new int[] {1,0});
	    PERL.put("eval", new int[] {1,0});
	    PERL.put("exec", new int[] {1,0});
	    PERL.put("exists", new int[] {1,0});
	    PERL.put("exit", new int[] {1,0});
	    PERL.put("exp", new int[] {1,0});
	    PERL.put("fcntl", new int[] {1,0});
	    PERL.put("fileno", new int[] {1,0});
	    PERL.put("flock", new int[] {1,0});
	    PERL.put("fork", new int[] {1,0});
	    PERL.put("format", new int[] {1,0});
	    PERL.put("formline", new int[] {1,0});
	    PERL.put("getc", new int[] {1,0});
	    PERL.put("getgrent", new int[] {1,0});
	    PERL.put("getgrgid", new int[] {1,0});
	    PERL.put("getgrnam", new int[] {1,0});
	    PERL.put("gethostbyaddr", new int[] {1,0});
	    PERL.put("gethostbyname", new int[] {1,0});
	    PERL.put("gethostent", new int[] {1,0});
	    PERL.put("getlogin", new int[] {1,0});
	    PERL.put("getnetbyaddr", new int[] {1,0});
	    PERL.put("getnetbyname", new int[] {1,0});
	    PERL.put("getnetent", new int[] {1,0});
	    PERL.put("getpeername", new int[] {1,0});
	    PERL.put("getpgrp", new int[] {1,0});
	    PERL.put("getppid", new int[] {1,0});
	    PERL.put("getpriority", new int[] {1,0});
	    PERL.put("getprotobyname", new int[] {1,0});
	    PERL.put("getprotobynumber", new int[] {1,0});
	    PERL.put("getprotoent", new int[] {1,0});
	    PERL.put("getpwent", new int[] {1,0});
	    PERL.put("getpwnam", new int[] {1,0});
	    PERL.put("getpwuid", new int[] {1,0});
	    PERL.put("getservbyname", new int[] {1,0});
	    PERL.put("getservbyport", new int[] {1,0});
	    PERL.put("getservent", new int[] {1,0});
	    PERL.put("getsockname", new int[] {1,0});
	    PERL.put("getsockopt", new int[] {1,0});
	    PERL.put("given", new int[] {1,0});
	    PERL.put("glob", new int[] {1,0});
	    PERL.put("gmtime", new int[] {1,0});
	    PERL.put("goto", new int[] {1,0});
	    PERL.put("grep", new int[] {1,0});
	    PERL.put("hex", new int[] {1,0});
	    PERL.put("import", new int[] {1,0});
	    PERL.put("index", new int[] {1,0});
	    PERL.put("int", new int[] {1,0});
	    PERL.put("ioctl", new int[] {1,0});
	    PERL.put("join", new int[] {1,0});
	    PERL.put("keys", new int[] {1,0});
	    PERL.put("kill", new int[] {1,0});
	    PERL.put("last", new int[] {1,0});
	    PERL.put("lc", new int[] {1,0});
	    PERL.put("lcfirst", new int[] {1,0});
	    PERL.put("length", new int[] {1,0});
	    PERL.put("link", new int[] {1,0});
	    PERL.put("listen", new int[] {1,0});
	    PERL.put("local", new int[] {2,0});
	    PERL.put("localtime", new int[] {1,0});
	    PERL.put("lock", new int[] {1,0});
	    PERL.put("log", new int[] {1,0});
	    PERL.put("lstat", new int[] {1,0});
	    PERL.put("m", null);
	    PERL.put("map", new int[] {1,0});
	    PERL.put("mkdir", new int[] {1,0});
	    PERL.put("msgctl", new int[] {1,0});
	    PERL.put("msgget", new int[] {1,0});
	    PERL.put("msgrcv", new int[] {1,0});
	    PERL.put("msgsnd", new int[] {1,0});
	    PERL.put("my", new int[] {2,0});
	    PERL.put("new", new int[] {1,0});
	    PERL.put("next", new int[] {1,0});
	    PERL.put("no", new int[] {1,0});
	    PERL.put("oct", new int[] {1,0});
	    PERL.put("open", new int[] {1,0});
	    PERL.put("opendir", new int[] {1,0});
	    PERL.put("ord", new int[] {1,0});
	    PERL.put("our", new int[] {2,0});
	    PERL.put("pack", new int[] {1,0});
	    PERL.put("package", new int[] {1,0});
	    PERL.put("pipe", new int[] {1,0});
	    PERL.put("pop", new int[] {1,0});
	    PERL.put("pos", new int[] {1,0});
	    PERL.put("print", new int[] {1,0});
	    PERL.put("printf", new int[] {1,0});
	    PERL.put("prototype", new int[] {1,0});
	    PERL.put("push", new int[] {1,0});
	    PERL.put("q", null);
	    PERL.put("qq", null);
	    PERL.put("qr", null);
	    PERL.put("quotemeta", null);
	    PERL.put("qw", null);
	    PERL.put("qx", null);
	    PERL.put("rand", new int[] {1,0});
	    PERL.put("read", new int[] {1,0});
	    PERL.put("readdir", new int[] {1,0});
	    PERL.put("readline", new int[] {1,0});
	    PERL.put("readlink", new int[] {1,0});
	    PERL.put("readpipe", new int[] {1,0});
	    PERL.put("recv", new int[] {1,0});
	    PERL.put("redo", new int[] {1,0});
	    PERL.put("ref", new int[] {1,0});
	    PERL.put("rename", new int[] {1,0});
	    PERL.put("require", new int[] {1,0});
	    PERL.put("reset", new int[] {1,0});
	    PERL.put("return", new int[] {1,0});
	    PERL.put("reverse", new int[] {1,0});
	    PERL.put("rewinddir", new int[] {1,0});
	    PERL.put("rindex", new int[] {1,0});
	    PERL.put("rmdir", new int[] {1,0});
	    PERL.put("s", null);
	    PERL.put("say", new int[] {1,0});
	    PERL.put("scalar", new int[] {1,0});
	    PERL.put("seek", new int[] {1,0});
	    PERL.put("seekdir", new int[] {1,0});
	    PERL.put("select", new int[] {1,0});
	    PERL.put("semctl", new int[] {1,0});
	    PERL.put("semget", new int[] {1,0});
	    PERL.put("semop", new int[] {1,0});
	    PERL.put("send", new int[] {1,0});
	    PERL.put("setgrent", new int[] {1,0});
	    PERL.put("sethostent", new int[] {1,0});
	    PERL.put("setnetent", new int[] {1,0});
	    PERL.put("setpgrp", new int[] {1,0});
	    PERL.put("setpriority", new int[] {1,0});
	    PERL.put("setprotoent", new int[] {1,0});
	    PERL.put("setpwent", new int[] {1,0});
	    PERL.put("setservent", new int[] {1,0});
	    PERL.put("setsockopt", new int[] {1,0});
	    PERL.put("shift", new int[] {1,0});
	    PERL.put("shmctl", new int[] {1,0});
	    PERL.put("shmget", new int[] {1,0});
	    PERL.put("shmread", new int[] {1,0});
	    PERL.put("shmwrite", new int[] {1,0});
	    PERL.put("shutdown", new int[] {1,0});
	    PERL.put("sin", new int[] {1,0});
	    PERL.put("sleep", new int[] {1,0});
	    PERL.put("socket", new int[] {1,0});
	    PERL.put("socketpair", new int[] {1,0});
	    PERL.put("sort", new int[] {1,0});
	    PERL.put("splice", new int[] {1,0});
	    PERL.put("split", new int[] {1,0});
	    PERL.put("sprintf", new int[] {1,0});
	    PERL.put("sqrt", new int[] {1,0});
	    PERL.put("srand", new int[] {1,0});
	    PERL.put("stat", new int[] {1,0});
	    PERL.put("state", new int[] {1,0});
	    PERL.put("study", new int[] {1,0});
	    PERL.put("sub", new int[] {1,0});
	    PERL.put("substr", new int[] {1,0});
	    PERL.put("symlink", new int[] {1,0});
	    PERL.put("syscall", new int[] {1,0});
	    PERL.put("sysopen", new int[] {1,0});
	    PERL.put("sysread", new int[] {1,0});
	    PERL.put("sysseek", new int[] {1,0});
	    PERL.put("system", new int[] {1,0});
	    PERL.put("syswrite", new int[] {1,0});
	    PERL.put("tell", new int[] {1,0});
	    PERL.put("telldir", new int[] {1,0});
	    PERL.put("tie", new int[] {1,0});
	    PERL.put("tied", new int[] {1,0});
	    PERL.put("time", new int[] {1,0});
	    PERL.put("times", new int[] {1,0});
	    PERL.put("tr", null);
	    PERL.put("truncate", new int[] {1,0});
	    PERL.put("uc", new int[] {1,0});
	    PERL.put("ucfirst", new int[] {1,0});
	    PERL.put("umask", new int[] {1,0});
	    PERL.put("undef", new int[] {1,0});
	    PERL.put("unlink", new int[] {1,0});
	    PERL.put("unpack", new int[] {1,0});
	    PERL.put("unshift", new int[] {1,0});
	    PERL.put("untie", new int[] {1,0});
	    PERL.put("use", new int[] {1,0});
	    PERL.put("utime", new int[] {1,0});
	    PERL.put("values", new int[] {1,0});
	    PERL.put("vec", new int[] {1,0});
	    PERL.put("wait", new int[] {1,0});
	    PERL.put("waitpid", new int[] {1,0});
	    PERL.put("wantarray", new int[] {1,0});
	    PERL.put("warn", new int[] {1,0});
	    PERL.put("when", new int[] {1,0});
	    PERL.put("write", new int[] {1,0});
	    PERL.put("y", null);
	}
	
	static String RXstyle = "string-2";
	static Pattern RXmodifiers = Pattern.compile("[goseximacplud]");
	
	static interface Processor {
		String process(StringStream stream, State state);
	}
	
	static class State {
		Processor tokenize;
		String chain;
		String style;
		Pattern tail;
		
		State(Processor tokenize, String chain, String style, Pattern tail) {
			this.tokenize = tokenize;
			this.chain = chain;
			this.style = style;
			this.tail = tail;
		}
	}
	
	static String look(StringStream stream, int c) {
		if (stream.pos() + c < 0 || stream.pos() + c >= stream.string().length())
			return "";
		return String.valueOf(stream.string().charAt(stream.pos() + c));
	}
	
	static String prefix(StringStream stream, int c) {
		if (c != 0) {
			int x = stream.pos() - c;
			x = x >= 0 ? x : 0;
			return stream.string().substring(x, x + c);
		} else {
			return stream.string().substring(0, stream.pos() - 1);
		}
	}
	
	static String suffix(StringStream stream, int c) {
		int y = stream.string().length();
		int x = y - stream.pos() + 1;
		return stream.string().substring(stream.pos(), stream.pos() + (c < y ? c : x));
	}
	
	static void eatSuffix(StringStream stream, int c) {
		int x = stream.pos() + c;
		int y;
		if (x <= 0)
			stream.pos(0);
		else if (x >= (y = stream.string().length() - 1))
			stream.pos(y);
		else
			stream.pos(x);
	}
	
	String tokenChain(StringStream stream, State state, String[] chain, String style, Pattern tail) {
		state.chain = "";
		state.style = "";
		state.tail = null;
		state.tokenize = new TokenChain(chain, style, tail);
		return state.tokenize.process(stream, state);
	}
	
	class TokenChain implements Processor {
		String[] chain;
		String style;
		Pattern tail;
		
		TokenChain(String[] chain, String style, Pattern tail) {
			this.chain = chain;
			this.style = style;
			this.tail = tail;
		}
		
		@Override
		public String process(StringStream stream, State state) {
			boolean e = false;
			String c;
			int i = 0;
			while (!(c = stream.next()).isEmpty()) {
				if (c.equals(chain[i]) && !e) {
					if (++i < chain.length) {
						state.chain = chain[i];
						state.style = style;
						state.tail = tail;
					} else if (tail != null) {
						stream.eatWhile(tail);
					}
					state.tokenize = new TokenPerl();
					return style;
				}
				e = !e && c.equals("\\");
			}
			return style;
		}
	}
	
	String tokenSomething(StringStream stream, State state, String string) {
		state.tokenize = new TokenSomething(string);
		return state.tokenize.process(stream, state);
	}
	
	class TokenSomething implements Processor {
		String string;
		
		TokenSomething(String string) {
			this.string = string;
		}

		@Override
		public String process(StringStream stream, State state) {
			if (stream.string().equals(string))
				state.tokenize = new TokenPerl();
			stream.skipToEnd();
			return "string";
		}
	}
	
	static Pattern[] pattern = new Pattern[41];
	
	static {
		pattern[0] = Pattern.compile("^\\-?[\\d\\.]");
		pattern[1] = Pattern.compile("^(\\-?(\\d*\\.\\d+(e[+-]?\\d+)?|\\d+\\.\\d*)|0x[\\da-fA-F]+|0b[01]+|\\d+(e[+-]?\\d+)?)");
		pattern[2] = Pattern.compile("^<<(?=\\w)");
		pattern[3] = Pattern.compile("\\w");
		pattern[4] = Pattern.compile("^\\=item(?!\\w)");
		pattern[5] = Pattern.compile("\\w");
		pattern[6] = Pattern.compile("\\w");
		pattern[7] = Pattern.compile("[\\^'\"!~\\/]");
		pattern[8] = Pattern.compile("[\\^'\"!~\\/]");
		pattern[9] = Pattern.compile("[\\^'\"!~\\/]");
		pattern[10] = Pattern.compile("[\\^'\"!~\\/]");
		pattern[11] = Pattern.compile("[\\^'\"!~\\/(\\[{<]");
		pattern[12] = Pattern.compile("[\\^'\"!~\\/]");
		pattern[13] = Pattern.compile("\\w");
		pattern[14] = Pattern.compile("[(\\[{<\\^'\"!~\\/]");
		pattern[15] = Pattern.compile("[\\^'\"!~\\/]");
		pattern[16] = Pattern.compile("[\\/>\\]})\\w]");
		pattern[17] = Pattern.compile("[(\\[{<\\^'\"!~\\/]");
		pattern[18] = Pattern.compile("[\\/>\\]})\\w]");
		pattern[19] = Pattern.compile("[(\\[{<\\^'\"!~\\/]");
		pattern[20] = Pattern.compile("[\\/>\\]})\\w]");
		pattern[21] = Pattern.compile("[(\\[{<\\^'\"!~\\/]");
		pattern[22] = Pattern.compile("~\\s*$");
		pattern[23] = Pattern.compile("\\d");
		pattern[24] = Pattern.compile("\\d");
		pattern[25] = Pattern.compile("[$@%]");
		pattern[26] = Pattern.compile("[A-Z]");
		pattern[27] = Pattern.compile("[@$%&]");
		pattern[28] = Pattern.compile("[=|\\\\\\-#?@;:&`~\\^!\\[\\]*'\"$+.,\\/<>()]");
		pattern[29] = Pattern.compile("[$@%&]");
		pattern[30] = Pattern.compile("[\\w$\\[\\]]");
		pattern[31] = Pattern.compile("[\\w$\\[\\]]");
		pattern[32] = Pattern.compile("[:+\\-\\^*$&%@=<>!?|\\/~\\.]");
		pattern[33] = Pattern.compile("[:+\\-\\^*$&%@=<>!?|\\/~\\.]");
		pattern[34] = Pattern.compile("\\w");
		pattern[35] = Pattern.compile("\\w");
		pattern[36] = Pattern.compile("[A-Z]");
		pattern[37] = Pattern.compile("[A-Z_]");
		pattern[38] = Pattern.compile("[\\da-z]");
		pattern[39] = Pattern.compile("[a-zA-Z_]");
		pattern[40] = Pattern.compile("\\w");
	}
	
	class TokenPerl implements Processor {

		@Override
		public String process(StringStream stream, State state) {
			if (stream.eatSpace())
				return "";
			if (!state.chain.equals(""))
				return tokenChain(stream, state, new String[] {state.chain}, state.style, state.tail);
			if (!stream.match(pattern[0], false).isEmpty())
				if (!stream.match(pattern[1]).isEmpty())
					return "number";
			if (!stream.match(pattern[2]).isEmpty()) {
				stream.eatWhile(pattern[3]);
				return tokenSomething(stream, state, stream.current().substring(2));
			}
			if (stream.sol() && !stream.match(pattern[4]).isEmpty())
				return tokenSomething(stream, state, "=cut");
			
			String ch = stream.next();
			if (ch.equals("\"") || ch.equals("'")) {
				if (prefix(stream, 3).equals("<<" + ch)) {
					int p = stream.pos();
					stream.eatWhile(pattern[5]);
					String n = stream.current().substring(1);
					if (!n.equals("") && !stream.eat(ch).isEmpty())
						return tokenSomething(stream, state, n);
					stream.pos(p);
				}
				return tokenChain(stream, state, new String[] {ch}, "string", null);
			}
			if (ch.equals("q")) {
                String c = look(stream, -2);
                if (!(!c.equals("") && pattern[6].matcher(c).matches())) {
                	c = look(stream, 0);
                	if (c.equals("x")) {
                		c = look(stream, 1);
                		if (c.equals("(")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {")"}, RXstyle, RXmodifiers);
                		}
                		if (c.equals("[")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"]"}, RXstyle, RXmodifiers);
                		}
                		if (c.equals("{")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"}"}, RXstyle, RXmodifiers);
                		}
                		if (c.equals("<")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {">"}, RXstyle, RXmodifiers);
                		}
                		if (pattern[7].matcher(c).matches()) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {stream.eat(c)}, RXstyle, RXmodifiers);
                		}
                	}
                	else if (c.equals("q")) {
                		c = look(stream, 1);
                		if (c.equals("(")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {")"}, "string", null);
                		}
                		if (c.equals("[")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"]"}, "string", null);
                		}
                		if (c.equals("{")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"}"}, "string", null);
                		}
                		if (c.equals("<")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {">"}, "string", null);
                		}
                		if (pattern[8].matcher(c).matches()) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {stream.eat(c)}, "string", null);
                		}
                	}
                	else if (c.equals("w")) {
                		c = look(stream, 1);
                		if (c.equals("(")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {")"}, "bracket", null);
                		}
                		if (c.equals("[")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"]"}, "bracket", null);
                		}
                		if (c.equals("{")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"}"}, "bracket", null);
                		}
                		if (c.equals("<")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {">"}, "bracket", null);
                		}
                		if (pattern[9].matcher(c).matches()) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {stream.eat(c)}, "bracket", null);
                		}
                	}
                	else if (c.equals("r")) {
                		c = look(stream, 1);
                		if (c.equals("(")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {")"}, RXstyle, RXmodifiers);
                		}
                		if (c.equals("[")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"]"}, RXstyle, RXmodifiers);
                		}
                		if (c.equals("{")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {"}"}, RXstyle, RXmodifiers);
                		}
                		if (c.equals("<")) {
                			eatSuffix(stream, 2);
                			return tokenChain(stream, state, new String[] {">"}, RXstyle, RXmodifiers);
                		}
                		if (pattern[10].matcher(c).matches()) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {stream.eat(c)}, RXstyle, RXmodifiers);
                		}
                	}
                	else if (pattern[11].matcher(c).matches()) {
                		if (c.equals("(")) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {")"}, "string", null);
                		}
                		if (c.equals("[")) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {"]"}, "string", null);
                		}
                		if (c.equals("{")) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {"}"}, "string", null);
                		}
                		if (c.equals("<")) {
                			eatSuffix(stream, 1);
                			return tokenChain(stream, state, new String[] {">"}, "string", null);
                		}
                		if (pattern[12].matcher(c).matches()) {
                			return tokenChain(stream, state, new String[] {stream.eat(c)}, "string", null);
                		}
                	}
                }
			}
			if (ch.equals("m")) {
				String c = look(stream, -2);
				if (!(!c.equals("") && pattern[13].matcher(c).matches())) {
					c = stream.eat(pattern[14]);
					if (!c.equals("")) {
						if (pattern[15].matcher(c).matches())
							return tokenChain(stream, state, new String[] {c}, RXstyle, RXmodifiers);
						if (c.equals("("))
							return tokenChain(stream, state, new String[] {")"}, RXstyle, RXmodifiers);
						if (c.equals("["))
							return tokenChain(stream, state, new String[] {"]"}, RXstyle, RXmodifiers);
						if (c.equals("{"))
							return tokenChain(stream, state, new String[] {"}"}, RXstyle, RXmodifiers);
						if (c.equals("<"))
							return tokenChain(stream, state, new String[] {">"}, RXstyle, RXmodifiers);
					}
				}
			}
			if (ch.equals("s")) {
				if (!pattern[16].matcher(look(stream, -2)).matches()) {
					String c = stream.eat(pattern[17]);
					if (!c.equals("")) {
						if (c.equals("["))
							return tokenChain(stream, state, new String[] {"]", "]"}, RXstyle, RXmodifiers);
						if (c.equals("{"))
							return tokenChain(stream, state, new String[] {"}", "}"}, RXstyle, RXmodifiers);
						if (c.equals("<"))
							return tokenChain(stream, state, new String[] {">", ">"}, RXstyle, RXmodifiers);
						if (c.equals("("))
							return tokenChain(stream, state, new String[] {")", ")"}, RXstyle, RXmodifiers);
						return tokenChain(stream, state, new String[] {c, c}, RXstyle, RXmodifiers);
					}
				}
			}
			if (ch.equals("y")) {
				if (!pattern[18].matcher(look(stream, -2)).matches()) {
					String c = stream.eat(pattern[19]);
					if (!c.equals("")) {
						if (c.equals("["))
							return tokenChain(stream, state, new String[] {"]", "]"}, RXstyle, RXmodifiers);
						if (c.equals("{"))
							return tokenChain(stream, state, new String[] {"}", "}"}, RXstyle, RXmodifiers);
						if (c.equals("<"))
							return tokenChain(stream, state, new String[] {">", ">"}, RXstyle, RXmodifiers);
						if (c.equals("("))
							return tokenChain(stream, state, new String[] {")", ")"}, RXstyle, RXmodifiers);
						return tokenChain(stream, state, new String[] {c, c}, RXstyle, RXmodifiers);
					}
				}
			}
			if (ch.equals("t")) {
				if (!pattern[20].matcher(look(stream, -2)).matches()) {
					String c = stream.eat("r");
					if (!c.equals("")) {
						c=stream.eat(pattern[21]);
						if (!c.equals("")) {
							if (c.equals("["))
								return tokenChain(stream, state, new String[] {"]", "]"}, RXstyle, RXmodifiers);
							if (c.equals("{"))
								return tokenChain(stream, state, new String[] {"}", "}"}, RXstyle, RXmodifiers);
							if (c.equals("<"))
								return tokenChain(stream, state, new String[] {">", ">"}, RXstyle, RXmodifiers);
							if (c.equals("("))
								return tokenChain(stream, state, new String[] {")", ")"}, RXstyle, RXmodifiers);
							return tokenChain(stream, state, new String[] {c, c}, RXstyle, RXmodifiers);
						}
					}
				}
			}
			if (ch.equals("`"))
				return tokenChain(stream, state, new String[] {ch}, "variable-2", null);
			if (ch.equals("/")) {
				if (!pattern[22].matcher(prefix(stream, 0)).matches())
					return "operator";
				else
					return tokenChain(stream, state, new String[] {ch}, RXstyle, RXmodifiers);
			}
			if (ch.equals("$")) {
				int p = stream.pos();
				if (stream.eatWhile(pattern[23]) || !stream.eat("{").isEmpty() &&
						stream.eatWhile(pattern[24]) && !stream.eat("}").isEmpty())
					return "variable-2";
				else
					stream.pos(p);
			}
            if (pattern[25].matcher(ch).matches()){
            	int p = stream.pos();
                if (!stream.eat("^").isEmpty() && !stream.eat(pattern[26]).isEmpty() ||
                		!pattern[27].matcher(look(stream, -2)).matches() &&
                		!stream.eat(pattern[28]).isEmpty()) {
                	String c = stream.current();
                	if (PERL.containsKey(c) && PERL.get(c) != null)
                		return "variable-2";
                }
                stream.pos(p);
            }
            if (pattern[29].matcher(ch).matches()) {
                if (stream.eatWhile(pattern[30]) || !stream.eat("{").isEmpty() && 
                		stream.eatWhile(pattern[31]) && !stream.eat("}").isEmpty()) {
                	String c=stream.current();
                	if (PERL.containsKey(c) && PERL.get(c) != null)
                		return "variable-2";
                	else
                		return "variable";
                }
            }
            if (ch.equals("#")) {
            	if (!look(stream, -2).equals("$")) {
            		stream.skipToEnd();
            		return "comment";
            	}
            }
            if (pattern[32].matcher(ch).matches()) {
                int p = stream.pos();
                stream.eatWhile(pattern[33]);
                if (PERL.containsKey(stream.current()) && PERL.get(stream.current()) != null)
                	return "operator";
                else
                	stream.pos(p);
            }
            if (ch.equals("_")) {
            	if (stream.pos() == 1) {
            		if (suffix(stream, 6).equals("_END__"))
            			return tokenChain(stream,state, new String[] {"\0"}, "comment", null);
            		else if (suffix(stream, 7).equals("_DATA__"))
            			return tokenChain(stream,state, new String[] {"\0"}, "variable-2", null);
            		else if (suffix(stream, 7).equals("_C__"))
            			return tokenChain(stream,state, new String[] {"\0"}, "string", null);
            	}
            }
            if (pattern[34].matcher(ch).matches()) {
                int p = stream.pos();
                if (look(stream, -2).equals("{") && (look(stream, 0).equals("}") ||
                		stream.eatWhile(pattern[35]) && look(stream, 0).equals("}")))
                	return "string";
                else
                	stream.pos(p);
            }
            if (pattern[36].matcher(ch).matches()) {
                String l = look(stream, -2);
                int p = stream.pos();
                stream.eatWhile(pattern[37]);
                if (pattern[38].matcher(look(stream, 0)).matches()) {
                	stream.pos(p);
                } else {
                	int[] c = PERL.get(stream.current());
                	if (c == null)
                		return "meta";
                	if (!l.equals(":")) {
                		if (c[0] == 1)
                			return "keyword";
                		else if (c[0] == 2)
                			return "def";
                		else if (c[0] == 3)
                			return "atom";
                		else if (c[0] == 4)
                			return "operator";
                		else if (c[0] == 5)
                			return "variable-2";
                		else
                			return "meta";
                	} else {
                		return "meta";
                	}
                }
            }
            if (pattern[39].matcher(ch).matches()) {
                String l = look(stream, -2);
                stream.eatWhile(pattern[40]);
                int[] c = PERL.get(stream.current());
                if (c == null)
                	return "meta";
                if (!l.equals(":")) {
                	if (c[0] == 1)
                		return "keyword";
                	else if (c[0] == 2)
                		return "def";
                	else if (c[0] == 3)
                		return "atom";
                	else if (c[0] == 4)
                		return "operator";
                	else if (c[0] == 5)
                		return "variable-2";
                	else
                		return "meta";
                } else {
                	return "meta";
                }
            }
            return "";
		}
	}
	
	@Override
	public boolean accept(String fileName) {
		return acceptExtensions(fileName, "pl", "prl", "pm");
	}

	@Override
	public State startState() {
		return new State(new TokenPerl(), "", "", null);
	}

	@Override
	public String token(StringStream stream, State state) {
		return (state.tokenize == null ? new TokenPerl() : state.tokenize).process(stream, state);
	}
	
	@Override
	public boolean acceptMime(String mime) {
		return mime != null && mime.equals("text/x-perl");
	}

	@Override
	public boolean acceptMode(String mode) {
		return mode != null && mode.equals("perl");
	}
}
