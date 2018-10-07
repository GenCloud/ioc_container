package org.ioc.annotations.configuration;

import org.ioc.annotations.context.Mode;

import java.lang.annotation.*;

/**
 * @author GenCloud
 * @date 09/2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface PropertyFunction {
	String value() default "";

	Mode scope() default Mode.SINGLETON;
}
