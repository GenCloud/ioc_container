---

## Module 'orm-factory'

### Intro
Add orm-factory module to your project. for maven projects just add this dependency:   
```xml
    <repositories>
        <repository>
            <id>ioc_cache</id>
            <url>https://raw.github.com/GenCloud/ioc_container/orm</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </repository>
    </repositories>
     
    <dependencies>
        <dependency>
            <groupId>org.ioc</groupId>
            <artifactId>orm-factory</artifactId>
            <version>2.3.0.RELEASE</version>
        </dependency>
    </dependencies>
```

A typical use of threads-factory module would be:
1. Add in Main class of application marker-annotation of enabled this module
```java
     @DatabaseModule //default datasource - Orient
     @ScanPackage(packages = {"org.ioc.test"})
     public class MainTest {
         public static void main(String... args){
           IoCStarter.start(MainTest.class, args);
         }
     }
```
* support datasource: OrientDB Schema
* support JPA annotations
* default configurations for orm factory
```properties
# Datasource
datasource.orient.database-type=LOCAL
datasource.orient.url=./database
datasource.orient.database=orient
datasource.orient.username=admin
datasource.orient.password=admin
datasource.orient.ddl-auto=dropCreate
datasource.orient.showSql=true
```

2. Create custom component, repositories and entity classes:
* entity classes:
```java
@Entity // marker for an inspector defining what an entity is.
// specifies the primary table for annotated entity
@Table(name = "child_entity", indexes = {
		//Indexes for table. These are only used if table generation is in effect. Defaults to no additional indexes.
		@Index(columnList = "name, sample_entity", unique = true) 
})
public class ChildEntity implements Serializable {
	@Id //identifier for primary key entity
	//Provides for specification of generation strategies for values of primary keys
	@GeneratedValue(strategy = GenerationType.SEQUENCE) 
	private long id;

	//Used to specify the mapped column for field
	@Column(name = "name")
	private String name;

        //Specifies column for joining an entity relations or element collection
	@JoinColumn(name = "sample_entity_id")
	//Defines single-valued association to another entity class that has many-to-one multiplicity
	@ManyToOne(fetch = FetchType.LAZY, cascade = ALL)
	private SampleEntity sampleEntity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SampleEntity getSampleEntity() {
		return sampleEntity;
	}

	public void setSampleEntity(SampleEntity sampleEntity) {
		this.sampleEntity = sampleEntity;
	}

	@Override
	public String toString() {
		return "ChildEntity{" +
				"id=" + id +
				", name='" + name
				+ '}';
	}
}

@Entity
public class OneToOneEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private long id;

        //Defines single-valued association to another entity that has one-to-one multiplicity
	@OneToOne(fetch = FetchType.LAZY, cascade = ALL)
	private SampleEntity sampleEntity;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public SampleEntity getSampleEntity() {
		return sampleEntity;
	}

	public void setSampleEntity(SampleEntity sampleEntity) {
		this.sampleEntity = sampleEntity;
	}

	@Override
	public String toString() {
		return "OneToOneEntity{" +
				"id=" + id +
				'}';
	}
}

@Entity
@Table(name = "sample_entity")
//Specifies a static, named query in the Java Persistence query language
@NamedQuery(name = "SampleEntity.findById", query = "select from sample_entity where id = :id")
public class SampleEntity implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private long id;

	@Column(name = "name")
	private String name;

	@Column(name = "year")
	private String year;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL)
	private OneToOneEntity oneToOneEntity;

        //Defines many-valued association with one-to-many multiplicity
	@OneToMany(fetch = FetchType.LAZY, cascade = ALL)
	private List<ChildEntity> childEntities = new ArrayList<>();

	public long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public List<ChildEntity> getChildEntities() {
		return childEntities;
	}

	public void setChildEntities(List<ChildEntity> childEntities) {
		this.childEntities = childEntities;
	}

	public OneToOneEntity getOneToOneEntity() {
		return oneToOneEntity;
	}

	public void setOneToOneEntity(OneToOneEntity oneToOneEntity) {
		this.oneToOneEntity = oneToOneEntity;
	}

	@Override
	public String toString() {
		return "SampleEntity{" +
				"id=" + id +
				", name='" + name + '\'' +
				", year='" + year + '\'' +
				'}';
	}
}
```

* repository classes:
```java
@IoCRepository //mandatory annotation-marker for channel inspector
public interface ChildEntityRepository extends CrudRepository<ChildEntity, Long> {
}

@IoCRepository
public interface OneToOneEntityRepository extends CrudRepository<OneToOneEntity, Long> {
}

@IoCRepository
public interface SampleEntityRepository extends CrudRepository<SampleEntity, Long> {
	@Transactional // generated query will be executed in the transaction
	SampleEntity findByNameEqAndYearEq(String name, String year);

	@Transactional
	List<SampleEntity> findByNameEq(String name);

	@Transactional
	/** Marker annotation for identifying named requests registered in entity and their execution.
	  * name() - query name, defined in @NamedQuery annotation in entity class;
	  * params() - parameters to replace when executing the query.
	  * 
	  * <pre>
	  * &#34;SampleEntity
	  *     @NamedQuery(name = "SampleEntity.findById", query = "select from sample_entity where id = :id")
          *     public class SampleEntity
          *     ...
          * &#42;SampleEntityRepository
          *     //parameter id is replaced when query has been execution
          *     @Query(name = "SampleEntity.findById", params = "id")
          *     SampleEntity namedQuery(long id);
	  */
	@Query(name = "SampleEntity.findById", params = "id")
	SampleEntity namedQuery(long id);
}
```

make test component:
```java
@IoCComponent
public class DatabaseComponent {
	@IoCDependency
	private SampleEntityRepository sampleEntityRepository;

	@IoCDependency
	private OneToOneEntityRepository oneToOneEntityRepository;

	@IoCDependency
	private ChildEntityRepository childEntityRepository;

        //CRUD default repository operation: save entity
	public void saveOneToOneEntity(OneToOneEntity oneToOneEntity) {
		oneToOneEntityRepository.save(oneToOneEntity);
	}

        //CRUD default repository operation: save entity
	public void saveChildEntity(ChildEntity childEntity) {
		childEntityRepository.save(childEntity);
	}

        //CRUD default repository operation: save entity
	public void saveSampleEntity(SampleEntity sampleEntity) {
		sampleEntityRepository.save(sampleEntity);
	}

        //CRUD default repository operation: find entity by your id
	public SampleEntity findSampleEntity(long id) {
		return sampleEntityRepository.fetch(id);
	}

        //find entity by defined named query
	public SampleEntity findByNamedQuery(long id) {
		return sampleEntityRepository.namedQuery(id);
	}

        //find entity by custom query where SampleEntity#name equals name and year
	public SampleEntity findSampleEntityByName(String name) {
		return sampleEntityRepository.findByNameEqAndYearEq(name, "2018");
	}

        //find all entities by custom query where SampleEntity#name equals name
	public List<SampleEntity> findAllByName(String name) {
		return sampleEntityRepository.findByNameEq(name);
	}

        //CRUD default repository operation: find all entities in table
	public List<SampleEntity> findAll() {
		return sampleEntityRepository.fetchAll();
	}

        //CRUD default repository operation: delete entity in table
	public void deleteSampleEntity(SampleEntity sampleEntity) {
		sampleEntityRepository.delete(sampleEntity);
	}
}
```

Try usage component in MainClass:
```java
    @DatabaseModule
    @ScanPackage(packages = {"org.ioc.test"})
    public class MainTest {
        public static void main(String... args) {
            DefaultIoCContext channel = IoCStarter.start(MainTest.class, args);
            final DatabaseComponent databaseComponent = channel.getType(DatabaseComponent.class);
       
       		log.info("Inserting test dataContainer into Schema");
       		final SampleEntity sampleEntity = new SampleEntity();
       		sampleEntity.setName("sample28");
       		sampleEntity.setYear("2018");
       
       		final SampleEntity sampleEntity1 = new SampleEntity();
       		sampleEntity1.setName("sample28");
       		sampleEntity1.setYear("2018");
       		databaseComponent.saveSampleEntity(sampleEntity1);
       
       		final SampleEntity sampleEntity2 = new SampleEntity();
       		sampleEntity2.setName("sample28");
       		sampleEntity2.setYear("2018");
       		databaseComponent.saveSampleEntity(sampleEntity2);
       
       		final SampleEntity sampleEntity3 = new SampleEntity();
       		sampleEntity3.setName("sample28");
       		sampleEntity3.setYear("2018");
       		databaseComponent.saveSampleEntity(sampleEntity3);
       
       		final OneToOneEntity oneToOneEntity = new OneToOneEntity();
       		sampleEntity.setOneToOneEntity(oneToOneEntity);
       		oneToOneEntity.setSampleEntity(sampleEntity);
       		databaseComponent.saveOneToOneEntity(oneToOneEntity);
       
       		final ChildEntity childEntity = new ChildEntity();
       		childEntity.setName("child1");
       		childEntity.setSampleEntity(sampleEntity);
       
       		databaseComponent.saveChildEntity(childEntity);
       
       		sampleEntity.getChildEntities().add(childEntity);
       		databaseComponent.saveSampleEntity(sampleEntity);
       
       		log.info("Fetch test data from Schema by generated query");
       		final SampleEntity get0 = databaseComponent.findSampleEntityByName("sample28");
       		log.info(get0.toString());
       
       		log.info("Fetch test data from Schema by named query");
       		final SampleEntity customQuery = databaseComponent.findByNamedQuery(sampleEntity.getId());
       		log.info(customQuery.toString());
       
       		log.info("Fetch all test data from Schema");
       		final List<SampleEntity> get1 = databaseComponent.findAll();
       		log.info(get1.toString());
       
       		log.info("Fetch all test data from Schema by generated query");
       		final List<SampleEntity> sampleEntityList = databaseComponent.findAllByName("sample28");
       		log.info(sampleEntityList.toString());
       
       		log.info("Fetch all test data from Entity cache");
       		final List<SampleEntity> get2 = databaseComponent.findAll();
       		log.info(get2.toString());
       
       		log.info("Delete all test data from Schema");
       		get2.forEach(databaseComponent::deleteSampleEntity);
        }
    }
```
    
4) Use it!

### Contribute
Pull requests are welcomed!!

The license is [GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.html/).

_GenCloud_
