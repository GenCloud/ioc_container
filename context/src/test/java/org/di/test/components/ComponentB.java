package org.di.test.components;

import org.di.annotations.IoCComponent;
import org.di.annotations.IoCDependency;
import org.di.test.environments.ExampleEnvironment;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@IoCComponent
public class ComponentB {
    @IoCDependency
    private ComponentA componentA;

    @IoCDependency
    private ExampleEnvironment exampleEnvironment;

    @Override
    public String toString() {
        return "ComponentB{hash: " + Integer.toHexString(hashCode()) + ", componentA=" + componentA +
                ", exampleEnvironment=" + exampleEnvironment +
                '}';
    }
}
