package org.di.annotations;

import java.lang.annotation.*;

/**
 * Specifies that the class marked with the annotation must be initialized in the
 * context of the application, its automatic search when scanning application packages.
 *
 * @author GenCloud
 * @date 04.09.2018
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    /**
     * If the value is redefined, then the component (class) will be placed in
     * the injection factory by its value, otherwise the original class name will be used.
     *
     * @return the name of the component (class), or the null value
     */
    String name() default "";
}
