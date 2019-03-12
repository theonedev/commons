package io.onedev.commons.utils.init;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public abstract class ManualConfig implements Serializable {

	private final String message;
	
	private final Serializable setting;
	
	private final Set<String> excludedProperties;
	
	public ManualConfig(String message, Serializable setting, Set<String> excludedProperties) {
		this.message = message;
		this.setting = setting;
		this.excludedProperties = excludedProperties;
	}
	
	public ManualConfig(String message, Serializable setting) {
		this(message, setting, new HashSet<>());
	}
	
	public String getMessage() {
		return message;
	}
	
	public Serializable getSetting() {
		return setting;
	}

	public Set<String> getExcludeProperties() {
		return excludedProperties;
	}

	public abstract Skippable getSkippable();
	
	public abstract void complete();
	
}
