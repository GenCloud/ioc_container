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
package org.ioc.enviroment.configurations.web;

import org.ioc.annotations.configuration.Property;
import org.ioc.annotations.configuration.PropertyFunction;
import org.ioc.context.factories.Factory;

import static org.ioc.context.factories.Factory.defaultWebFactory;
import static org.ioc.utils.ReflectionUtils.instantiateClass;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Property(prefix = "web.server.")
public class WebAutoConfiguration {
	private int port = 8080;

	@Property("ssl-enabled")
	private boolean sslEnabled = false;

	@Property("key-cert-file-path")
	private String keyCertChainFile;

	@Property("key-file-path")
	private String keyFile;

	@Property("key-password")
	private String keyPassword;

	@Property("velocity.input.encoding")
	private String velocityInputEncoding = "UTF-8";

	@Property("velocity.output.encoding")
	private String velocityOutputEncoding = "UTF-8";

	@Property("velocity.resource.loader")
	private String velocityResourceLoader = "file";

	@Property("velocity.resource.loader.class")
	private String velocityResourceLoaderClass = "org.apache.velocity.runtime.resource.loader.FileResourceLoader";

	@Property("velocity.resource.loading.path")
	private String velocityResourceLoadingPath = "./site";

	public int getPort() {
		return port;
	}

	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public String getKeyCertChainFile() {
		return keyCertChainFile;
	}

	public String getKeyFile() {
		return keyFile;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public String getVelocityInputEncoding() {
		return velocityInputEncoding;
	}

	public String getVelocityOutputEncoding() {
		return velocityOutputEncoding;
	}

	public String getVelocityResourceLoader() {
		return velocityResourceLoader;
	}

	public String getVelocityResourceLoaderClass() {
		return velocityResourceLoaderClass;
	}

	public String getVelocityResourceLoadingPath() {
		return velocityResourceLoadingPath;
	}

	@PropertyFunction
	public Object webFactory() {
		final Class<? extends Factory> factory = defaultWebFactory();
		return instantiateClass(factory);
	}
}
