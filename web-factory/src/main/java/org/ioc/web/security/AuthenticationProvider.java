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
package org.ioc.web.security;

import org.ioc.web.annotations.RequestParam;
import org.ioc.web.model.http.Request;
import org.ioc.web.model.http.Response;
import org.ioc.web.model.session.HttpSession;
import org.ioc.web.security.configuration.SecurityConfigureAdapter;
import org.ioc.web.security.encoder.Encoder;
import org.ioc.web.security.user.UserDetails;

import java.util.Collections;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class AuthenticationProvider {
	private SecurityConfigureAdapter securityConfigureAdapter;

	private String path;
	private String redirectPath;

	private Encoder encoder;

	public void setSecurityConfigureAdapter(SecurityConfigureAdapter securityConfigureAdapter) {
		this.securityConfigureAdapter = securityConfigureAdapter;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setRedirectPath(String redirectPath) {
		this.redirectPath = redirectPath;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	public Response auth(Request request, @RequestParam("username") String username, @RequestParam("password") String password) {
		final UserDetails userDetails = securityConfigureAdapter.getContext().authenticate(username, password, encoder);
		if (userDetails == null) {
			return Response.badRequest().body(Collections.singletonMap("message", "User with this name|password not found!")).build();
		} else {
			final HttpSession session = securityConfigureAdapter.getContext().findSession(request);
			if (session != null) {
				session.setUserDetails(userDetails);
				session.setAuthenticated(true);
				if (redirectPath != null && !redirectPath.isEmpty()) {
					return Response.success().addHeader("location", redirectPath).build();
				}
				return Response.success().build();
			}

			return Response.badRequest().body(Collections.singletonMap("message", "Session not found!")).build();
		}
	}
}
