package org.di.test;

import org.apache.log4j.BasicConfigurator;
import org.di.annotations.ScanPackage;
import org.di.context.AppContext;
import org.di.context.runner.DIStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@ScanPackage(packages = {"org.di.test", "org.di"})
public class AppMain {
    private static final Logger log = LoggerFactory.getLogger(AppMain.class);

    public static void main(String... args) {
        BasicConfigurator.configure();

        final AppContext context = DIStarter.start(AppMain.class, args);
        final DefaultService defaultService = (DefaultService) context.getSingleton(DefaultService.class);
        log.info(defaultService.toString());

        defaultService.printInfo();

        final DefConstructorComponent defConstructorComponent = (DefConstructorComponent) context.getSingleton(DefConstructorComponent.class);
        log.info(defConstructorComponent.toString());

        defConstructorComponent.printInfo();
    }
}
