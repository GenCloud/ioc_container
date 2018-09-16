package org.di.cache.annotations;

import java.lang.annotation.*;

/**
 * Indicate to proxy that this method should not be cached.
 *
 * @author GenCloud
 * @date 16.09.2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface CacheIgnore {
}
