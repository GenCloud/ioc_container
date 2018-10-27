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

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class RequestEntry {
	private FullHttpRequest httpRequest;

	private HttpMethod method;

	private Channel channel;

	private Set<Cookie> cookies = new TreeSet<>();

	public RequestEntry(Channel channel, FullHttpRequest httpRequest) {
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

	public String getParameter(String paramName) {
		final String uri = httpRequest.uri();
		final QueryStringDecoder decoder = new QueryStringDecoder(uri);
		final Map<String, List<String>> map = decoder.parameters();

		final List<String> list = map.get(paramName);
		if (list == null || list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
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
		return getHeaders().get(header);
	}

	public HttpHeaders getHeaders() {
		return httpRequest.headers();
	}

	public String getPath() {
		return httpRequest.uri();
	}

	public FullHttpRequest getHttpRequest() {
		return httpRequest;
	}
}
