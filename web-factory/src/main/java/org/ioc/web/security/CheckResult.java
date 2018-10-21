/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of DI (IoC) Container Project.
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
package org.ioc.web.security;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class CheckResult {
	private boolean isOk;
	private boolean isResource;
	private Throwable throwable;

	public CheckResult(boolean isOk) {
		this(isOk, false);
	}

	public CheckResult(boolean isOk, boolean isResource) {
		this.isOk = isOk;
		this.isResource = isResource;
	}

	public boolean isOk() {
		return isOk;
	}

	public void setOk(boolean ok) {
		isOk = ok;
	}

	public boolean isResource() {
		return isResource;
	}

	public void setResource(boolean resource) {
		isResource = resource;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}
}
