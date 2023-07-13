package io.onedev.commons.loader;

import java.util.Date;
import java.util.Set;

import io.onedev.commons.utils.DependencyAware;

public interface Plugin extends DependencyAware<String> {
	
	String getId();

	String getName();
	
	String getVendor();
	
	String getVersion();

	String getDescription();

	Date getDate();
	
	boolean isProduct();

	Set<String> getDependencies();

	/**
	 * This function will be called before starting other plugins depending on this plugin.
	 */
	public void start();
	
	/**
	 * This function will be called after other plugins depending on this plugin have been started.
	 */
	public void postStart();
	
	/**
	 * This function will be called before stopping other plugins depending on this plugin.
	 */
	public void preStop();
	
	/**
	 * This function will be called after other plugins depending on this plugin have been stopped.
	 */
	public void stop();

}
