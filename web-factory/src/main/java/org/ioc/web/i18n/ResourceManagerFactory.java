/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
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
package org.ioc.web.i18n;

import org.ioc.context.factories.Factory;
import org.ioc.context.factories.ICacheFactory;
import org.ioc.context.model.cache.ICache;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.sensible.factories.CacheFactorySensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.BundleAutoConfiguration;
import org.ioc.exceptions.IoCException;
import org.ioc.exceptions.ResourceMessageNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class ResourceManagerFactory implements Factory, CacheFactorySensible, ContextSensible, EnvironmentSensible<BundleAutoConfiguration> {
	private IoCContext context;

	private ICacheFactory cacheFactory;

	private ICache<String, ConcurrentMap<String, String>> resourceCache;

	private BundleAutoConfiguration bundleAutoConfiguration;

	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	@Override
	public void initialize() throws IoCException {
		resourceCache = cacheFactory.installEternal("bundle-cache", 10000);

		final String fileName = bundleAutoConfiguration.getFileName();
		for (String loc : bundleAutoConfiguration.getAllowedLocales()) {
			final Locale locale = Locale.forLanguageTag(loc);
			if (locale != null) {
				final ResourceBundle bundle = PropertyResourceBundle.getBundle(fileName, locale, new UTF8Control());
				final Enumeration<String> keys = bundle.getKeys();
				ConcurrentMap<String, String> map = resourceCache.get(locale.getLanguage());
				if (map == null) {
					map = new ConcurrentHashMap<>();
				}

				while (keys.hasMoreElements()) {
					final String key = keys.nextElement();
					final String value = bundle.getString(key);
					map.put(key, value);
				}

				resourceCache.put(locale.getLanguage(), map);
			}
		}
	}

	@Override
	public void factoryInform(Factory factory) throws IoCException {
		this.cacheFactory = (ICacheFactory) factory;
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
	public void environmentInform(BundleAutoConfiguration environment) throws IoCException {
		this.bundleAutoConfiguration = environment;
	}

	public String getMessage(String key, Locale locale) {
		final ConcurrentMap<String, String> map = resourceCache.get(locale.getLanguage());
		if (map != null) {
			final String msg = map.get(key);
			if (msg != null) {
				return msg;
			} else {
				throw new ResourceMessageNotFoundException("Cant find message with key - [" + key + "], locale - [" + locale + "]");
			}
		} else {
			throw new ResourceMessageNotFoundException("Cant find messages in locale - [" + locale + "]");
		}
	}

	private static class UTF8Control extends Control {
		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
				throws IOException {
			final String bundleName = toBundleName(baseName, locale);
			final String resourceName = toResourceName(bundleName, "properties");
			ResourceBundle bundle = null;
			InputStream stream = null;
			if (reload) {
				final URL url = loader.getResource(resourceName);
				if (url != null) {
					final URLConnection connection = url.openConnection();
					if (connection != null) {
						connection.setUseCaches(false);
						stream = connection.getInputStream();
					}
				}
			} else {
				stream = loader.getResourceAsStream(resourceName);
			}

			if (stream != null) {
				try {
					bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
				} finally {
					stream.close();
				}
			}
			return bundle;
		}
	}
}
