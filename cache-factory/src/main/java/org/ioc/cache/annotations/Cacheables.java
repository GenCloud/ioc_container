package org.ioc.cache.annotations;

import java.lang.annotation.*;

/**
 * Flags an interface that is capable of caching.
 * Note: only interfaces can be cached!
 *
 * @author GenCloud
 * @date 09/2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Cacheables {
}
