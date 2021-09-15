package io.onedev.commons.utils;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

public abstract class TaskLogger {

	private static final Pattern EOL_PATTERN = Pattern.compile("\r?\n");
	
	public void log(@Nullable String message, Throwable t) {
		log(toString(message, t));
	}
	
	public static String toString(@Nullable String message, Throwable t) {
		StringBuilder builder = new StringBuilder();
		if (message != null)
			builder.append(message).append(": ");
		boolean firstLine = true;
		for (String line: Splitter.on(EOL_PATTERN).split(Throwables.getStackTraceAsString(t))) {
			if (firstLine)
				builder.append(line);
			else
				builder.append("\n    ").append(line);
			firstLine = false;
		}
		return builder.toString();
	}

	public abstract void log(String message, @Nullable String sessionId);

	public void log(String message) {
		log(message, (String)null);
	}
	
	public void error(String message, Throwable t) {
		log(wrapWithAnsiError(toString(message, t)));
	}

	public void error(String message) {
		log(wrapWithAnsiError(message));
	}
	
	public void warning(String message) {
		log(wrapWithAnsiWarning(message));
	}

	public void notice(String message) {
		log(wrapWithAnsiNotice(message));
	}
	
	public void success(String message) {
		log(wrapWithAnsiSuccess(message));
	}
	
	public static String wrapWithAnsiSuccess(String text) {
		return "\033[1;32m" + text + "\033[0m";
	}
	
	public static String wrapWithAnsiError(String text) {
		return "\033[1;31m" + text + "\033[0m";
	}
	
	public static String wrapWithAnsiWarning(String text) {
		return "\033[1;33m" + text + "\033[0m";
	}
	
	public static String wrapWithAnsiNotice(String text) {
		return "\033[1;35m" + text + "\033[0m";
	}
	
}
