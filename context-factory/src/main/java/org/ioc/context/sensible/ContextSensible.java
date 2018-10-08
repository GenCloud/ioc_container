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
package org.ioc.context.sensible;

import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCException;

/**
 * Any component can implement this interface to obtain information about the running {@link IoCContext}.
 *
 * @author GenCloud
 * @date 09/2018
 */
@FunctionalInterface
public interface ContextSensible extends Sensible {
	/**
	 * Set the {@link IoCContext} to component.
	 *
	 * @param ioCContext initialized application contexts
	 * @throws IoCException throw if contexts throwing by methods
	 */
	void contextInform(IoCContext ioCContext) throws IoCException;
}
