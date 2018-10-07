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
package org.ioc.context.listeners.facts;

import org.ioc.context.listeners.AbstractFact;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class OnTypeInitFact extends AbstractFact {
	private final String componentName;

	/**
	 * Constructs a prototypical Event.
	 *
	 * @param source        The object on which the Event initially occurred.
	 * @param componentName component name
	 * @throws IllegalArgumentException if source is null.
	 */
	public OnTypeInitFact(String componentName, Object source) {
		super(source);
		this.componentName = componentName;
	}

	public String getComponentName() {
		return componentName;
	}
}
