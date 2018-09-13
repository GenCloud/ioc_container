package org.di.context.factories.config;

import org.di.context.excepton.starter.IoCStopException;

/**
 * Interface for invoke destroy method in component if it is present.
 *
 * @author GenCloud
 * @date 13.09.2018
 */
public interface ComponentDestroyable {
    void destroy() throws IoCStopException;
}
