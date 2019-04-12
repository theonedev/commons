package io.onedev.commons.utils.concurrent;

import java.util.concurrent.Callable;

import io.onedev.commons.utils.ExceptionUtils;

public class Capacity {
	
	private final int max;
	
	private int active;
	
	public Capacity(int max) {
		this.max = max;
	}
	
	public <T> T call(Callable<T> callable) {
		synchronized (this) {
			while (active >= max) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			active++;
		}
		
		try {
			return callable.call();
		} catch (Exception e) {
			throw ExceptionUtils.unchecked(e);
		} finally {
			synchronized (this) {
				active--;
				notify();
			}
		}
	}

	public void run(Runnable runnable) {
		call(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				runnable.run();
				return null;
			}
			
		});
	}
	
	public synchronized boolean hasMore() {
		return active < max;
	}
	
}
