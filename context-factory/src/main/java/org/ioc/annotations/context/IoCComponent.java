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
package org.ioc.annotations.context;

import java.lang.annotation.*;

/**
 * Specifies that the class marked with the annotation must be initialized in the
 * contexts of the application, its automatic search when scanning application packages.
 *
 * @author GenCloud
 * @date 09/2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface IoCComponent {
	/**
	 * If the value is redefined, then the component (class) will be placed in
	 * the injection factories by its value, otherwise the original class name will be used.
	 *
	 * @return the name of the component (class), or the null value
	 */
	String value() default "";

	/**
	 * Identifier of access to the component (class)
	 *
	 * @return load option
	 */
	Mode scope() default Mode.SINGLETON;
}
