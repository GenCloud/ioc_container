---

## Module 'threads-factory'

[![Build Status](https://api.travis-ci.org/GenCloud/di_container.svg?branch=master)](https://api.travis-ci.org/GenCloud/di_container)
### Intro
Add threads-factory module to your project. for maven projects just add this dependency:
```xml
    <repositories>
        <repository>
            <id>di_container-mvn-repo</id>
            <url>https://raw.github.com/GenCloud/di_container/threading/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>org.genfork</groupId>
            <artifactId>threads-factory</artifactId>
            <version>0.0.2-STABLE</version>
        </dependency>
    </dependencies>
```

A typical use of threads-factory module would be:
1) Add in Main class of application marker-annotation of enabled this module
```java
@ThreadingModule
@ScanPackage(packages = {"org.di.test"})
public class MainTest {
    public static void main(String... args){
      IoCStarter.start(MainTest.class, args);
    }
}
```
2) Mark sample component of inheritance ThreadFactorySensible<F>
```java
@IoCComponent
public class ComponentThreads implements ThreadFactorySensible<DefaultThreadingFactory> {
    private final Logger log = LoggerFactory.getLogger(AbstractTask.class);

    private DefaultThreadingFactory defaultThreadingFactory; //Thread factory to instantiate by Sensibles

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        // scheduling sample task
        defaultThreadingFactory.async(new AbstractTask<Void>() {
            @Override
            public Void call() {
                log.info("Start test thread!");
                return null;
            }
        });
    }

    @Override
    public void threadFactoryInform(DefaultThreadingFactory defaultThreadingFactory) throws IoCException {
        this.defaultThreadingFactory = defaultThreadingFactory;
    }

    // register method in runnable task and start running it
    @SimpleTask(startingDelay = 1, fixedInterval = 5)
    public void schedule() {
        log.info("I'm Big Daddy, scheduling and incrementing param - [{}]", atomicInteger.incrementAndGet());
    }
}
```
3) Default methods of factory
- scheduling
```java
    // Executes an asynchronous tasks. Tasks scheduled here will go to an default shared thread pool.
    <T> AsyncFuture<T> async(Task<T>)
    // Executes an asynchronous tasks at an scheduled time. Please note that resources in scheduled
    // thread pool are limited and tasks should be performed fast.
    <T> AsyncFuture<T> async(long, TimeUnit, Task<T>)
    // Executes an asynchronous tasks at an scheduled time. Please note that resources in scheduled
    // thread pool are limited and tasks should be performed fast.
    ScheduledAsyncFuture async(long, TimeUnit, long, Runnable)
```

4) Use it!

### Contribute
Pull requests are welcomed!!

The license is [GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.html/).

_GenCloud_
