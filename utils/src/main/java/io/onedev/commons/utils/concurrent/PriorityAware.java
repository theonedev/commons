package io.onedev.commons.utils.concurrent;

public interface PriorityAware extends Comparable<PriorityAware> {
	int getPriority();
}
