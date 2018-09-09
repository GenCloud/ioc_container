package org.di.test;

import org.di.annotations.Component;
import org.di.annotations.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 09.09.2018
 */
@Component
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Dependency
    private DefaultService service;
    @Dependency
    private DefaultComponent component;
    @Dependency
    private DefConstructorComponent defConstructorComponent;

    public void printInfo() {
        log.info(String.valueOf(service));
        log.info(String.valueOf(component));
        log.info(String.valueOf(defConstructorComponent));
    }
}
