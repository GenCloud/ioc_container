package org.di.test;

import org.apache.log4j.BasicConfigurator;
import org.di.annotations.ScanPackage;
import org.di.context.AppContext;
import org.di.context.runner.IoCStarter;
import org.di.factories.DependencyFactory;
import org.di.test.components.ComponentA;
import org.di.test.components.ComponentB;
import org.di.test.components.ComponentC;
import org.di.test.components.ComponentD;
import org.di.test.environments.ExampleEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@ScanPackage(packages = {"org.di.test", "org.di"})
public class MainTest {
    private static final Logger log = LoggerFactory.getLogger(MainTest.class);

    public static void main(String... args) {
        BasicConfigurator.configure();

        final AppContext context = IoCStarter.start(MainTest.class, args);
        printStatistic(context);

        log.info("Getting configuration instance");
        final ExampleEnvironment exampleEnvironment = (ExampleEnvironment) context.getType(ExampleEnvironment.class);
        log.info(exampleEnvironment.toString());

        log.info("Getting type from context");
        final ComponentA componentA = (ComponentA) context.getType(ComponentA.class);
        log.info(componentA.toString());

        log.info("Getting type from context");
        final ComponentB componentB = (ComponentB) context.getType(ComponentB.class);
        log.info(componentB.toString());

        log.info("Getting type from context");
        final ComponentC componentC = (ComponentC) context.getType(ComponentC.class);
        log.info(componentC.toString());

        log.info("Getting MainController from context");
        final ComponentD componentD = (ComponentD) context.getType(ComponentD.class);
        log.info(componentD.toString());
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
