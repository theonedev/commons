package io.onedev.commons.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CollectionUtils {
	
	public static <T> Set<T> findDuplicates(Collection<T> collection) {
	    Set<T> uniques = new HashSet<>();
	    return collection.stream().filter(e -> !uniques.add(e)).collect(Collectors.toSet());
	}
	
}
