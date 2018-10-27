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
package org.ioc.test.environments;

import org.ioc.enviroment.listeners.IEnvironmentFact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class SampleEnvironmentFact implements IEnvironmentFact {
	private static final Logger log = LoggerFactory.getLogger(SampleEnvironmentFact.class);

	@Override
	public void preParseEnvironment(String path) {
		log.info("Loading: [{}] - [{}] environment class", path, getClass().getSimpleName());
	}

	@Override
	public void missPropertyEvent(String name) {
		log.warn("Missing property key - [{}]", name);
	}

	@Override
	public void postParseEnvironment(String path) {
		log.info("Successfully loaded: [{}] property addView", path);
	}

	@Override
	public void typeCastException(String name, String value) {
		log.warn("Invalid property - [{}]. Fail store value [{}]", name, value);
	}
}
