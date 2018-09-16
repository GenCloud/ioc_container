package org.di.context.annotations;

import org.di.context.factories.config.Factory;

import java.lang.annotation.*;

/**
 * @author GenCloud
 * @date 16.09.2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Factories {
    Class<? extends Factory>[] enabled();
}
