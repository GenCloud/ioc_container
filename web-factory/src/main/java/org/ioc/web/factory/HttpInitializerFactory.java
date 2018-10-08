/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of DI (IoC) Container Project.
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
package org.ioc.web.factory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.ioc.context.factories.Factory;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.web.WebAutoConfiguration;
import org.ioc.exceptions.IoCException;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class HttpInitializerFactory implements Factory, ContextSensible, EnvironmentSensible<WebAutoConfiguration> {
	private IoCContext context;

	private WebAutoConfiguration webAutoConfiguration;

	private ServerBootstrap serverBootstrap;

	private AtomicReference<HttpServerStatus> serverStateRef;

	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	@Override
	public void initialize() throws IoCException {
		serverBootstrap = new ServerBootstrap();

		EventLoopGroup eventLoopGroup;
		Class<? extends ServerSocketChannel> serverSocketChannel;
		if (IS_OS_UNIX) {
			eventLoopGroup = new EpollEventLoopGroup(1);
			serverSocketChannel = EpollServerSocketChannel.class;
		} else {
			eventLoopGroup = new NioEventLoopGroup(1);
			serverSocketChannel = NioServerSocketChannel.class;
		}

		SslContext sslContext = null;
		if (webAutoConfiguration.isSslEnabled()) {
			final File keyCertChainFile = Paths.get(webAutoConfiguration.getKeyCertChainFile()).toFile();
			final File keyFile = Paths.get(webAutoConfiguration.getKeyFile()).toFile();
			final String keyPassword = webAutoConfiguration.getKeyPassword();

			try {
				sslContext = SslContextBuilder.forServer(keyCertChainFile, keyFile, keyPassword).build();
			} catch (SSLException e) {
				e.printStackTrace();
			}
		}


		serverBootstrap
				.option(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.group(eventLoopGroup)
				.channel(serverSocketChannel);
		//todo: initializer
	}

	/**
	 * Set the {@link IoCContext} to component.
	 *
	 * @param context initialized application contexts
	 * @throws IoCException throw if contexts throwing by methods
	 */
	@Override
	public void contextInform(IoCContext context) throws IoCException {
		this.context = context;
	}

	@Override
	public void environmentInform(WebAutoConfiguration webAutoConfiguration) throws IoCException {

		this.webAutoConfiguration = webAutoConfiguration;
	}
}
