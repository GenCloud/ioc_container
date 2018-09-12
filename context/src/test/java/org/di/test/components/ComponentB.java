package org.di.test.components;

import org.di.annotations.IoCComponent;
import org.di.annotations.IoCDependency;
import org.di.test.environments.ExampleEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@IoCComponent
public class ComponentB {
    private static final Logger log = LoggerFactory.getLogger(ComponentB.class);

    @IoCDependency
    private ComponentA componentA;

    @IoCDependency
    private ExampleEnvironment exampleEnvironment;

    @PostConstruct
    public void init() {
        log.info("Testing invoke annotated method with PostConstruct");
    }

    @Override
    public String toString() {
        return "ComponentB{hash: " + Integer.toHexString(hashCode()) + ", componentA=" + componentA +
                ", exampleEnvironment=" + exampleEnvironment +
                '}';
    }
}
