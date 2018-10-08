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
package org.ioc.orm.metadata.transaction;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.utils.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class Tx implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(Tx.class);

	private final ITransactional transactional;
	private final boolean ownsTransaction;

	private Boolean success = null;

	public Tx(ITransactional transactional) throws OrmException {
		Assertion.checkNotNull(transactional, "Tx");

		this.transactional = transactional;

		if (this.transactional.pending()) {
			ownsTransaction = false;
		} else {
			ownsTransaction = true;
			this.transactional.start();
		}
	}

	public void success() {
		if (FALSE.equals(success)) {
			log.warn("Moving transaction from 'failure' to 'success'.  Please ensure this is desired state.");
		}
		success = TRUE;
	}

	public void failure() {
		success = FALSE;
	}

	public void exec(TxRunnable runnable) throws OrmException {
		if (runnable == null) {
			return;
		}

		for (int i = 1; i <= 10; i++) {
			try {
				runnable.run(this);
				return;
			} catch (Exception e) {
				if (e.getClass().getSimpleName().equalsIgnoreCase("ONeedRetryException")) {
					log.info("Error executing unit of work, recovering from retry-exception [attempt #" + i + "].", e);
				} else {
					success = FALSE;
					throw new OrmException("Unable to exec unit of work.", e);
				}
			}
		}
	}

	public Object exec(TxCallable callable) throws OrmException {
		if (callable == null) {
			return null;
		}

		for (int i = 1; i <= 10; i++) {
			try {
				return callable.call(this);
			} catch (Exception e) {
				if (e.getClass().getSimpleName().equalsIgnoreCase("ONeedRetryException")) {
					log.info("Error executing unit of work, recovering from retry-exception [attempt #" + i + "].", e);
				} else {
					success = FALSE;
					throw new OrmException("Unable to exec unit of work.", e);
				}
			}
		}

		return null;
	}

	@Override
	public void close() {
		if (!ownsTransaction) {
			return;
		}

		if (TRUE.equals(success)) {
			transactional.commit();
		} else {
			transactional.rollback();
		}
	}
}
