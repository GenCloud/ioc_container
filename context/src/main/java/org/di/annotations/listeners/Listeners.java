package org.di.annotations.listeners;

import org.di.listeners.Listener;

import java.lang.annotation.*;

/**
 * <p>Indicates to the context which packages should be scanned to identify listeners
 * and initializeComponents them</p>
 * <p>If no package is specified, the package of the class marked with this annotation will be scanned</p>
 *
 * @author GenCloud
 * @date 04.09.2018
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Listeners {
    /**
     * @return a string array of application packages, that will be scanned
     */
    String[] packages() default {};

    /**
     * @return an array of class-listeners
     */
    Class<? extends Listener>[] classes() default {};
}
