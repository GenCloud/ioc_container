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
package org.ioc.web.factory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.ioc.web.model.handlers.DefaultRequestHandler;
import org.ioc.web.model.mapping.MappingContainer;
import org.ioc.web.model.session.SessionManager;
import org.ioc.web.model.view.TemplateResolver;
import org.ioc.web.security.configuration.SecurityConfigureAdapter;

import java.util.List;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
	private final SslContext sslContext;
	private final SessionManager sessionManager;
	private final SecurityConfigureAdapter securityConfigureAdapter;
	private final MappingContainer mappingContainer;
	private final List<Object> resolvers;
	private final TemplateResolver resolver;

	public HttpChannelInitializer(SslContext sslContext, SessionManager sessionManager,
								  SecurityConfigureAdapter securityConfigureAdapter, MappingContainer mappingContainer,
								  List<Object> resolvers, TemplateResolver resolver) {
		this.sslContext = sslContext;
		this.sessionManager = sessionManager;
		this.securityConfigureAdapter = securityConfigureAdapter;
		this.mappingContainer = mappingContainer;
		this.resolvers = resolvers;
		this.resolver = resolver;
	}

	/**
	 * This method will be called once the {@link Channel} was registered. After the method returns this instance
	 * will be removed from the {@link ChannelPipeline} of the {@link Channel}.
	 *
	 * @param ch the {@link Channel} which was registered.
	 */
	@Override
	protected void initChannel(SocketChannel ch) {
		final ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new HttpServerCodec(),
				new HttpServerExpectContinueHandler(),
				new HttpObjectAggregator(Integer.MAX_VALUE),
				new ChunkedWriteHandler(),
				new DefaultRequestHandler(mappingContainer, resolver, sessionManager, securityConfigureAdapter, resolvers));

		if (sslContext != null) {
			pipeline.addLast(sslContext.newHandler(ch.alloc()));
		}
	}
}
