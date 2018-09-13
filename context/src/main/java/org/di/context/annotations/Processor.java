package org.di.context.annotations;

import org.di.context.factories.config.ComponentProcessor;

import java.lang.annotation.*;

/**
 * Annotation for indicating enabled component processors.
 *
 * @author GenCloud
 * @date 13.09.2018
 * @see ComponentProcessor
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Processor {
    /**
     * Should be set up to true if you with to ignore component processor.
     */
    boolean ignore() default false;
}
