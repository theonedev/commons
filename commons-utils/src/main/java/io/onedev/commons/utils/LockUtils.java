package io.onedev.commons.utils;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;

public class LockUtils {
	
    private final static Map<String, Lock> locks = 
    		new ReferenceMap<String, Lock>(ReferenceStrength.HARD, ReferenceStrength.WEAK);
    
    private final static Map<String, ReadWriteLock> rwLocks = 
    		new ReferenceMap<String, ReadWriteLock>(ReferenceStrength.HARD, ReferenceStrength.WEAK);

    /**
     * Get named lock. 
     * 
     * @param name
     * 			name of the lock
     * @return
     * 			lock associated with specified name
     */
    public static Lock getLock(String name, boolean fair) {
    	Lock lock;
    	synchronized (locks) {
	        lock = locks.get(name);
	        if (lock == null) {
	        	lock = new ReentrantLock(fair);
	        	locks.put(name, lock);
	        } 
    	}
    	return lock;
    }
    
    public static Lock getLock(String name) {
    	return getLock(name, false);
    }
    
    /**
     * Get named read write lock. 
     * 
     * @param name
     * 			name of the read write lock
     * @return
     * 			read write lock associated with specified name
     */
    public static ReadWriteLock getReadWriteLock(String name, boolean fair) {
    	ReadWriteLock lock;
    	synchronized (rwLocks) {
	        lock = rwLocks.get(name);
	        if (lock == null) {
	        	lock = new ReentrantReadWriteLock(fair);
	        	rwLocks.put(name, lock);
	        } 
    	}
    	return lock;
    }

    public static ReadWriteLock getReadWriteLock(String name) {
    	return getReadWriteLock(name, false);
    }
    
    /**
     * Execute specified callable in lock of specified name.
     * 
     * @param name
     * 			name of the lock to be acquired
     * @param callable
     * 			callable to be execute within the named lock
     * @return
     * 			return value of the callable
     */
    public static <T> T call(String name, boolean fair, Callable<T> callable) {
    	Lock lock = getLock(name, fair);
		try {
			lock.lockInterruptibly();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
    		return callable.call();
    	} catch (Exception e) {
    		throw new RuntimeException(e);
		} finally {
    		lock.unlock();
    	}
    }

	public static void run(String name, boolean fair, Runnable runnable) {
		call(name, fair, () -> {
			runnable.run();
			return null;
		});
	}

    public static <T> T call(String name, Callable<T> callable) {
    	return call(name, false, callable);
    }

	public static void run(String name, Runnable runnable) {
		call(name, false, () -> {
			runnable.run();
			return null;
		});
	}

    /**
     * Execute specified callable in read lock of specified name.
     * 
     * @param name
     * 			name of the lock to be acquired
     * @param callable
     * 			callable to be execute within the named read lock
     * @return
     * 			return value of the callable
     */
    public static <T> T read(String name, boolean fair, Callable<T> callable) {
		// must remain lock variable explicitly until lock is released, as otherwise
		// the lock will be GCed due to weak reference
		var lock = getReadWriteLock(name, fair);
		try {
			lock.readLock().lockInterruptibly();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
    		return callable.call();
    	} catch (Exception e) {
    		throw new RuntimeException(e);
		} finally {
			lock.readLock().unlock();
    	}
    }

	public static void read(String name, boolean fair, Runnable runnable) {
		read(name, fair, () -> {
			runnable.run();
			return null;
		});
	}

    public static <T> T read(String name, Callable<T> callable) {
    	return read(name, false, callable);
    }

	public static void read(String name, Runnable runnable) {
		read(name, () -> {
			runnable.run();
			return null;
		});
	}
    
    /**
     * Execute specified callable in write lock of specified name.
     * 
     * @param name
     * 			name of the lock to be acquired
     * @param callable
     * 			callable to be execute within the named write lock
     * @return
     * 			return value of the callable
     */
    public static <T> T write(String name, boolean fair, Callable<T> callable) {
		// must remain lock variable explicitly until lock is released, as otherwise
		// the lock will be GCed due to weak reference
		var lock = getReadWriteLock(name, fair);
		try {
			lock.writeLock().lockInterruptibly();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		try {
    		return callable.call();
    	} catch (Exception e) {
    		throw new RuntimeException(e);
		} finally {
			lock.writeLock().unlock();
    	}
    }

	public static void write(String name, boolean fair, Runnable runnable) {
		write(name, fair, () -> {
			runnable.run();
			return null;
		});
	}

    public static <T> T write(String name, Callable<T> callable) {
    	return write(name, false, callable);
    }

	public static void write(String name, Runnable runnable) {
		write(name, () -> {
			runnable.run();
			return null;
		});
	}

}