package org.di.test.components;

import org.di.annotations.IoCComponent;
import org.di.annotations.IoCDependency;

/**
 * @author GenCloud
 * @date 05.09.2018
 */
@IoCComponent
public class ComponentC {
    private final Integer value;
    private final ComponentB componentB;
    private final ComponentA componentA;

    @IoCDependency
    public ComponentC(Integer value, ComponentB componentB, ComponentA componentA) {
        this.value = value;
        this.componentB = componentB;
        this.componentA = componentA;
    }

    @Override
    public String toString() {
        return "ComponentC{hash: " + Integer.toHexString(hashCode()) +
                ", value=" + value +
                ", componentB=" + componentB +
                ", componentA=" + componentA +
                '}';
    }
}
