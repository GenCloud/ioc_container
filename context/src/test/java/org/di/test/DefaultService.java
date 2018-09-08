package org.di.test;

import org.di.annotations.Component;
import org.di.annotations.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@Component
public class DefaultService {
    private static final Logger log = LoggerFactory.getLogger(DefaultService.class);

    @Dependency
    private DefaultComponent defaultComponent;

    public void printInfo() {
        log.info(String.valueOf(defaultComponent));
    }
}
