---

## DI (IoC) Container realization

[![Build Status](https://api.travis-ci.org/GenCloud/di_container.svg?branch=master)](https://api.travis-ci.org/GenCloud/di_container)
### Intro
Add IoC to your project. for maven projects just add this dependency:
```xml
    <repositories>
        <repository>
            <id>di_container-mvn-repo</id>
            <url>https://raw.github.com/GenCloud/di_container/mvn-repo/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>org.genfork</groupId>
            <artifactId>context</artifactId>
            <version>0.0.2-STABLE</version>
        </dependency>
    </dependencies>
```

A typical use of IoC would be:
```java
@ScanPackage(packages = {"org.di.test"})
public class MainTest {
    public static void main(String... args) {
        IoCStarter.start(MainTest.class, args);
    }
}
```

A component usage would be:
```java
@Lazy // annotation of component is marked for lazy-loading (firts call is instantiated)
@IoCComponent
@LoadOpt(PROTOTYPE) // type for loading - PROTOTYPE | SINGLETON, if !present annotation - component has default type SINGLETON
public class ComponentA {
    @Override
    public String toString() {
        return "ComponentA{hash:" + Integer.toHexString(hashCode()) + "}";
    }
}
```

A component dependency usage would be:
```java
    @IoCDependency // marked field for scanner found dependency
    private ComponentA componentA;
```

A configuration usage would be:
```java
@Property(path = "configs/ExampleEnvironment.properties") // main annotation for init environment
// path - destination of configuration file
public class ExampleEnvironment extends SamplePropertyListener {

    private String nameApp;

    private String[] components;

    @PropertyFunction
    public SampleProperty value() {
        return new SampleProperty(158);
    }

    @Override
    public String toString() {
        return "ExampleEnvironment{hash: " + Integer.toHexString(hashCode()) + ", nameApp='" + nameApp + '\'' +
                ", components=" + Arrays.toString(components) +
                '}';
    }
    
    public class SampleProperty {
        private int value;
    
        public SampleProperty(int value) {
            this.value = value;
        }
    
        public int getValue() {
            return value;
        }
    }
}
```
### Contribute
Pull requests are welcomed!!

The license is [GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.html/).

This library is published as an act of giving and generosity, from developers to developers. 

_GenCloud_