package org.di.context.contexts.sensibles;

import org.di.context.excepton.IoCException;

/**
 * @author GenCloud
 * @date 14.09.2018
 */
public interface ThreadFactorySensible extends Sensible {
    void threadFactoryInform(Object factory) throws IoCException;
}
