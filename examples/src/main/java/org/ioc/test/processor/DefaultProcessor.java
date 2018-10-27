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
package org.ioc.test.processor;

import org.ioc.context.processors.TypeProcessor;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCException;
import org.ioc.test.types.TypeA;
import org.ioc.test.types.TypeB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class DefaultProcessor implements TypeProcessor, ContextSensible {
	private final Logger log = LoggerFactory.getLogger(DefaultProcessor.class);

	@Override
	public Object afterComponentInitialization(String componentName, Object component) {
		if (component instanceof TypeA) {
			log.info("Sample changing TypeA bag after initialization");
		}
		return component;
	}

	@Override
	public Object beforeComponentInitialization(String componentName, Object component) {
		if (component instanceof TypeB) {
			log.info("Sample changing TypeB bag before initialization");
		}
		return component;
	}

	@Override
	public void contextInform(IoCContext context) throws IoCException {
		log.info("I'm informed for context - [{}]", context);
	}
}
