package org.di.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@Documented
@Target({TYPE, CONSTRUCTOR, FIELD, METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependency {
    String name() default "";
}
