package org.di.context.annotations.modules;

import java.lang.annotation.*;

/**
 * Marker of enabled caching module in contexts.
 *
 * @author GenCloud
 * @date 16.09.2018
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheModule {
    /**
     * @return enabled or disabled module
     */
    boolean enabled() default true;
}
