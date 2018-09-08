package org.di.annotations;

import java.lang.annotation.*;

/**
 * Indicates the option to load the component (class) {@link Opt}
 *
 * @author GenCloud
 * @date 04.09.2018
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoadOpt {
    Opt value() default Opt.SINGLETON;

    /**
     * Identifier of access to the component (class)
     */
    enum Opt {
        /**
         * single access
         */
        SINGLETON,
        /**
         * multiply access
         */
        PROTOTYPE
    }
}
