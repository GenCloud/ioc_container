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

import org.ioc.web.exception.EncodeException;
import org.ioc.web.model.http.Cookie;
import org.ioc.web.model.http.Request;
import org.ioc.web.model.session.HttpSession;
import org.ioc.web.model.session.SessionManager;
import org.ioc.web.security.encoder.Encoder;
import org.ioc.web.security.user.UserDetails;
import org.ioc.web.security.user.UserDetailsProcessor;

import java.util.LinkedList;
import java.util.List;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SecurityContext {
	private final List<UserDetailsProcessor> userDetailsProcessors = new LinkedList<>();
	private final SessionManager sessionManager;

	SecurityContext(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	List<UserDetailsProcessor> getUserDetailsProcessors() {
		return userDetailsProcessors;
	}

	public UserDetails authenticate(String username, String password, Encoder encoder) {
		UserDetails currentUserDetails;
		for (UserDetailsProcessor roleDetails : userDetailsProcessors) {
			currentUserDetails = roleDetails.loadUser(username);
			if (currentUserDetails != null) {
				if (!encoder.match(password, currentUserDetails.getPassword())) {
					throw new EncodeException("Password's does not match! Check password encryption.");
				}

				return currentUserDetails;
			}
		}

		return null;
	}

	public UserDetails findCredentials(Request request) {
		final HttpSession session = findSession(request);
		if (session != null) {
			return session.getUserDetails();
		}

		return null;
	}

	public HttpSession findSession(Request request) {
		if (request.getCookies() != null) {
			final Cookie sessionId = request.getCookie("SESSIONID");

			if (sessionId == null) {
				return null;
			}

			return sessionManager.getSession(sessionId.getValue());
		}

		return null;
	}
}
