package org.di.test.components;

import org.di.context.annotations.IoCComponent;
import org.di.context.annotations.IoCDependency;

/**
 * @author GenCloud
 * @date 09.09.2018
 */
@IoCComponent
public class ComponentD {
    @IoCDependency
    private ComponentB componentB;
    @IoCDependency
    private ComponentA componentA;
    @IoCDependency
    private ComponentC componentC;

    @Override
    public String toString() {
        return "ComponentD{hash: " + Integer.toHexString(hashCode()) + ", ComponentB=" + componentB +
                ", ComponentA=" + componentA +
                ", ComponentC=" + componentC +
                '}';
    }
}
