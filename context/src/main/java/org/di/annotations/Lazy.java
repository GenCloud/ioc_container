package org.di.annotations;

import java.lang.annotation.*;

/**
 * @author GenCloud
 * @date 09.09.2018
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lazy {
}
