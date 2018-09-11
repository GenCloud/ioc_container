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
import org.di.test.components.abstrac.AbstractComponent;
import org.di.test.components.abstrac.TestAbstractComponent;
import org.di.test.components.inter.InterfaceComponent;
import org.di.test.components.inter.MyInterface;
import org.di.test.environments.ExampleEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@ScanPackage(packages = {"org.di.test", "org.di"})
public class MainTest extends Assert {
    private static final Logger log = LoggerFactory.getLogger(MainTest.class);

    private AppContext appContext;

    @Before
    public void initializeContext() {
        BasicConfigurator.configure();
        appContext = IoCStarter.start(MainTest.class, (String) null);
    }

    @Test
    public void printStatistic() {
        DependencyFactory dependencyFactory = appContext.getDependencyFactory();
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

    @Test
    public void testInstantiatedComponents() {
        log.info("Getting ExampleEnvironment from context");
        final ExampleEnvironment exampleEnvironment = appContext.getType(ExampleEnvironment.class);
        assertNotNull(exampleEnvironment);
        log.info(exampleEnvironment.toString());

        log.info("Getting ComponentB from context");
        final ComponentB componentB = appContext.getType(ComponentB.class);
        assertNotNull(componentB);
        log.info(componentB.toString());

        log.info("Getting ComponentC from context");
        final ComponentC componentC = appContext.getType(ComponentC.class);
        assertNotNull(componentC);
        log.info(componentC.toString());

        log.info("Getting ComponentD from context");
        final ComponentD componentD = appContext.getType(ComponentD.class);
        assertNotNull(componentD);
        log.info(componentD.toString());
    }

    @Test
    public void testProto() {
        log.info("Getting ComponentA from context (first call)");
        final ComponentA componentAFirst = appContext.getType(ComponentA.class);
        log.info("Getting ComponentA from context (second call)");
        final ComponentA componentASecond = appContext.getType(ComponentA.class);
        assertNotSame(componentAFirst, componentASecond);
        log.info(componentAFirst.toString());
        log.info(componentASecond.toString());
    }

    @Test
    public void testInterfacesAndAbstracts() {
        log.info("Getting MyInterface from context");
        final InterfaceComponent myInterface = appContext.getType(MyInterface.class);
        log.info(myInterface.toString());

        log.info("Getting TestAbstractComponent from context");
        final AbstractComponent testAbstractComponent = appContext.getType(TestAbstractComponent.class);
        log.info(testAbstractComponent.toString());
    }
}
