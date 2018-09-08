package org.di.annotations;

import java.lang.annotation.*;

/**
 * Indicates the context, which packages should be scanned to identify components (classes) for their injection
 * If no package is specified, the package of the class marked with this annotation will be scanned
 *
 * @author GenCloud
 * @date 04.09.2018
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScanPackage {
    /**
     * @return a string array of application packages, that will be scanned
     */
    String[] packages() default {};

    /**
     * @return an array of classes, whose packages should be scanned
     */
    Class<?>[] classes() default {};
}
