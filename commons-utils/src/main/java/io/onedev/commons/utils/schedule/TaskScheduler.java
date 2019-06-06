package io.onedev.commons.utils.schedule;

public interface TaskScheduler {
	
	void start();
	
	String schedule(SchedulableTask task);
	
	void unschedule(String taskId);

	void stop();
	
}
