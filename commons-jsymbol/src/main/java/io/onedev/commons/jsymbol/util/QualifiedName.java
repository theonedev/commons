package io.onedev.commons.jsymbol.util;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

public class QualifiedName implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String prefix;
	
	private final String unqualified;
	
	private final String suffix;
	
	public QualifiedName(String unqualifiedName, @Nullable String prefix, @Nullable String suffix) {
		this.unqualified = unqualifiedName;
		this.prefix = prefix;
		this.suffix = suffix;
	}
	
	public QualifiedName(String qualifiedName, String...qualifierSeparators) {
		int index = -1;
		for (String separator: qualifierSeparators) {
			int currentIndex = qualifiedName.lastIndexOf(separator);
			if (currentIndex != -1 && index < currentIndex + separator.length())
				index = currentIndex + separator.length();
		}
		if (index != -1) {
			unqualified = qualifiedName.substring(index);
			prefix = qualifiedName.substring(0, index);
			suffix = null;
		} else {
			unqualified = qualifiedName;
			prefix = suffix = null;
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public String getUnqualified() {
		return unqualified;
	}

	public String getSuffix() {
		return suffix;
	}

	@Override
	public String toString() {
		return (prefix!=null?prefix:"") + unqualified + (suffix!=null?suffix:"");
	}
	
}
