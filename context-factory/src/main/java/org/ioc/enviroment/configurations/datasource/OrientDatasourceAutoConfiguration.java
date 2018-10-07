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
package org.ioc.enviroment.configurations.datasource;

import org.ioc.annotations.configuration.Property;
import org.ioc.annotations.configuration.PropertyFunction;
import org.ioc.context.factories.Factory;

import static org.ioc.context.factories.Factory.defaultDatabaseFactory;
import static org.ioc.utils.ReflectionUtils.instantiateClass;

/**
 * @author GenCloud
 * @date 09/2018
 */
@Property(prefix = "datasource.orient.")
public class OrientDatasourceAutoConfiguration {
	@Property(value = "database-type")
	private OrientType databaseType = OrientType.LOCAL;

	private String url = "./database";

	private String database = "orient";

	private String username = "admin";

	private String password = "admin";

	@Property(value = "ddl-auto")
	private DDL ddlAuto = DDL.dropCreate;

	@Property(value = "showSql")
	private boolean logQueries;

	public OrientType getDatabaseType() {
		return databaseType == null ? OrientType.LOCAL : databaseType;
	}

	public String getUrl() {
		return url == null || url.isEmpty() ? "./database" : url;
	}

	public String getDatabase() {
		return database == null || database.isEmpty() ? "orient" : database;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public DDL getDdlAuto() {
		return ddlAuto == null ? DDL.dropCreate : ddlAuto;
	}

	public boolean isLogQueries() {
		return logQueries;
	}

	@PropertyFunction
	public Object dataSource() {
		final Class<? extends Factory> factory = defaultDatabaseFactory();
		return instantiateClass(factory);
	}

	public enum DDL {
		create, dropCreate, refresh, none
	}

	public static enum OrientType {
		REMOTE,
		LOCAL,
		LOCAL_SERVER
	}
}
