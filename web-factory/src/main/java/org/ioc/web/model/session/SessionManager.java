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
package org.ioc.web.model.session;

import io.netty.channel.Channel;
import org.ioc.enviroment.configurations.web.WebAutoConfiguration;
import org.ioc.threads.factory.DefaultThreadPoolFactory;
import org.ioc.threads.factory.model.interfaces.ScheduledTaskFuture;
import org.ioc.web.util.HttpServerUtil;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class SessionManager {
	private final SessionIdProducer SESSION_ID_PRODUCER = new SessionIdProducer();
	private final WebAutoConfiguration webAutoConfiguration;
	private final DefaultThreadPoolFactory threadPoolFactory;

	private ConcurrentMap<String, HttpSession> sessions = new ConcurrentHashMap<>();

	private ScheduledTaskFuture cleanTask;

	private String expiredUri = "/";

	public SessionManager(WebAutoConfiguration webAutoConfiguration, DefaultThreadPoolFactory threadPoolFactory) {
		this.webAutoConfiguration = webAutoConfiguration;
		this.threadPoolFactory = threadPoolFactory;

		startSessionCleaning();
	}

	public WebAutoConfiguration getWebAutoConfiguration() {
		return webAutoConfiguration;
	}

	private Map<String, HttpSession> getSessions() {
		return Collections.unmodifiableMap(sessions);
	}

	public HttpSession getSession(String sessionId) {
		return getSessions().get(sessionId);
	}

	public boolean containsSession(String sessionId) {
		return getSessions().containsKey(sessionId);
	}

	public void remove(String sessionId) {
		sessions.remove(sessionId);
	}

	public void setExpiredUri(String expiredUri) {
		this.expiredUri = expiredUri;
	}

	public String createSessionId() {
		return SESSION_ID_PRODUCER.generateSessionId();
	}

	public void addSession(String sessionId, Channel channel) {
		if (sessions.get(sessionId) == null) {
			final HttpSession httpSession = new HttpSession();
			httpSession.setSessionId(sessionId);
			httpSession.setMaxAge(webAutoConfiguration.getSessionTimeout());
			httpSession.setChannel(channel);
			sessions.putIfAbsent(sessionId, httpSession);
		}
	}

	private void startSessionCleaning() {
		if (cleanTask == null) {
			cleanTask = threadPoolFactory.async(1, TimeUnit.MINUTES, 1, () ->
					getSessions()
							.values()
							.stream()
							.filter(HttpSession::hasExpires)
							.filter(HttpSession::isAuthenticated)
							.forEach(this::expireSession));
		}
	}

	private void stopSessionCleaning() {
		if (cleanTask != null) {
			cleanTask.cancel(true);
			cleanTask = null;
		}
	}

	private void expireSession(HttpSession httpSession) {
		HttpServerUtil.sendRedirect(httpSession.getChannel(), expiredUri);
		remove(httpSession.getSessionId());
	}
}
