package io.onedev.commons.launcher.loader;

import java.util.Collection;

public interface ImplementationProvider {
	
	Class<?> getAbstractClass();
	
	Collection<Class<?>> getImplementations();
}
