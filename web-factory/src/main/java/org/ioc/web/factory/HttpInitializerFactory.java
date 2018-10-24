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
package org.ioc.web.factory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.internal.PlatformDependent;
import org.ioc.annotations.context.Mode;
import org.ioc.annotations.context.Order;
import org.ioc.annotations.context.PostConstruct;
import org.ioc.context.factories.Factory;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.web.WebAutoConfiguration;
import org.ioc.exceptions.IoCException;
import org.ioc.web.annotations.UrlMapping;
import org.ioc.web.model.mapping.Mapping;
import org.ioc.web.model.mapping.MappingContainer;
import org.ioc.web.model.resolvers.ArgumentResolver;
import org.ioc.web.model.session.SessionManager;
import org.ioc.web.model.view.TemplateResolver;
import org.ioc.web.model.view.VelocityResolver;
import org.ioc.web.security.configuration.SecurityConfigureAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.ioc.utils.ReflectionUtils.resolveTypeName;
import static org.ioc.web.util.HttpServerUtil.toHttpMethod;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Order(997)
public class HttpInitializerFactory implements Factory, ContextSensible, EnvironmentSensible<WebAutoConfiguration> {
	private static final Logger log = LoggerFactory.getLogger(HttpInitializerFactory.class);

	private static final boolean isEpollType = !PlatformDependent.isWindows() && Epoll.isAvailable();

	private final MappingContainer mappingContainer = new MappingContainer();
	private SessionManager sessionManager;

	private IoCContext context;

	private WebAutoConfiguration webAutoConfiguration;

	private ServerBootstrap bootstrap;
	private EventLoopGroup eventLoopGroup;
	private Class<? extends ServerSocketChannel> serverSocketChannel;
	private SslContext sslContext;

	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	@Override
	public void initialize() throws IoCException {
		bootstrap = new ServerBootstrap();

		sessionManager = new SessionManager(webAutoConfiguration);

		final int coreSize = Runtime.getRuntime().availableProcessors();
		if (isEpollType) {
			eventLoopGroup = new EpollEventLoopGroup(coreSize);
			serverSocketChannel = EpollServerSocketChannel.class;
		} else {
			eventLoopGroup = new NioEventLoopGroup(coreSize);
			serverSocketChannel = NioServerSocketChannel.class;
		}

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
	}

	@SuppressWarnings("unchecked")
	@PostConstruct
	public void start() throws InterruptedException {
		final Collection<TypeMetadata> types = context.getMetadatas(Mode.REQUEST);

		final Collection<TypeMetadata> values = context.getSingletonFactory().getTypes().values();

		final List<Object> findedResolvers = values.stream()
				.filter(t -> ArgumentResolver.class.isAssignableFrom(t.getType()))
				.map(TypeMetadata::getInstance)
				.collect(Collectors.toList());

		final SecurityConfigureAdapter securityConfigureAdapter = new SecurityConfigureAdapter(context, sessionManager);
		final TemplateResolver templateResolver = new VelocityResolver(webAutoConfiguration);

		context.setType(resolveTypeName(templateResolver.getClass()), templateResolver);
		context.setType(resolveTypeName(sessionManager.getClass()), sessionManager);
		context.setType(resolveTypeName(securityConfigureAdapter.getClass()), securityConfigureAdapter);

		registerMapping(types);

		bootstrap
				.group(eventLoopGroup)
				.channel(serverSocketChannel)
				.childHandler(new HttpChannelInitializer(sslContext, sessionManager, securityConfigureAdapter, mappingContainer, findedResolvers, templateResolver));

		log.info("Http server started on port(s): {} (http)", webAutoConfiguration.getPort());
		bootstrap.bind(webAutoConfiguration.getPort()).sync().channel().closeFuture().sync();
	}

	private String normalizePath(Class<?> controller, Method methodController) {
		String finalPath = "";
		final UrlMapping controllerMapping = controller.getAnnotation(UrlMapping.class);
		final UrlMapping methodMapping = methodController.getAnnotation(UrlMapping.class);

		if (controllerMapping != null) {
			finalPath = finalPath.concat(controllerMapping.value());
		}

		if (methodMapping != null && !methodMapping.value().isEmpty()) {
			String value = methodMapping.value();
			if (value.startsWith("/")) {
				value = value.replaceFirst("/", "");
			}

			if (!finalPath.endsWith("/")) {
				finalPath = finalPath.concat("/");
			}

			finalPath = finalPath.concat(value);
		}

		return finalPath;
	}

	private void registerMapping(Collection<TypeMetadata> collection) {
		for (TypeMetadata metadata : collection) {
			for (Method method : metadata.getType().getDeclaredMethods()) {
				method.setAccessible(true);
				final UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);
				if (urlMapping != null) {
					final String path = normalizePath(metadata.getType(), method);

					final Mapping mapping = new Mapping(metadata.getInstance(), method,
							toHttpMethod(urlMapping.method()), path);

					mapping.setConsumes(urlMapping.consumes());
					mapping.setProduces(urlMapping.produces());

					if (String.class.isAssignableFrom(method.getReturnType())) {
						mapping.setIsView();
					}

					mapping.setMetadata(metadata);
					mapping.setParameters(new Object[method.getParameterCount()]);
					mappingContainer.addMapping(context, path, method, mapping);
				}
			}
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
}
