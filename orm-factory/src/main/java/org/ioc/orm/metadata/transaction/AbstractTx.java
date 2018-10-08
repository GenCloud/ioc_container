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

/**
 * @author GenCloud
 * @date 10/2018
 */
public abstract class AbstractTx implements ITransactional {
	@Override
	public void withTx(TxRunnable txRunnable) throws OrmException {
		try (Tx tx = openTx()) {
			tx.exec(txRunnable);
		} catch (Exception e) {
			throw new OrmException("Unable to exec transaction.", e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T withTx(TxCallable<T> tTxCallable) throws OrmException {
		final Object[] tmp = new Object[1];
		try (Tx tx = openTx()) {
			tmp[0] = tx.exec(tTxCallable);
		} catch (Exception e) {
			throw new OrmException("Unable to exec transaction.", e);
		}
		return (T) tmp[0];
	}

	@Override
	public Tx openTx() throws OrmException {
		return new Tx(this);
	}
}
