package org.di.context.annotations.modules;

import java.lang.annotation.*;

/**
 * Marker of enabled threading module in contexts.
 *
 * @author GenCloud
 * @date 13.09.2018
 * {@link org.di.threads}
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ThreadingModule {
    /**
     * @return enabled or disabled module
     */
    boolean enabled() default true;
}
