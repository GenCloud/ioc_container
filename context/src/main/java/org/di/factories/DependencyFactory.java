package org.di.factories;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple template class for implementations that creates a singleton or
 * a prototype object {@link org.di.annotations.LoadOpt.Opt}, depending on a flag.
 * <p>
 * If the "singleton" flag is true (the default), this class will create
 * the object that it creates exactly once on initialization and subsequently
 * return said singleton instance on all calls to the method.
 *
 * @author GenCloud
 * @date 04.09.2018
 */
public class DependencyFactory {
    private Map<String, Object> singletons = new HashMap<>();

    public Object getSingleton(String className) {
        return singletons.get(className);
    }

    public void addSingleton(String className, Object object) {
        singletons.put(className, object);
    }
}
