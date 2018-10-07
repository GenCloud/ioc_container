/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of DI (IoC) Container Project.
 *
 * DI (IoC) Container Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DI (IoC) Container Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DI (IoC) Container Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.orm.factory.orient.pool;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.orient.ODBPool;

import java.util.*;

/**
 * Local (disk path) database schema manager.
 *
 * @author GenCloud
 * @date 10/2018
 */
public class LocalODBPool implements ODBPool {
	private final String url;

	private final String username;

	private final String password;

	private final Set<ODatabaseDocument> opened = new HashSet<>();

	private final Timer timer = new Timer(getClass().getName() + "- Timer", true);

	private final long timerDelay = timerDelay();

	public LocalODBPool(String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
	}

	private static long timerDelay() {
		final String prop = System.getProperty("local.pool.closeDelay");
		if (prop == null || prop.trim().isEmpty()) {
			return 0;
		}

		try {
			return Long.parseLong(prop);
		} catch (Exception e) {
			throw new OrmException("Unable to parse close delay value of [" + prop + "].", e);
		}
	}

	@Override
	public synchronized ODatabaseDocument acquire() {
		if (opened.isEmpty() && timerDelay > 0) {
			Orient.instance().startup();
		}

		final ODatabaseDocument documentTx = new ODatabaseDocumentTx(url);
		if (!documentTx.exists()) {
			documentTx.create();
		} else {
			documentTx.open(username, password);
		}

		documentTx.activateOnCurrentThread();

		opened.add(documentTx);

		if (timerDelay > 0) {
			timer.schedule(new CloseTask(), timerDelay);
		}

		return documentTx;
	}

	private synchronized boolean checkOpen() {
		if (opened.isEmpty()) {
			return false;
		}

		new ArrayList<>(opened).stream().filter(ODatabaseDocument::isClosed).forEach(opened::remove);

		if (opened.isEmpty() && timerDelay > 0) {
			Orient.instance().shutdown();
			return false;
		} else {
			return true;
		}
	}

	@Override
	public synchronized void close() {
		timer.cancel();
		opened.clear();
		Orient.instance().shutdown();
	}

	private class CloseTask extends TimerTask {
		@Override
		public void run() {
			if (checkOpen()) {
				timer.schedule(new CloseTask(), timerDelay);
			}
		}
	}
}
