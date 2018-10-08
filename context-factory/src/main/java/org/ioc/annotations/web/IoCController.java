package org.ioc.annotations.web;

import java.lang.annotation.*;

/**
 * Specifies that the class marked with the annotation must be initialized in the
 * contexts of the application, its automatic search when scanning application packages.
 *
 * @author GenCloud
 * @date 10/2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface IoCController {
	/**
	 * If the value is redefined, then the component (class) will be placed in
	 * the injection factories by its value, otherwise the original class name will be used.
	 *
	 * @return the name of the component (class), or the null value
	 */
	String value() default "";
}
