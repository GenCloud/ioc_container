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
package org.ioc.aop;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class JunctionDot {
	private volatile boolean processed;
	private Object target;
	private Object proxy;
	private Method method;
	private Object[] args;
	private Exception exception;
	private Object returnValue;

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Object getProxy() {
		return proxy;
	}

	public void setProxy(Object proxy) {
		this.proxy = proxy;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public Object getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(Object returnValue) {
		this.returnValue = returnValue;
	}

	public void process() {
		if (!processed) {
			try {
				this.returnValue = method.invoke(target, args);
			} catch (Exception e) {
				this.exception = e;
			}
			this.processed = true;
		}
	}

	@Override
	public String toString() {
		return "JunctionDot{" +
				"processed=" + processed +
				", target=" + target +
				", proxy=" + proxy +
				", method=" + method +
				", args=" + Arrays.toString(args) +
				", exception=" + exception +
				", returnValue=" + returnValue +
				'}';
	}
}
