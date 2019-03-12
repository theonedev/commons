package io.onedev.commons.utils.stringmatch;

import java.io.Serializable;
import java.util.Set;

public class PatternSet implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Set<String> includes;
	
	private final Set<String> excludes;
	
	public PatternSet(Set<String> includes, Set<String> excludes) {
		this.includes = includes;
		this.excludes = excludes;
	}

	public Set<String> getIncludes() {
		return includes;
	}

	public Set<String> getExcludes() {
		return excludes;
	}

	public boolean matches(Matcher matcher, String value) {
		for (String exclude: excludes) {
			if (matcher.matches(exclude, value))
				return false;
		}
		for (String include: includes) {
			if (matcher.matches(include, value))
				return true;
		}
		return false;
	}
	
}
