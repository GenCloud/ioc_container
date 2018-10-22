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
package org.ioc.web.security.configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class HttpContainer {
	private HttpAuthorizeRequest configuredRequests;
	private AuthenticationRequestConfigurer configuredAuthRequest;

	HttpAuthorizeRequest getConfiguredRequests() {
		if (configuredRequests == null) {
			configuredRequests = new HttpAuthorizeRequest(this);
		}

		return configuredRequests;
	}

	AuthenticationRequestConfigurer getConfiguredAuthRequest() {
		if (configuredAuthRequest == null) {
			configuredAuthRequest = new AuthenticationRequestConfigurer(this);
		}

		return configuredAuthRequest;
	}

	public HttpAuthorizeRequest configureRequests() {
		if (configuredRequests == null) {
			configuredRequests = new HttpAuthorizeRequest(this);
		}

		return configuredRequests;
	}

	public AuthenticationRequestConfigurer configureSession() {
		if (configuredAuthRequest == null) {
			configuredAuthRequest = new AuthenticationRequestConfigurer(this);
		}

		return configuredAuthRequest;
	}

	public static class AuthenticationRequestConfigurer {
		private final HttpContainer container;

		private String expiredPath;

		AuthenticationRequestConfigurer(HttpContainer container) {
			this.container = container;
		}

		public String getExpiredPath() {
			return expiredPath;
		}

		public AuthenticationRequestConfigurer expiredPath(String expiredPath) {
			this.expiredPath = expiredPath;
			return this;
		}

		public HttpContainer and() {
			return container;
		}
	}

	public static class HttpAuthorizeRequest {
		public static final String ROLE_ANONYMOUS = "ANONYMOUS";

		private final HttpContainer container;

		private Map<RequestSettings, List<String>> includeRolePermits = new ConcurrentHashMap<>();

		HttpAuthorizeRequest(HttpContainer container) {
			this.container = container;
		}

		Map<RequestSettings, List<String>> getIncludeRolePermits() {
			return Collections.unmodifiableMap(includeRolePermits);
		}

		boolean containsRole(String request, String roleName) {
			for (RequestSettings requestSettings : getIncludeRolePermits().keySet()) {
				if (Objects.equals(request, requestSettings.getUrlPattern())) {
					final List<String> roles = getIncludeRolePermits().get(requestSettings);
					return roles != null && roles.contains(roleName);
				}
			}

			return false;
		}

		boolean containsRoles(String request, List<String> roleNames) {
			return getIncludeRolePermits().keySet()
					.stream()
					.filter(requestSettings -> Objects.equals(request, requestSettings.getUrlPattern()))
					.map(requestSettings -> getIncludeRolePermits().get(requestSettings))
					.findFirst()
					.filter(roles -> roles.containsAll(roleNames))
					.isPresent();

		}

		public HttpAuthorizeRequest anonymousRequests(String... urlPatterns) {
			if (urlPatterns != null) {
				Arrays.stream(urlPatterns).forEach(s -> addAnonymousRequest(s, false));
			}

			return this;
		}

		public HttpAuthorizeRequest resourceRequests(String... urlPatterns) {
			if (urlPatterns != null) {
				Arrays.stream(urlPatterns).forEach(s -> addAnonymousRequest(s, true));
			}

			return this;
		}

		void addAnonymousRequest(String urlPattern, boolean isResource) {
			if (includeRolePermits.isEmpty()) {
				includeRolePermits.put(new RequestSettings(urlPattern, false, isResource),
						Collections.singletonList(ROLE_ANONYMOUS));
				return;
			}

			final Optional<RequestSettings> optional = includeRolePermits.keySet()
					.stream()
					.filter(r -> Objects.equals(r.getUrlPattern(), urlPattern))
					.findFirst();
			if (!optional.isPresent()) {
				includeRolePermits.put(new RequestSettings(urlPattern, false, isResource),
						Collections.singletonList(ROLE_ANONYMOUS));
			}
		}

		public HttpAuthorizeRequest authorizeRequests(String request, String... roles) {
			Arrays.stream(roles).forEach(r -> addRolePermit(request, r));
			return this;
		}

		void addRolePermit(String request, String roleName) {
			final Optional<RequestSettings> optional = includeRolePermits.keySet()
					.stream()
					.filter(r -> Objects.equals(r.getUrlPattern(), request))
					.findFirst();

			if (optional.isPresent()) {
				final RequestSettings requestSettings = optional.get();
				List<String> roles = includeRolePermits.get(requestSettings);
				if (roles == null) {
					roles = new ArrayList<>();
					roles.add(roleName);
					includeRolePermits.put(new RequestSettings(request, true, false), roles);
					return;
				}

				roles.add(roleName);
			} else {
				includeRolePermits.put(new RequestSettings(request, true, false),
						new ArrayList<>(Collections.singletonList(roleName)));
			}
		}

		public HttpContainer and() {
			return container;
		}
	}
}
