package org.di.test;

import org.apache.log4j.BasicConfigurator;
import org.di.annotations.ScanPackage;
import org.di.context.AppContext;
import org.di.context.runner.DIStarter;
import org.di.factories.DependencyFactory;
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
        printStatistic(context);
        log.info("Getting type from context");
        final DefaultComponent defaultComponent = (DefaultComponent) context.getType(DefaultComponent.class);
        log.info(defaultComponent.toString());

        log.info("Getting type from context");
        final DefaultService defaultService = (DefaultService) context.getType(DefaultService.class);
        log.info(defaultService.toString());

        defaultService.printInfo();

        log.info("Getting type from context");
        final Object o = context.getType(DefConstructorComponent.class);
        final DefConstructorComponent defConstructorComponent = (DefConstructorComponent) o;
        log.info(defConstructorComponent.toString());

        defConstructorComponent.printInfo();

        log.info("Getting MainController from context");
        final MainController mainController = (MainController) context.getType(MainController.class);
        log.info(mainController.toString());

        mainController.printInfo();
    }

    private static void printStatistic(AppContext context) {
        DependencyFactory dependencyFactory = context.getDependencyFactory();
        log.info("Initializing singleton types - {}", dependencyFactory.getSingletons().size());
        log.info("Initializing proto types - {}", dependencyFactory.getPrototypes().size());

        log.info("For Each singleton types");
        for (Object o : dependencyFactory.getSingletons().values()) {
            log.info("------- {}", o.getClass().getSimpleName());
        }

        log.info("For Each proto types");
        for (Object o : dependencyFactory.getPrototypes().values()) {
            log.info("------- {}", o.getClass().getSimpleName());
        }
    }
}
