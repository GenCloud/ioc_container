package org.ioc.annotations.modules;

import org.ioc.enviroment.configurations.web.WebAutoConfiguration;

import java.lang.annotation.*;

/**
 * Marker of enabled web server in contexts.
 *
 * @author GenCloud
 * @date 10/2018
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebModule {
	/**
	 * @return enabled or disabled module
	 */
	boolean enabled() default true;

	Class<?> autoConfigurationClass() default WebAutoConfiguration.class;
}