package org.di.threads.annotation;

import java.lang.annotation.*;

/**
 * @author GenCloud
 * @date 13.09.2018
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigureThread {
}
