package org.ioc.annotations.modules;

import org.ioc.enviroment.configurations.ThreadingAutoConfiguration;

import java.lang.annotation.*;

/**
 * Marker of enabled threading module in contexts.
 *
 * @author GenCloud
 * @date 09/2018
 * {@link org.ioc.threads}
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThreadingModule {
	/**
	 * @return enabled or disabled module
	 */
	boolean enabled() default true;

	Class<?> autoConfigurationClass() default ThreadingAutoConfiguration.class;
}
