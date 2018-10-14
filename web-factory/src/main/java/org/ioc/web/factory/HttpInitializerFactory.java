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
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.internal.PlatformDependent;
import org.ioc.annotations.context.Mode;
import org.ioc.annotations.context.Order;
import org.ioc.annotations.web.MappingMethod;
import org.ioc.annotations.web.UrlMapping;
import org.ioc.cache.ICache;
import org.ioc.cache.ICacheFactory;
import org.ioc.context.factories.Factory;
import org.ioc.context.model.ControllerMetadata;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.sensible.factories.CacheFactorySensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.web.WebAutoConfiguration;
import org.ioc.exceptions.IoCException;
import org.ioc.web.HttpServerInspector;
import org.ioc.web.HttpServerMapper;
import org.ioc.web.HttpServerUtil;
import org.ioc.web.engine.PageManager;
import org.ioc.web.engine.VelocityManager;
import org.ioc.web.model.ModelMap;

import javax.net.ssl.SSLException;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Order(997)
public class HttpInitializerFactory implements Factory, ContextSensible, CacheFactorySensible,
		EnvironmentSensible<WebAutoConfiguration> {
	private IoCContext context;

	private WebAutoConfiguration webAutoConfiguration;

	private static final boolean isEpollType = !PlatformDependent.isWindows() && Epoll.isAvailable();
	private ICacheFactory cacheFactory;
	private ICache<MappingMethod, Map<String, HttpServerMapper>> mappingRequests;
	private ServerBootstrap bootstrap;

	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	@Override
	public void initialize() throws IoCException {
		mappingRequests = cacheFactory.installEternal("mapping-request-cache", 200);

		bootstrap = new ServerBootstrap();

		EventLoopGroup eventLoopGroup;
		Class<? extends ServerSocketChannel> serverSocketChannel;

		final int coreSize = Runtime.getRuntime().availableProcessors();
		if (isEpollType) {
			eventLoopGroup = new EpollEventLoopGroup(coreSize);
			serverSocketChannel = EpollServerSocketChannel.class;
		} else {
			eventLoopGroup = new NioEventLoopGroup(coreSize);
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

		bootstrap
				.option(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.group(eventLoopGroup)
				.channel(serverSocketChannel)
				.childHandler(new HttpChannelInitializer(sslContext, new VelocityManager(webAutoConfiguration)));
	}

	//	@PostConstruct for
	public void start() throws InterruptedException {
		final Collection<TypeMetadata> types = context.getMetadatas(Mode.REQUEST);
		instantiateToCache(types);

		bootstrap.bind(webAutoConfiguration.getPort()).sync().channel().closeFuture().sync();
	}

	private void instantiateToCache(Collection<TypeMetadata> types) {
		types.stream().filter(t -> t instanceof ControllerMetadata).forEach(this::resolveMetadata);
	}

	private void resolveMetadata(TypeMetadata metadata) {
		final ControllerMetadata controllerMetadata = (ControllerMetadata) metadata;
		final Class<?> type = controllerMetadata.getType();
		final String controllerPath = controllerMetadata.getMappingPath();

		Method[] methods = type.getDeclaredMethods();

		for (Method method : methods) {
			UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);

			if (urlMapping == null) {
				continue;
			}

			method.setAccessible(true);

			HttpServerMapper mapping = new HttpServerMapper(controllerMetadata.getInstance(), method);

			String firstPath = "";
			if (controllerPath != null && !controllerPath.isEmpty()) {
				firstPath = controllerPath;
			}

			String match = firstPath + urlMapping.value();

			Map<String, HttpServerMapper> map = mappingRequests.get(urlMapping.method());
			if (map == null) {
				map = new ConcurrentHashMap<>();
				map.put(match, mapping);
				mappingRequests.put(urlMapping.method(), map);
				continue;
			}

			map.put(match, mapping);
		}
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

	@Override
	public void factoryInform(Factory factory) throws IoCException {
		if (ICacheFactory.class.isAssignableFrom(factory.getClass())) {
			cacheFactory = (ICacheFactory) factory;
		}
	}

	class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
		private final SslContext sslContext;
		private final PageManager pageManager;

		HttpChannelInitializer(SslContext sslContext, PageManager pageManager) {
			this.sslContext = sslContext;
			this.pageManager = pageManager;
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
			pipeline.addLast(new HttpServerCodec())
					.addLast(new HttpObjectAggregator(Integer.MAX_VALUE))
					.addLast(new ChunkedWriteHandler())
					.addLast(new WebServerHandler(pageManager))
					.addLast(new ReadTimeoutHandler(15, MINUTES))
					.addLast(new StringEncoder(Charset.forName("UTF-8")))
					.addLast(new StringDecoder(Charset.forName("UTF-8")));

			if (sslContext != null) {
				pipeline.addLast(sslContext.newHandler(ch.alloc()));
			}
		}
	}

	class WebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

		private final PageManager pageManager;

		WebServerHandler(PageManager pageManager) {
			this.pageManager = pageManager;
		}

		@Override
		protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
			if (!request.decoderResult().isSuccess()) {
				HttpServerUtil.sendError(ctx, BAD_REQUEST);
				return;
			}

			final ModelMap attribute = new ModelMap();

			HttpServerInspector analysis = HttpServerInspector.inspectRequest(ctx, request, mappingRequests);
			if (analysis == null) {
				return;
			}

			final Object result = analysis.invokeControllerMethod(attribute);

			analysis.writeResponse(attribute, result, pageManager);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
		}
	}
}
