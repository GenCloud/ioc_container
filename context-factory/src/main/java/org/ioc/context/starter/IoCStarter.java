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
package org.ioc.context.starter;

import org.ioc.annotations.context.ScanPackage;
import org.ioc.context.DefaultIoCContext;
import org.ioc.context.listeners.facts.OnContextIsInitializedFact;
import org.ioc.context.type.IoCContext;
import org.ioc.utils.BannerUtils;
import org.ioc.utils.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classes that can be used to bootstrap and launch a application from a main method.
 * <p>
 * In most circumstances the static {@link #start(Class, String...)} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 *
 * <pre class="code">
 * public class App {
 *     public static void main(String... args) throws Exception {
 *         IoCStarter.run(App.class, args);
 *     }
 * }
 * </pre>
 *
 * @author GenCloud
 * @date 09/2018
 * @see #start(Class, String...)
 * @see #start(Class[], String...)
 */
public class IoCStarter {
	private static final Logger log = LoggerFactory.getLogger(IoCStarter.class);

	private DefaultIoCContext ioCContext;

	/**
	 * Static helper that can be used to run a {@link IoCStarter} from the
	 * specified source using default settings.
	 *
	 * @param mainClass the class to load
	 * @param args      application resolvers
	 * @return running {@link IoCContext}
	 */
	public static DefaultIoCContext start(Class<?> mainClass, String... args) {
		return new IoCStarter().start(new Class[]{mainClass}, args);
	}

	/**
	 * Run the application, creating {@link IoCContext}.
	 *
	 * @param mainClasses the class to load
	 * @param args        application resolvers
	 * @return running {@link IoCContext}
	 */
	private DefaultIoCContext start(Class<?>[] mainClasses, String... args) {
		final StopWatch stopWatch = new StopWatch();
		try {
			BannerUtils.printBanner(System.out);
			stopWatch.start();
			log.info("Start initialization of contexts app");
			ioCContext = initializeContext(mainClasses, args);

		} catch (Exception e) {
			final String msg = e.getMessage();
			log.error("Incorrect start: {}", msg, e);
			System.exit(0);
		}

		stopWatch.stop();

		log.info("App contexts started in [{}] seconds", stopWatch);
		return ioCContext;
	}

	/**
	 * Load components into contexts.
	 *
	 * @param mainClasses the classes to inspect
	 * @param args        block startup resolvers
	 * @return running {@link IoCContext}
	 */
	private DefaultIoCContext initializeContext(Class<?>[] mainClasses, String... args) {
		final DefaultIoCContext context = new DefaultIoCContext();
		for (Class<?> mainSource : mainClasses) {
			final ScanPackage scanPackage = mainSource.getAnnotation(ScanPackage.class);
			if (scanPackage != null) {
				final String[] packages = scanPackage.packages();
				context.init(mainSource, packages);
			}
		}

		Runtime.getRuntime().addShutdownHook(new ShutdownHook(context));

		context.getDispatcherFactory().fireEvent(new OnContextIsInitializedFact(context));

		return context;
	}

	private class ShutdownHook extends Thread {
		private final IoCContext context;

		ShutdownHook(IoCContext context) {
			this.context = context;
		}

		@Override
		public void run() {
			log.info("Start shutdown application");
			context.destroy();
		}
	}
}
