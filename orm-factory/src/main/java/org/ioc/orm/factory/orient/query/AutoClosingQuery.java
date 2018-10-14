/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.orm.factory.orient.query;

import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.utils.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Type of query that is automatically closed when receiving information from database.
 *
 * @author GenCloud
 * @date 10/2018
 */
public class AutoClosingQuery implements Iterable<ODocument>, Closeable, AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(AutoClosingQuery.class);

	private static final Executor EXECUTOR_SERVICE = Executors.newCachedThreadPool(r -> {
		final Thread thread = new Thread(r);
		thread.setName(AutoClosingQuery.class.getSimpleName() + "- Listener");
		thread.setDaemon(true);
		return thread;
	});

	private final ODatabaseDocument databaseDocument;
	private final String query;
	private final List<Object> parameterList;
	private final Map<String, Object> parameterMap;

	private OrientNonBlockingListener results = null;

	private boolean closed = false;

	private AutoClosingQuery(ODatabaseDocument databaseDocument, String query, Collection<?> params) {
		Assertion.checkNotNull(databaseDocument);
		Assertion.checkNotNull(query);

		this.databaseDocument = databaseDocument;
		this.query = query;

		parameterMap = Collections.emptyMap();
		if (params != null && !params.isEmpty()) {
			parameterList = new ArrayList<>(params);
		} else {
			parameterList = Collections.emptyList();
		}
	}

	public AutoClosingQuery(ODatabaseDocument databaseDocument, String query, Map<String, Object> params) {
		Assertion.checkNotNull(databaseDocument);
		Assertion.checkNotNull(query);

		this.databaseDocument = databaseDocument;
		this.query = query;

		parameterList = Collections.emptyList();
		if (params != null && !params.isEmpty()) {
			parameterMap = new LinkedHashMap<>(params);
		} else {
			parameterMap = Collections.emptyMap();
		}
	}

	private static void close(Object obj) throws Exception {
		if (obj == null) {
			return;
		}

		if (obj instanceof Closeable) {
			((Closeable) obj).close();
		} else if (obj instanceof AutoCloseable) {
			((AutoCloseable) obj).close();
		}
	}

	private static long timerDelay() {
		final String prop = System.getProperty("query.nonBlocking.timeOut");
		if (prop == null || prop.trim().isEmpty()) {
			return -1;
		}

		try {
			return Long.parseLong(prop);
		} catch (Exception e) {
			throw new OrmException("Unable to parse timeout value of [" + prop + "].", e);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			closeResults();
		} catch (Exception e) {
			throw new IOException("Unable to close results.");
		}

		synchronized (this) {
			closed = true;
		}
	}

	private boolean isClosed() {
		synchronized (this) {
			if (closed) {
				return true;
			}
		}

		return databaseDocument.isClosed();
	}

	private synchronized Iterator<?> exec() {
		try {
			closeResults();
		} catch (Exception e) {
			log.warn("Unable to close previous query results.", e);
		}

		final OrientNonBlockingListener blockingListener = new OrientNonBlockingListener();
		final Runnable runnable = () -> {
			try {
				databaseDocument.activateOnCurrentThread();
				final OSQLQuery q = new OSQLAsynchQuery(query, blockingListener);
				if (!parameterList.isEmpty()) {
					databaseDocument.query(q, parameterList.toArray());
				} else if (!parameterMap.isEmpty()) {
					databaseDocument.query(q, parameterMap);
				} else {
					databaseDocument.query(q);
				}
			} catch (Exception e) {
				log.error("Error during async query in listener thread.", e);
				blockingListener.offerElement(e);
			}
		};

		EXECUTOR_SERVICE.execute(runnable);

		results = blockingListener;
		return results;
	}

	private synchronized void closeResults() throws Exception {
		if (results != null) {
			close(results);
			results = null;
		}
	}

	@Override
	public Iterator<ODocument> iterator() {
		final Iterator<?> iterator = exec();
		if (iterator == null) {
			return Collections.emptyIterator();
		}

		return new Iterator<ODocument>() {
			@Override
			public boolean hasNext() {
				if (isClosed()) {
					return false;
				}

				if (!iterator.hasNext()) {
					try {
						close();
					} catch (Exception e) {
						log.warn("Unable to close results.", e);
					}
					return false;
				}

				return true;
			}

			@Override
			public ODocument next() {
				if (isClosed()) {
					throw new OrmException("Query is closed.");
				}

				final Object obj = iterator.next();
				if (obj == null) {
					return null;
				}

				if (obj instanceof ODocument) {
					return (ODocument) obj;
				} else if (obj instanceof ORecord) {
					return databaseDocument.load((ORecord) obj);
				} else if (obj instanceof OIdentifiable) {
					return databaseDocument.load(((OIdentifiable) obj).getIdentity());
				}

				throw new OrmException("Unexpected document type [" + obj.getClass() + "].");
			}
		};
	}

	private static class EndOfServiceElement {
		//Trigger class
	}

	private class OrientNonBlockingListener implements OCommandResultListener, Iterator<Object>, Closeable {
		private final BlockingQueue<Object> blockingQueue = new ArrayBlockingQueue<>(50);

		private Object next = null;
		private boolean done = false;
		private boolean needFetch = true;

		@Override
		public boolean result(Object record) {
			if (record == null) {
				return false;
			}

			if (isClosed() || isDone()) {
				return false;
			}

			return offerElement(record);
		}

		@Override
		public void end() {
			endService();
		}

		@Override
		public Object getResult() {
			return null;
		}

		@Override
		public void close() {
			if (!isDone()) {
				endService();
			}
		}

		private void endService() {
			synchronized (this) {
				done = true;
			}

			offerElement(new EndOfServiceElement());
		}

		private synchronized boolean isDone() {
			return done;
		}

		boolean offerElement(Object item) {
			final long fetchStartMs = System.currentTimeMillis();
			final long maxWaitMs = timerDelay();
			while (!isClosed() && !isDone()) {
				try {
					if (blockingQueue.offer(item, 100, TimeUnit.MILLISECONDS)) {
						return true;
					}
				} catch (InterruptedException e) {
					log.trace("Unable to offer vertex in non-blocking blockingQueue.", e);
					return false;
				}
				if (maxWaitMs > 0 && System.currentTimeMillis() - fetchStartMs > maxWaitMs) {
					if (log.isDebugEnabled()) {
						log.debug("Waited more than [{}] ms to add item to blockingQueue - offer aborted.", maxWaitMs);
					}
					return false;
				}
			}
			return false;
		}

		@Override
		public boolean hasNext() {
			if (needFetch) {
				if (isFetched()) {
					return false;
				}
			}

			return next != null;
		}

		@Override
		public Object next() {
			if (needFetch) {
				if (isFetched()) {
					return null;
				}
			}

			final Object document = next;
			next = null;
			needFetch = true;
			return document;
		}

		private boolean isFetched() {
			next = null;
			needFetch = true;
			final long fetchStartMs = System.currentTimeMillis();
			final long maxWaitMs = timerDelay();
			while (!blockingQueue.isEmpty() || (!isClosed() && !isDone())) {
				try {
					final Object item = blockingQueue.poll(100, TimeUnit.MILLISECONDS);
					if (item instanceof EndOfServiceElement) {
						next = null;
						needFetch = false;
						return true;
					} else if (item instanceof Throwable) {
						throw new OrmException("Unable to fetch next vertex from query.", (Throwable) item);
					} else if (item != null) {
						next = item;
						needFetch = false;
						return false;
					} else if (maxWaitMs > 0 && System.currentTimeMillis() - fetchStartMs > maxWaitMs) {
						if (log.isDebugEnabled()) {
							log.debug("Waited more than [{}] ms for item from blockingQueue - fetch aborted.", maxWaitMs);
						}
						return true;
					}
				} catch (InterruptedException e) {
					if (log.isDebugEnabled()) {
						log.debug("Unable to poll non-blocking blockingQueue.", e);
					}
					return true;
				}
			}
			return true;
		}
	}
}
