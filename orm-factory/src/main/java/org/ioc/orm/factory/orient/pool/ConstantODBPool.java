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
package org.ioc.orm.factory.orient.pool;

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import org.ioc.orm.factory.orient.ODBPool;

import static org.ioc.orm.util.OrientUtils.toUrl;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ConstantODBPool implements ODBPool {
	private final OPartitionedDatabasePool pool;

	public ConstantODBPool(String url, String database, String username, String password) {
		pool = new OPartitionedDatabasePool(toUrl(url, database), username, password);
	}

	@Override
	public ODatabaseDocument acquire() {
		return pool.acquire();
	}

	@Override
	public void close() {
		pool.close();
	}
}
