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
package org.ioc.web.annotations;

import org.ioc.web.model.rate.RateLimitType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Describes rate limit attributes on method.
 * Annotation for specifying method rate limit configuration
 * will be evaluated to decide whether method invocation is allowed or not.
 *
 * @author GenCloud
 * @date 10/2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
@Documented
public @interface RateLimit {
	/**
	 * Returns max number of calls per TimeUnit.
	 *
	 * @return check limit
	 */
	int limit() default 1;

	/**
	 * @return RateLimitType
	 */
	RateLimitType type() default RateLimitType.RemoteAddress;

	/**
	 * Returns TimeUnit.
	 *
	 * @return TimeUnit to measure number of calls
	 */
	TimeUnit timeUnit() default TimeUnit.SECONDS;

	/**
	 * Returns header name for type -> HeaderValue.
	 * Header value will be evaluated in request.
	 *
	 * @return name of HTTP header
	 */
	String headerName() default "";

	/**
	 * Returns header name for type -> CookieValue.
	 * Cookie value will be evaluated in request.
	 *
	 * @return name of cookie
	 */
	String cookieName() default "";
}

