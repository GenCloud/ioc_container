package org.ioc.context.model.cache.expiring.lock;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

public class LimitLock implements Closeable {
	private final Lock lock;

	private final ThreadLocal<Integer> holdingThreads = new ThreadLocal<>();

	public LimitLock(Lock lock) {
		this.lock = lock;
	}

	public LimitLock acquire() {
		lock.lock();
		assert addCurrentThread();
		return this;
	}

	@Override
	public void close() {
		lock.unlock();
		assert removeCurrentThread();
	}

	private boolean addCurrentThread() {
		final Integer current = holdingThreads.get();
		holdingThreads.set(current == null ? 1 : current + 1);
		return true;
	}

	private boolean removeCurrentThread() {
		final Integer count = holdingThreads.get();
		assert count != null && count > 0;
		if (count == 1) {
			holdingThreads.remove();
		} else {
			holdingThreads.set(count - 1);
		}
		return true;
	}

	public boolean isHeldByCurrentThread() {
		final Integer count = holdingThreads.get();
		return count != null && count > 0;
	}
}
