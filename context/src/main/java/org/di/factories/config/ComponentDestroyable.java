package org.di.factories.config;

import org.di.excepton.starter.IoCStopException;

/**
 * Interface for invoke destroy method in component if it is present.
 *
 * @author GenCloud
 * @date 13.09.2018
 */
public interface ComponentDestroyable {
    void destroy() throws IoCStopException;
}
