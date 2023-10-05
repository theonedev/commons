package io.onedev.commons.utils;

import io.onedev.commons.bootstrap.Bootstrap;

import java.util.HashSet;
import java.util.Set;

public class ExceptionUtils extends org.apache.commons.lang3.exception.ExceptionUtils {
	
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T find(Throwable throwable, Class<T> exceptionClass) {
        if (exceptionClass.isAssignableFrom(throwable.getClass()))
            return (T) throwable;
        Set<Throwable> examinedCauses = new HashSet<Throwable>(); 
        examinedCauses.add(throwable);
        Throwable cause = throwable.getCause();
        while (cause != null) {
            if (exceptionClass.isAssignableFrom(cause.getClass()))
                return (T) cause;
            examinedCauses.add(cause);
            
            // cause.getCause() may return a previously examined cause
            if (examinedCauses.contains(cause.getCause()))
            	break;
            cause = cause.getCause();
        }
        return null;
    }
	
	public static RuntimeException unchecked(Throwable e) {
		return Bootstrap.unchecked(e);
	}

}
