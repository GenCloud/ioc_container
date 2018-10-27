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
package org.ioc.web.model.rate;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Class holding method execution context.
 *
 * @author GenCloud
 * @date 10/2018
 */
public class RateLimitModel {
	private final Method method;
	private final int limit;
	private final RateLimitType rateLimitType;
	private final TimeUnit unit;
	private final String value;

	public RateLimitModel(Method method, int limit, RateLimitType rateLimitType, TimeUnit unit, String value) {
		this.method = method;
		this.limit = limit;
		this.rateLimitType = rateLimitType;
		this.unit = unit;
		this.value = value;
	}

	public Method getMethod() {
		return method;
	}

	public int getLimit() {
		return limit;
	}

	public RateLimitType getRateLimitType() {
		return rateLimitType;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RateLimitModel)) return false;
		RateLimitModel that = (RateLimitModel) o;
		return getLimit() == that.getLimit() &&
				Objects.equals(getMethod(), that.getMethod()) &&
				getRateLimitType() == that.getRateLimitType() &&
				getUnit() == that.getUnit() &&
				Objects.equals(getValue(), that.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getMethod(), getLimit(), getRateLimitType(), getUnit(), getValue());
	}

	@Override
	public String toString() {
		return "RateLimitModel{" +
				"method=" + method +
				", limit=" + limit +
				", type=" + rateLimitType +
				", timeUnit=" + unit +
				", evaluatedValue='" + value + '\'' +
				'}';
	}
}
