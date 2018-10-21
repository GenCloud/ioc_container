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

import io.netty.handler.codec.http.HttpResponseStatus;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.session.HttpSession;
import org.ioc.web.model.session.SessionManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class Response {
	private Object body;

	private HttpResponseStatus responseStatus = HttpResponseStatus.NOT_FOUND;

	private Map<String, String> headers = new HashMap<>();

	private Set<Cookie> cookies = new HashSet<>();

	private String viewPage;

	private ModelAndView model;

	private Response(Object body, HttpResponseStatus responseStatus, Map<String, String> headers, Set<Cookie> cookies) {
		this.body = body;
		this.responseStatus = responseStatus;
		this.headers = headers;
		this.cookies = cookies;
	}

	private Response(Object body, HttpResponseStatus responseStatus, Map<String, String> headers, Set<Cookie> cookies, String viewPage) {
		this.body = body;
		this.responseStatus = responseStatus;
		this.headers = headers;
		this.cookies = cookies;
		this.viewPage = viewPage;
	}

	public Response(SessionManager sessionManager, Request request) {
		if (request.getCookies() != null) {
			Cookie cookie = request.getCookie("SESSIONID");
			HttpSession session;
			if (cookie == null || !sessionManager.containsSession(cookie.getValue())) {
				cookie = new Cookie("SESSIONID", sessionManager.createSessionId(), sessionManager.getWebAutoConfiguration().getSessionTimeout());
				sessionManager.addSession(cookie.getValue(), request.getChannel());
				addCookie(cookie);
			} else {
				session = sessionManager.getSession(cookie.getValue());
				if (session.hasExpires()) {
					cookie = new Cookie("SESSIONID", sessionManager.createSessionId(), sessionManager.getWebAutoConfiguration().getSessionTimeout());
					sessionManager.addSession(cookie.getValue(), request.getChannel());
					addCookie(cookie);
				}
			}
		}
	}

	public static Builder success() {
		return status(HttpResponseStatus.OK);
	}

	public static Builder badRequest() {
		return status(HttpResponseStatus.BAD_REQUEST);
	}

	private static Builder status(HttpResponseStatus status) {
		return new Builder(status);
	}

	public ModelAndView getModel() {
		return model;
	}

	public void setModel(ModelAndView model) {
		this.model = model;
	}

	public String getViewPage() {
		return viewPage;
	}

	public void setViewPage(String view) {
		this.viewPage = view;
	}

	public long getFileLength() {
		return viewPage == null ? 0 : viewPage.length();
	}

	public void addCookie(Cookie cookie) {
		cookies.add(cookie);
	}

	public Set<Cookie> getCookies() {
		return cookies;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void addHeader(String header, String value) {
		headers.putIfAbsent(header, value);
	}

	public Object getBody() {
		return body;
	}

	public void setBody(Object body) {
		this.body = body;
	}

	public HttpResponseStatus getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(HttpResponseStatus responseStatus) {
		this.responseStatus = responseStatus;
	}

	public static class Builder {
		private final Map<String, String> headers = new HashMap<>();
		private final Set<Cookie> cookies = new HashSet<>();
		private HttpResponseStatus status;
		private Object body;
		private String view;

		Builder(HttpResponseStatus status) {
			this.status = status;
		}

		public Builder body(Object o) {
			this.body = o;
			return this;
		}

		public Response build() {
			final Map<String, String> map = new HashMap<>();
			headers.keySet().forEach(s -> map.putIfAbsent(s, headers.get(s)));

			if (view == null) {
				return new Response(body, status, map, cookies);
			} else {
				return new Response(body, status, map, cookies, view);
			}
		}

		public Builder addHeader(String header, String value) {
			headers.putIfAbsent(header, value);
			return this;
		}

		public Builder addCookie(String name, String value, long maxAge) {
			cookies.add(new Cookie(name, value, maxAge));
			return this;
		}

		public Builder addView(String view) {
			this.view = view;
			return this;
		}
	}
}
