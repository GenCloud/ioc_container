package org.di.context.contexts.sensibles;

import org.di.context.excepton.IoCException;
import org.di.context.factories.config.Factory;

/**
 * @author GenCloud
 * @date 15.09.2018
 */
public interface FactorySensible extends Sensible {
    void factoryInform(Factory factory) throws IoCException;
}
