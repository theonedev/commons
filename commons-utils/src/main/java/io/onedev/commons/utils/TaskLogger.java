package io.onedev.commons.utils;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

public abstract class TaskLogger {

	private static final Pattern EOL_PATTERN = Pattern.compile("\r?\n");
	
	public void log(String message, Throwable t) {
		StringBuilder builder = new StringBuilder(message);
		for (String line: Splitter.on(EOL_PATTERN).split(Throwables.getStackTraceAsString(t))) {
			if (builder.length() == 0)
				builder.append(line);
			else
				builder.append("\n    ").append(line);
		}
		log(builder.toString());
	}

	public abstract void log(String message, @Nullable String sessionId);

	public void log(String message) {
		log(message, (String)null);
	}
	
	public void error(String message, Throwable t) {
		log(wrapWithAnsiError(message), t);
	}

	public void error(String message) {
		log(wrapWithAnsiError(message));
	}
	
	public void warning(String message, Throwable t) {
		log(wrapWithAnsiWarning(message), t);
	}

	public void warning(String message) {
		log(wrapWithAnsiWarning(message));
	}

	public void emphasize(String message, Throwable t) {
		log(wrapWithAnsiEmphasize(message), t);
	}

	public void emphasize(String message) {
		log(wrapWithAnsiEmphasize(message));
	}
	
	public static String wrapWithAnsiError(String text) {
		return "\033[1;31m" + text + "\033[0m";
	}
	
	public static String wrapWithAnsiWarning(String text) {
		return "\033[1;33m" + text + "\033[0m";
	}
	
	public static String wrapWithAnsiEmphasize(String text) {
		return "\033[1;35m" + text + "\033[0m";
	}
	
}
