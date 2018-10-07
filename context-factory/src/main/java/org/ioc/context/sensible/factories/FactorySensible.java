package org.ioc.context.sensible.factories;


import org.ioc.context.factories.Factory;
import org.ioc.context.sensible.Sensible;
import org.ioc.exceptions.IoCException;

/**
 * @author GenCloud
 * @date 09/2018
 */
public interface FactorySensible extends Sensible {
	void factoryInform(Factory factory) throws IoCException;
}
