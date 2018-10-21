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
package org.ioc.web.model.http;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class Request {
	private FullHttpRequest httpRequest;

	private HttpMethod method;

	private Channel channel;

	private Set<Cookie> cookies = new TreeSet<>();

	public Request(Channel channel, FullHttpRequest httpRequest) {
		this.httpRequest = httpRequest;
		this.channel = channel;

		method = httpRequest.method();

		parseCookie();
	}

	private void parseCookie() {
		if (httpRequest.headers().contains("Cookie")) {
			cookies.addAll(Cookie.doDecode(httpRequest));
		}
	}

	public HttpMethod getHttpMethod() {
		return method;
	}

	public Channel getChannel() {
		return channel;
	}

	public Set<Cookie> getCookies() {
		return cookies;
	}

	public boolean containsCookie(String name) {
		return cookies.stream().anyMatch(cookie -> Objects.equals(cookie.getName(), name));
	}

	public Cookie getCookie(String name) {
		final Optional<Cookie> optional = cookies
				.stream()
				.filter(cookie -> Objects.equals(cookie.getName(), name))
				.findFirst();
		return optional.orElse(null);
	}

	public String getHeader(String header) {
		return httpRequest.headers().get(header);
	}

	public String getPath() {
		return httpRequest.uri();
	}

	public FullHttpRequest getHttpRequest() {
		return httpRequest;
	}
}
