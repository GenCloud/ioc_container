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
package org.ioc.web.handler.metadata;

import java.util.Objects;

/**
 * @author GenCloud
 * @date 10/2018
 */
public enum ProtocolType {
	HTTP_1_0("http/1.0"), HTTP_1_1("http/1.1"), HTTP_2_0("h2-15");

	private final String type;

	ProtocolType(String type) {
		this.type = type;
	}

	public static ProtocolType ofType(String type) {
		if (Objects.equals(type, HTTP_1_0.type)) {
			return HTTP_1_0;
		}

		if (Objects.equals(type, HTTP_1_1.type)) {
			return HTTP_1_1;
		}

		if (Objects.equals(type, HTTP_2_0.type)) {
			return HTTP_2_0;
		}

		throw new IllegalArgumentException("Unexpected type: " + type);
	}
}
