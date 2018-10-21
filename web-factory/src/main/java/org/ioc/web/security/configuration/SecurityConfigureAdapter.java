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

import org.ioc.annotations.context.Mode;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.type.IoCContext;
import org.ioc.utils.ReflectionUtils;
import org.ioc.web.exception.NoFoundAuthenticatedUserException;
import org.ioc.web.exception.SessionNotFoundException;
import org.ioc.web.exception.WrongMatchException;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.http.Request;
import org.ioc.web.model.http.Response;
import org.ioc.web.model.mapping.Mapping;
import org.ioc.web.model.session.HttpSession;
import org.ioc.web.model.session.SessionManager;
import org.ioc.web.security.AuthenticationProvider;
import org.ioc.web.security.CheckResult;
import org.ioc.web.security.configuration.HttpContainer.AuthenticationRequestConfigurer;
import org.ioc.web.security.configuration.HttpContainer.HttpAuthorizeRequest;
import org.ioc.web.security.encoder.Encoder;
import org.ioc.web.security.encoder.NullEncoder;
import org.ioc.web.security.filter.Filter;
import org.ioc.web.security.interceptors.HttpRequestInterceptor;
import org.ioc.web.security.user.UserDetails;
import org.ioc.web.security.user.UserDetailsProcessor;
import org.ioc.web.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.ioc.web.security.configuration.HttpContainer.HttpAuthorizeRequest.ROLE_ANONYMOUS;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SecurityConfigureAdapter {
	private static final Logger log = LoggerFactory.getLogger(SecurityConfigureAdapter.class);

	private final IoCContext context;
	private final HttpContainer container;
	private final SessionManager sessionManager;

	private final List<Filter> filters = new LinkedList<>();
	private final List<HttpRequestInterceptor> requestInterceptors = new LinkedList<>();
	private final PathUtil pathUtil = new PathUtil();
	private final AtomicReference<SecurityContext> securityContextReference = new AtomicReference<>();

	private boolean configuredUser;

	public SecurityConfigureAdapter(IoCContext context, SessionManager sessionManager) {
		this.context = context;
		this.sessionManager = sessionManager;

		container = new HttpContainer();

		final SecurityContext securityContext = new SecurityContext(sessionManager);
		securityContextReference.set(securityContext);

		configure();

		if (!configuredUser) {
			addDefaultRequestSecurity();
		}
	}

	private void addDefaultRequestSecurity() {
		container
				.configureRequests()
				.anonymousRequests("/**")
				.resourceRequests("/**");
	}

	private void configure() {
		final Collection<TypeMetadata> metadatas = context.getMetadatas(Mode.SINGLETON);
		metadatas.forEach(metadata -> {
			final Class<?> type = metadata.getType();
			final Object instance = metadata.getInstance();
			if (HttpRequestInterceptor.class.isAssignableFrom(type)) {
				requestInterceptors.add((HttpRequestInterceptor) instance);
			}

			if (Filter.class.isAssignableFrom(type)) {
				filters.add((Filter) instance);
			}

			if (UserDetailsProcessor.class.isAssignableFrom(type)) {
				final List<UserDetailsProcessor> userDetailsProcessors = getContext().getUserDetailsProcessors();
				if (userDetailsProcessors.isEmpty()) {
					userDetailsProcessors.add((UserDetailsProcessor) instance);
				}
			}

			if (SecurityConfigureProcessor.class.isAssignableFrom(type)) {
				((SecurityConfigureProcessor) instance).configure(container);
				configuredUser = true;
			}
		});

		final AuthenticationRequestConfigurer configurer = container.getConfiguredAuthRequest();
		final String expiredPath = configurer.getExpiredPath();
		if (expiredPath != null && !expiredPath.isEmpty()) {
			sessionManager.setExpiredUri(expiredPath);
		}

		postConfigureAuthManager();
	}

	private void postConfigureAuthManager() {
		final AuthenticationRequestConfigurer configuredAuthRequest = container.getConfiguredAuthRequest();
		if (configuredAuthRequest != null) {
			final String authPage = configuredAuthRequest.getAuthRequest();
			if (authPage != null && !authPage.isEmpty()) {
				final Collection<TypeMetadata> metadatas = context.getMetadatas(Mode.SINGLETON);
				final Optional<Object> optional = metadatas
						.stream()
						.filter(m -> Encoder.class.isAssignableFrom(m.getType()))
						.map(TypeMetadata::getInstance)
						.findFirst();

				final AuthenticationProvider authenticationRoutes = new AuthenticationProvider();
				authenticationRoutes.setSecurityConfigureAdapter(this);
				authenticationRoutes.setPath(authPage);
				if (optional.isPresent()) {
					authenticationRoutes.setEncoder((Encoder) optional.get());
				} else {
					authenticationRoutes.setEncoder(new NullEncoder());
				}

				final String redirectPath = configuredAuthRequest.getRedirectPath();
				if (redirectPath != null && !redirectPath.isEmpty()) {
					authenticationRoutes.setRedirectPath(redirectPath);
				}

				final TypeMetadata metadata = new TypeMetadata(ReflectionUtils.resolveTypeName(authenticationRoutes.getClass()),
						authenticationRoutes, Mode.REQUEST);
				context.registerTypes(Collections.singletonList(metadata));
			}
		}
	}

	public CheckResult secureRequest(Request request, Response response, ModelAndView modelAndView, Mapping mapping) {
		final CheckResult result = checkRequest(request, mapping);

		if (!result.isOk()) {
			return result;
		}

		if (log.isDebugEnabled()) {
			log.debug("Start filtering request - [{}]", request.getPath());
		}

		final boolean ok = filters
				.stream()
				.allMatch(filter -> filter.doFilter(request, response)) && requestInterceptors
				.stream()
				.allMatch(interceptor -> interceptor.preHandle(request, response, modelAndView, mapping));
		result.setOk(ok);
		return result;
	}

	public SecurityContext getContext() {
		return securityContextReference.get();
	}

	public HttpContainer getContainer() {
		return container;
	}

	private CheckResult checkRequest(Request request, Mapping mapping) {
		final String url = request.getPath();
		final HttpAuthorizeRequest authorizeRequest = container.getConfiguredRequests();
		final Map<RequestSettings, List<String>> rolePermits = authorizeRequest.getIncludeRolePermits();
		CheckResult result;
		for (RequestSettings settings : rolePermits.keySet()) {
			final String pattern = settings.getUrlPattern();
			if (pathUtil.match(pattern, url) || pathUtil.match(pattern + "?**", url)) {
				if (!settings.isResource() && mapping != null) {
					final HttpSession session = getContext().findSession(request);

					if (session != null) {
						final UserDetails user = session.getUserDetails();
						if (!session.isAuthenticated()) {
							if (authorizeRequest.containsRole(pattern, ROLE_ANONYMOUS)) {
								return new CheckResult(true);
							}

							result = new CheckResult(false);
							log.warn("Can't authorize request - no authenticated user in session!");
							result.setThrowable(new NoFoundAuthenticatedUserException("Can't authorize request - no authenticated user in session!"));
							return result;
						}

						if (settings.isAuthenticated() && session.isAuthenticated()) {
							if (authorizeRequest.containsRoles(pattern, user.getRoles())) {
								return new CheckResult(true);
							}
						} else if (session.isAuthenticated()) {
							if (authorizeRequest.containsRole(pattern, ROLE_ANONYMOUS)) {
								return new CheckResult(true);
							}
						} else {
							result = new CheckResult(false);
							log.warn("Can't authorize request - wrong match!");
							result.setThrowable(new WrongMatchException("Can't authorize request - wrong match!"));
							return result;
						}
					} else {
						result = new CheckResult(false);
						log.warn("Can't authorize request - wrong session!");
						result.setThrowable(new SessionNotFoundException("Can't authorize request - wrong session!"));
						return result;

					}
				} else {
					return new CheckResult(true, true);
				}
			}
		}

		result = new CheckResult(false);
		log.warn("Can't authorize request - wrong match!");
		result.setThrowable(new WrongMatchException("Can't authorize request - wrong match!"));
		return result;
	}
}
