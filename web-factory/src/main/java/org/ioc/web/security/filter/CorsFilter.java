/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of IoC Starter Project.
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
package org.ioc.web.security.filter;

import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.ioc.annotations.context.Order;
import org.ioc.web.model.http.RequestEntry;
import org.ioc.web.model.http.ResponseEntry;
import org.ioc.web.security.filter.exception.FilterException;

import static org.ioc.web.security.filter.CsrfFilter.X_CSRF_TOKEN_HEADER;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Order(100)
public class CorsFilter implements Filter {
	@Override
	public boolean doFilter(RequestEntry requestEntry, ResponseEntry responseEntry) throws FilterException {
		responseEntry.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "localhost"); //todo
		responseEntry.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

		if (requestEntry.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null && HttpMethod.OPTIONS.name().equals(requestEntry.getHttpMethod().name())) {
			// CORS "pre-flight" request
			responseEntry.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE");
			responseEntry.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, HttpHeaders.CONTENT_TYPE + ", " + X_CSRF_TOKEN_HEADER);
			responseEntry.addHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1");
		}

		return true;
	}
}
