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
package org.ioc.web.model.http;

import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpRequest;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class Cookie implements Comparable<Cookie> {
	private String name;
	private String value;
	private String host;
	private String path;
	private String domain;
	private long createTime = System.currentTimeMillis();
	private long maxAge;
	private boolean httpOnly = true;
	private boolean secure = false;

	public Cookie(String name, String value, long maxAge) {
		this.name = name;
		this.value = value;
		this.maxAge = maxAge;
	}

	public Cookie(io.netty.handler.codec.http.cookie.Cookie cookie) {
		name = cookie.name();
		value = cookie.value();
		domain = cookie.domain();
		path = cookie.path();
		httpOnly = cookie.isHttpOnly();
		secure = cookie.isSecure();
		maxAge = cookie.maxAge();
	}

	public static Set<Cookie> doDecode(HttpRequest request) {
		final String header = request.headers().get(HttpHeaders.COOKIE);
		final Set<io.netty.handler.codec.http.Cookie> decode = CookieDecoder.decode(header);
		return decode.stream().map(Cookie::new).collect(Collectors.toSet());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public long getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Cookie)) {
			return false;
		}
		Cookie cookie = (Cookie) o;
		return createTime == cookie.createTime &&
				maxAge == cookie.maxAge &&
				isHttpOnly() == cookie.isHttpOnly() &&
				secure == cookie.secure &&
				Objects.equals(name, cookie.name) &&
				Objects.equals(value, cookie.value) &&
				Objects.equals(getHost(), cookie.getHost()) &&
				Objects.equals(getPath(), cookie.getPath()) &&
				Objects.equals(domain, cookie.domain);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value, getHost(), getPath(), domain, createTime, maxAge, isHttpOnly(), secure);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getName())
				.append("=")
				.append(getValue());
		if (maxAge != -1) {
			builder.append("; Max-Age")
					.append("=")
					.append(getMaxAge());
		}

		if (httpOnly) {
			builder.append("; HttpOnly");
		}

		if (secure) {
			builder.append("; Secure");
		}

		return builder.toString();
	}

	@Override
	public int compareTo(Cookie o) {
		return o.getName().compareTo(getName());
	}
}
