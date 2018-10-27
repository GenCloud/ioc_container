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
package org.ioc.web.interceptors;

import org.ioc.context.factories.cache.ExpiringCacheFactory;
import org.ioc.context.model.cache.ICache;
import org.ioc.context.model.cache.expiring.Loader;
import org.ioc.web.annotations.RateLimit;
import org.ioc.web.exception.RateLimitException;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.http.Cookie;
import org.ioc.web.model.http.RequestEntry;
import org.ioc.web.model.http.ResponseEntry;
import org.ioc.web.model.mapping.Mapping;
import org.ioc.web.model.rate.RateLimitModel;
import org.ioc.web.model.rate.RateLimitStereotype;
import org.ioc.web.model.rate.RateLimitType;
import org.ioc.web.security.interceptors.HttpRequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import static org.ioc.web.model.rate.RateLimitType.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class RateLimitInterceptor implements HttpRequestInterceptor {
	private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

	private final Loader<RateLimitModel, RateLimitStereotype> loader = model -> new RateLimitStereotype(model.getUnit(), model.getLimit());

	private final ICache<RateLimitModel, RateLimitStereotype> cache;

	public RateLimitInterceptor(ExpiringCacheFactory expiringCacheFactory) {
		cache = expiringCacheFactory.installEternal("expiring-cache", 20000);
	}

	@Override
	public boolean preHandle(RequestEntry requestEntry, ResponseEntry responseEntry, ModelAndView modelAndView, Mapping mapping) {
		if (mapping == null) {
			return true;
		}

		final Method method = mapping.getMethod();
		final RateLimit rateLimit = method.getAnnotation(RateLimit.class);
		if (rateLimit == null) {
			return true;
		}

		final String config = readConfig(rateLimit, requestEntry);
		final RateLimitModel model = new RateLimitModel(method, rateLimit.limit(), rateLimit.type(),
				rateLimit.timeUnit(), config);

		final boolean requestAllowed = check(model);

		if (!requestAllowed) {
			if (log.isDebugEnabled()) {
				log.debug("Block request [{}] with rate limiter: type={}, value={}", requestEntry.getHttpRequest().uri(),
						rateLimit.type(), config);
			}

			throw new RateLimitException("Block request [" + requestEntry.getHttpRequest().uri() + "] with rate limiter: type="
					+ rateLimit.type() + ", value=" + config);
		}

		return true;
	}

	@Override
	public void postHandle(RequestEntry requestEntry, ResponseEntry responseEntry, ModelAndView modelAndView, Mapping mapping) {
	}

	private boolean check(RateLimitModel model) {
		try {
			final RateLimitStereotype stereotype = cache.computeIfAbsent(model, loader);
			stereotype.removeOldest();
			return stereotype.check();
		} catch (ExecutionException e) {
			if (log.isErrorEnabled()) {
				log.error("Exception occurred while calculating rate limit value", e);
			}
		}
		return true;
	}

	private String readConfig(RateLimit rateLimit, RequestEntry requestEntry) {
		String value = null;
		RateLimitType type = rateLimit.type();
		if (type == CookieValue) {
			if (!rateLimit.cookieName().isEmpty()) {
				final Cookie cookie = requestEntry.getCookie(rateLimit.cookieName());
				if (cookie != null) {
					value = cookie.getValue();
				}
			} else {
				if (log.isWarnEnabled()) {
					log.warn("Cant resolve HTTP cookie value for empty cookie name, check @RateLimit configuration.");
				}
			}
		} else if (type == HeaderValue) {
			if (!rateLimit.headerName().isEmpty()) {
				value = requestEntry.getHeader(rateLimit.headerName());
			} else {
				if (log.isWarnEnabled()) {
					log.warn("Cant resolve HTTP header value for empty header name, check @RateLimit configuration.");
				}
			}
		} else if (type == RemoteAddress) {
			final InetSocketAddress address = (InetSocketAddress) requestEntry.getChannel().remoteAddress();
			value = address.getAddress().getHostAddress();
		}

		return value;
	}
}
