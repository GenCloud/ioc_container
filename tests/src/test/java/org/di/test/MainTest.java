package org.di.test;

import org.apache.log4j.BasicConfigurator;
import org.di.context.annotations.Lazy;
import org.di.context.annotations.ScanPackage;
import org.di.context.annotations.modules.ThreadingModule;
import org.di.context.contexts.AppContext;
import org.di.context.contexts.runner.IoCStarter;
import org.di.context.factories.DependencyInitiator;
import org.di.test.components.*;
import org.di.test.components.abstrac.AbstractComponent;
import org.di.test.components.abstrac.TestAbstractComponent;
import org.di.test.components.inter.InterfaceComponent;
import org.di.test.components.inter.MyInterface;
import org.di.test.components.lazy.ComponentForLazyDep;
import org.di.test.components.lazy.LazyComponentA;
import org.di.test.components.lazy.LazyComponentB;
import org.di.test.environments.ExampleEnvironment;
import org.di.test.threading.ComponentThreads;
import org.di.threads.configuration.ThreadingConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author GenCloud
 * @date 04.09.2018
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ThreadingModule
@ScanPackage(packages = {"org.di.test"})
public class MainTest extends Assert {
    private static final Logger log = LoggerFactory.getLogger(MainTest.class);

    private static AppContext appContext;

    @BeforeClass
    public static void initializeContext() {
        BasicConfigurator.configure();
        appContext = IoCStarter.start(MainTest.class, (String) null);
    }

    @Test
    public void a_printStatistic() {
        DependencyInitiator dependencyInitiator = appContext.getDependencyInitiator();
        log.info("Initializing singleton types - {}", dependencyInitiator.getSingletons().size());
        log.info("Initializing proto types - {}", dependencyInitiator.getPrototypes().size());

        log.info("For Each singleton types");
        for (Object o : dependencyInitiator.getSingletons().values()) {
            log.info("------- {}", o.getClass().getSimpleName());
        }

        log.info("For Each proto types");
        for (Object o : dependencyInitiator.getPrototypes().values()) {
            log.info("------- {}", o.getClass().getSimpleName());
        }
    }

    @Test
    public void b_testInstantiatedComponents() {
        log.info("Getting ExampleEnvironment from contexts");
        final ExampleEnvironment exampleEnvironment = appContext.getType(ExampleEnvironment.class);
        assertNotNull(exampleEnvironment);
        log.info(exampleEnvironment.toString());

        log.info("Getting ComponentB from contexts");
        final ComponentB componentB = appContext.getType(ComponentB.class);
        assertNotNull(componentB);
        log.info(componentB.toString());

        log.info("Getting ComponentC from contexts");
        final ComponentC componentC = appContext.getType(ComponentC.class);
        assertNotNull(componentC);
        log.info(componentC.toString());

        log.info("Getting ComponentD from contexts");
        final ComponentD componentD = appContext.getType(ComponentD.class);
        assertNotNull(componentD);
        log.info(componentD.toString());
    }

    @Test
    public void c_testProto() {
        log.info("Getting ComponentA from contexts (first call)");
        final ComponentA componentAFirst = appContext.getType(ComponentA.class);
        log.info("Getting ComponentA from contexts (second call)");
        final ComponentA componentASecond = appContext.getType(ComponentA.class);
        assertNotSame(componentAFirst, componentASecond);
        log.info(componentAFirst.toString());
        log.info(componentASecond.toString());
    }

    @Test
    public void d_testInterfacesAndAbstracts() {
        log.info("Getting MyInterface from contexts");
        final InterfaceComponent myInterface = appContext.getType(MyInterface.class);
        log.info(myInterface.toString());

        log.info("Getting TestAbstractComponent from contexts");
        final AbstractComponent testAbstractComponent = appContext.getType(TestAbstractComponent.class);
        log.info(testAbstractComponent.toString());
    }

    @Test
    public void e_testLazys() {
        log.info("Getting Lazy object from contexts");
        final List<Object> lazys = appContext.getDependencyInitiator().getSingletons()
                .values()
                .stream()
                .filter(o -> o.getClass().isAnnotationPresent(Lazy.class))
                .collect(Collectors.toList());

        log.info("Found {} lazily initialized types that are in dependencies", lazys.size());
        if (!lazys.isEmpty()) {
            log.info("For Each lazy types");
            lazys.forEach(o -> log.info(o.toString()));
        }

        final Optional<Object> o = lazys
                .stream()
                .filter(o1 -> o1.getClass().getSimpleName().equals(LazyComponentA.class.getSimpleName()))
                .findFirst();
        assertTrue("Independent lazy component is not initialized in the factory", !o.isPresent());

        log.info("Getting LazyComponentA from contexts");
        final LazyComponentA lazyComponentA = appContext.getType(LazyComponentA.class);
        log.info(lazyComponentA.toString());

        log.info("Getting LazyComponentB from contexts");
        final LazyComponentB lazyComponentB = appContext.getType(LazyComponentB.class);
        log.info(lazyComponentB.toString());

        log.info("Getting ComponentForLazyDep from contexts");
        final ComponentForLazyDep componentForLazyDep = appContext.getType(ComponentForLazyDep.class);
        log.info(componentForLazyDep.toString());
    }

    @Test
    public void f_testSensibleComponents() {
        log.info("Getting SensibleContextComponent from contexts");
        final SensibleContextComponent sensibleContextComponent = appContext.getType(SensibleContextComponent.class);
        log.info(sensibleContextComponent.toString());
    }

    @Test
    public void g_testThreading() {
        log.info("Getting ThreadingConfiguration from contexts");
        final ThreadingConfiguration threadingConfiguration = appContext.getType(ThreadingConfiguration.class);
        assertNotNull(threadingConfiguration);
        log.info(threadingConfiguration.toString());

        log.info("Getting ComponentThreads from contexts");
        appContext.getType(ComponentThreads.class);
    }

    @Test
    public void z_testCloseContext() {
        System.exit(1);
    }
}
