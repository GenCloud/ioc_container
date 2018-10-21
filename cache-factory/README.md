---

## Module 'cache-factory'
[![Build Status](https://travis-ci.org/GenCloud/ioc_container.svg?branch=master)](https://travis-ci.org/GenCloud/ioc_container)

### Intro
Add cache-factory module to your project. for maven projects just add this dependency:
```xml
   <repositories>
       <repository>
           <id>web</id>
           <url>https://raw.github.com/GenCloud/ioc_container/cache</url>
           <snapshots>
               <enabled>true</enabled>
               <updatePolicy>always</updatePolicy>
           </snapshots>
       </repository>
   </repositories>
    
   <dependencies>
       <dependency>
           <groupId>org.ioc</groupId>
           <artifactId>cache-factory</artifactId>
           <version>2.2.3.STABLE</version>
       </dependency>
   </dependencies>
```
    
A typical use of cache-factory module would be:
1) Add in Main class of application marker-annotation of enabled this module
```java
    @CacheModule
    @ScanPackage(packages = {"org.ioc.test"})
    public class MainTest {
        public static void main(String... args){
          IoCStarter.start(MainTest.class, args);
        }
    }
```
* default configurations for cache factory
```properties
# Cache
cache.factory=org.ioc.cache.impl.EhFactory
```

2) Mark sample component of inheritance CacheFactorySensible
```java
    @IoCComponent
    public class CacheComponentTest implements CacheFactorySensible {
        private static final Logger log = LoggerFactory.getLogger(CacheComponentTest.class);
    
        private EhFactory factory;
    
        private ICache<String, String> sampleCache;
    
        @PostConstruct
        public void initializeCache() {
            sampleCache = factory.installEternal("sample-test-cache", 200);
    
            log.info("Creating sample cache - [{}]", sampleCache);
    
            sampleCache.put("1", "First");
            sampleCache.put("2", "Second");
            sampleCache.put("3", "Third");
            sampleCache.put("4", "Fourth");
    
            log.info("Loaded size - [{}]", sampleCache.size());
        }
    
        public String getElement(String key) {
            final String value = sampleCache.get(key);
            log.info("Getting value from cache - [{}]", value);
            return value;
        }
    
        public void removeElement(String key) {
            log.info("Remove object from cache");
            sampleCache.remove(key);
        }
    
        public void invalidate() {
            sampleCache.clear();
            log.info("Clear all cache, size - [{}]", sampleCache.size());
        }
    
        @Override
        public void factoryInform(Factory factory) throws IoCException {
            this.factory = (EhFactory) factory;
        }
    
        @Override
        public String toString() {
            return "CacheComponentTest{" +
                    "factory=" + factory +
                    ", sampleCache=" + sampleCache +
                    '}';
        }
    }
```
3) Default methods of factory
- cache management
```java
            //Add pair <K, V> to cache. Notice: if there is already a value with given id in map,
            // {@link IllegalArgumentException} will be thrown.
           void put(K, V);
       
           //Returns cached value correlated to given key.
           V get(K);
       
           //Checks whether this map contains a value related to given key.
           boolean contains(K key);
       
           //Removes an entry from map, that has given key.
           void remove(K key);
       
           //Clears cache.
           void clear();
       
           //size of cache map
           int size();
```
    
4) Use it!

### Contribute
Pull requests are welcomed!!

The license is [GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.html/).

_GenCloud_
