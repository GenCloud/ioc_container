package org.di.test.components;

import org.di.annotations.IoCComponent;
import org.di.annotations.IoCDependency;
import org.di.test.environments.SampleProperty;

/**
 * @author GenCloud
 * @date 05.09.2018
 */
@IoCComponent
public class ComponentC {
    private final SampleProperty sampleProperty;
    private final ComponentB componentB;
    private final ComponentA componentA;

    @IoCDependency
    public ComponentC(SampleProperty sampleProperty, ComponentB componentB, ComponentA componentA) {
        this.sampleProperty = sampleProperty;
        this.componentB = componentB;
        this.componentA = componentA;
    }

    @Override
    public String toString() {
        return "ComponentC{hash: " + Integer.toHexString(hashCode()) +
                ", value=" + sampleProperty.getValue() +
                ", componentB=" + componentB +
                ", componentA=" + componentA +
                '}';
    }
}
