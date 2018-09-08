package org.di.test;

import org.di.annotations.Component;
import org.di.annotations.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 05.09.2018
 */
@Component
public class DefConstructorComponent {
    private static final Logger log = LoggerFactory.getLogger(DefConstructorComponent.class);

    private final DefaultService defaultService;
    private final DefaultComponent defaultComponent;

    @Dependency
    public DefConstructorComponent(DefaultService defaultService, DefaultComponent defaultComponent) {
        this.defaultService = defaultService;
        this.defaultComponent = defaultComponent;
    }

    public void printInfo() {
        log.info("DefComponent - " + String.valueOf(defaultComponent));
        log.info("DefService - " + String.valueOf(defaultService));
    }
}
