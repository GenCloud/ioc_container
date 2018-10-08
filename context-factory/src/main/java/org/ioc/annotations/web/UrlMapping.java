package org.ioc.annotations.web;

import java.lang.annotation.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
@Documented
public @interface UrlMapping {
	String value();

	MappingMethod method() default MappingMethod.GET;
}
