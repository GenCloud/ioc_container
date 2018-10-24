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
package org.ioc.enviroment.configurations;

import org.ioc.annotations.configuration.Property;
import org.ioc.annotations.configuration.PropertyFunction;
import org.ioc.context.factories.Factory;

import static org.ioc.context.factories.Factory.defaultResourceManagerFactory;
import static org.ioc.utils.ReflectionUtils.instantiateClass;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Property(prefix = "messages.")
public class BundleAutoConfiguration {
	private boolean enabled;

	@Property("file.name")
	private String fileName;

	@Property(value = "allowed.locales", splitter = ",")
	private String[] allowedLocales;

	public String getFileName() {
		return fileName;
	}

	public String[] getAllowedLocales() {
		return allowedLocales;
	}

	@PropertyFunction
	@SuppressWarnings("unchecked")
	public Object resourceFactory() {
		if (enabled) {
			final Class<? extends Factory> factory = defaultResourceManagerFactory();
			return instantiateClass(factory);
		}

		return null;
	}
}
