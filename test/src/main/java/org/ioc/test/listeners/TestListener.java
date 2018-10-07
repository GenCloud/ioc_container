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
package org.ioc.test.listeners;

import org.ioc.annotations.listeners.Fact;
import org.ioc.context.listeners.AbstractFact;
import org.ioc.context.listeners.IListener;
import org.ioc.context.listeners.facts.OnContextIsInitializedFact;
import org.ioc.context.listeners.facts.OnTypeInitFact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 09/2018
 */
@Fact
public class TestListener implements IListener {
	private final Logger log = LoggerFactory.getLogger(TestListener.class);

	@Override
	public boolean dispatch(AbstractFact abstractFact) {
		if (OnContextIsInitializedFact.class.isAssignableFrom(abstractFact.getClass())) {
			log.info("ListenerInform - Context is initialized! [{}]", abstractFact.getSource());
		} else if (OnTypeInitFact.class.isAssignableFrom(abstractFact.getClass())) {
			final OnTypeInitFact ev = (OnTypeInitFact) abstractFact;
			log.info("ListenerInform - Component [{}] in instance [{}] is initialized!", ev.getComponentName(), ev.getSource());
		}
		return true;
	}
}
