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

import io.netty.handler.codec.http.HttpMethod;
import org.ioc.annotations.context.Order;
import org.ioc.web.model.http.Cookie;
import org.ioc.web.model.http.RequestEntry;
import org.ioc.web.model.http.ResponseEntry;
import org.ioc.web.security.filter.exception.FilterException;
import org.ioc.web.security.filter.exception.InvalidCsrfTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Order()
public class CsrfFilter implements Filter {
	static final String X_CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";
	private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);
	private static final String CSRF_TOKEN_COOKIE = "CSRF-TOKEN";

	private final Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");

	@Override
	public boolean doFilter(RequestEntry requestEntry, ResponseEntry responseEntry) throws FilterException {
		final HttpMethod method = requestEntry.getHttpMethod();
		if (!allowedMethods.matcher(method.name()).matches()) {
			final String csrfTokenValue = requestEntry.getHttpRequest().headers().get(X_CSRF_TOKEN_HEADER);

			String csrfCookieValue = null;
			final Cookie requestCookie = requestEntry.getCookie(CSRF_TOKEN_COOKIE);
			if (requestCookie != null) {
				csrfCookieValue = requestCookie.getValue();
			}

			if (csrfTokenValue == null || !csrfTokenValue.equals(csrfCookieValue)) {
				log.warn("Missing/bad CSRF-TOKEN while CSRF is enabled for request {}", requestEntry.getPath());
				throw new InvalidCsrfTokenException("Missing/bad CSRF-TOKEN while CSRF is enabled for request " + requestEntry.getPath());
			}

			final Cookie csrf = new Cookie(CSRF_TOKEN_COOKIE, "", 0);
			responseEntry.addCookie(csrf);
		}

		return true;
	}
}
