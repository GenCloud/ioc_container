package org.ioc.annotations.web;

import java.lang.annotation.*;

/**
 * @author GenCloud
 * @date 13.10.2018
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {
	String value();
}
