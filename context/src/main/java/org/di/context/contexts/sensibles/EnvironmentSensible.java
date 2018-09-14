package org.di.context.contexts.sensibles;

import org.di.context.excepton.IoCException;

/**
 * @author GenCloud
 * @date 14.09.2018
 */
public interface EnvironmentSensible<E> extends Sensible {
    void environmentInform(E environment) throws IoCException;
}
