/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.orm.factory.orient;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.io.FilenameUtils;
import org.ioc.annotations.context.IoCRepository;
import org.ioc.annotations.context.Order;
import org.ioc.cache.ICache;
import org.ioc.cache.ICacheFactory;
import org.ioc.context.factories.Factory;
import org.ioc.context.processors.DestroyProcessor;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.sensible.factories.CacheFactorySensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration;
import org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.DDL;
import org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.OrientType;
import org.ioc.exceptions.IoCException;
import org.ioc.orm.cache.FacilityCacheManager;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.Schema;
import org.ioc.orm.factory.facility.FacilityManager;
import org.ioc.orm.factory.facility.FacilityManagerFactory;
import org.ioc.orm.factory.orient.pool.ConstantODBPool;
import org.ioc.orm.factory.orient.pool.LocalODBPool;
import org.ioc.orm.factory.orient.query.OrientQuery;
import org.ioc.orm.factory.orient.session.OrientDatabaseSessionFactory;
import org.ioc.orm.generator.IdProducer;
import org.ioc.orm.generator.type.OrientIdProducer;
import org.ioc.orm.metadata.inspectors.FacilityMetadataInspector;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.type.IndexMetadata;
import org.ioc.orm.metadata.type.SchemaMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitorFactory;
import org.ioc.orm.metadata.visitors.column.factory.OrientVisitorFactory;
import org.ioc.orm.repositories.CrudRepository;
import org.ioc.orm.repositories.SampleRepository;
import org.ioc.orm.repositories.proxy.RepositoryHandler;
import org.ioc.orm.util.OrientUtils;
import org.ioc.orm.util.QueryingUtils;
import org.ioc.utils.collections.ArrayListSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.persistence.Entity;
import javax.persistence.TableGenerator;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE.UNIQUE;
import static com.orientechnologies.orient.core.metadata.schema.OType.STRING;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.GenerationType.TABLE;
import static org.apache.commons.lang3.StringUtils.join;
import static org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.DDL.dropCreate;
import static org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.DDL.none;
import static org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.OrientType.*;
import static org.ioc.orm.util.OrientUtils.*;
import static org.ioc.utils.ReflectionUtils.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Order(998)
public class OrientSchemaFactory implements Schema, ContextSensible, DestroyProcessor, Factory, CacheFactorySensible, EnvironmentSensible<OrientDatasourceAutoConfiguration> {
	private static final Logger log = LoggerFactory.getLogger(OrientSchemaFactory.class);
	private static final ColumnVisitorFactory columnFactory = new OrientVisitorFactory();

	private ICacheFactory cacheFactory;
	private FacilityCacheManager cache;

	private final Map<String, OrientQuery> queryMap = new ConcurrentHashMap<>();
	private ODBPool pool;
	private DDL ddl;
	private SchemaMetadata schemaMetadata;
	private OrientDatasourceAutoConfiguration configuration;
	private IoCContext context;
	private FacilityManager facilityManager;

	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	@Override
	public void initialize() throws IoCException {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		final OrientType type = configuration.getDatabaseType();
		final String url = configuration.getUrl();
		final String database = configuration.getDatabase();
		final String username = configuration.getUsername();
		final String password = configuration.getPassword();

		final File path = new File(url);

		ddl = configuration.getDdlAuto();

		final String[] packages = context.getPackages();
		final List<Class<?>> types = findClassesByAnnotation(Entity.class, packages);
		if (type == LOCAL) {
			String url1 = "plocal:" + FilenameUtils.normalize(path.getAbsolutePath());
			pool = new LocalODBPool(url1, "admin", "admin");
		} else if (type == LOCAL_SERVER) {
			log.error("Not support databaseDocument type. Coming soon!");
			return;
		} else if (type == REMOTE) {
			pool = new ConstantODBPool(url, database, username, password);
		}

		final FacilityMetadataInspector parser = new FacilityMetadataInspector(columnFactory, types);
		schemaMetadata = new SchemaMetadata(parser.inspect());

		Orient.instance().startup();
		createSchema();

		final List<Class<?>> repositories = findClassesByAnnotation(IoCRepository.class, packages);
		facilityManager = new FacilityManagerFactory(this, getMetadata()).create();

		repositories.stream().filter(CrudRepository.class::isAssignableFrom).forEach(c -> {
			final ParameterizedType parameterizedType = (ParameterizedType) c.getGenericInterfaces()[0];
			final Class<?> entity = (Class<?>) parameterizedType.getActualTypeArguments()[0];
			final Object repo = installProxyRepository(facilityManager, c, entity);
			final String repoName = resolveTypeName(getOrigin(repo.getClass()));
			context.setType(repoName, repo);

			if (log.isDebugEnabled()) {
				log.debug("Registered repository type - [{}]", repoName);
			}
		});

		final ICache<FacilityMetadata, Map<Object, WeakReference>> objects =
				cacheFactory.installEternal("entity-cache", 1000);
		cache = new FacilityCacheManager(objects);

		log.info("Successful initialized [{}]", getClass().getSimpleName());
	}

	/**
	 * Initialize user's repository to proxy class for intercept custom function's or CRUD.
	 *
	 * @param facilityManager entity manager
	 * @param interfaceClass  type of repository
	 * @param entityClass     repository controlled entity class
	 * @param <E>             generic entity type
	 * @param <ID>            generic primary key type
	 * @return initialized proxy
	 */
	private <E, ID> Object installProxyRepository(FacilityManager facilityManager, Class<?> interfaceClass, Class<E> entityClass) {
		final boolean logQueries = configuration.isLogQueries();
		final FacilityMetadata facilityMetadata = schemaMetadata.getMetadata(entityClass);
		final SampleRepository<E, ID> repository = new SampleRepository<>(facilityManager, entityClass);
		final RepositoryHandler dynamicRepository = new RepositoryHandler(facilityManager, entityClass, facilityMetadata,
				repository, logQueries);

		final Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(new Class[]{interfaceClass});
		enhancer.setCallback(dynamicRepository);
		enhancer.setCallbackFilter(m -> m.isBridge() ? 1 : 0);
		return enhancer.create();
	}

	/**
	 * Set the {@link IoCContext} to component.
	 *
	 * @param context initialized application contexts
	 * @throws IoCException throw if contexts throwing by methods
	 */
	@Override
	public void contextInform(IoCContext context) throws IoCException {
		this.context = context;
	}

	@Override
	public void environmentInform(OrientDatasourceAutoConfiguration configuration) throws IoCException {
		this.configuration = configuration;
	}

	@Override
	public void factoryInform(Factory cacheFactory) throws IoCException {
		if (ICacheFactory.class.isAssignableFrom(cacheFactory.getClass())) {
			this.cacheFactory = (ICacheFactory) cacheFactory;
		}
	}

	public ODatabaseDocument createSchema() {
		return pool.acquire();
	}

	@Override
	public SchemaMetadata getMetadata() {
		return schemaMetadata;
	}

	@Override
	public OrientDatabaseSessionFactory openSession() {
		return new OrientDatabaseSessionFactory(createSchema(), queryMap, cache);
	}

	@Override
	public void update() {
		if (ddl == none) {
			return;
		}

		try (ODatabaseDocument database = createSchema()) {
			schemaMetadata.getFacilityMetadataCollection().forEach(entity -> {
				updateFacilityWithoutTransaction(entity, database);
				updateProducers(entity, database);
			});
		} catch (Exception e) {
			throw new OrmException("Unable to refresh database schema.", e);
		}
	}

	@Override
	public void installQuery(FacilityMetadata facilityMetadata, String name, String query) {
		if (name == null || name.isEmpty()) {
			return;
		}

		if (query == null || query.isEmpty()) {
			return;
		}

		if (queryMap.containsKey(name)) {
			return;
		}

		final String tableQuery = QueryingUtils.prepareQuery(facilityMetadata, query);
		if (tableQuery == null || tableQuery.isEmpty()) {
			return;
		}

		final OrientQuery prepared = new OrientQuery(query, tableQuery);
		queryMap.put(name, prepared);
	}

	@Override
	public boolean uninstallQuery(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}

		return queryMap.remove(name) != null;
	}

	@Override
	public void closeSession() {
		pool.close();
	}

	@Override
	public void destroy() {
		cache = null;

		if (facilityManager != null) {
			facilityManager.close();
			facilityManager = null;
		}

		closeSession();
	}

	/**
	 * Function of refresh primary key producers in entity meta data.
	 *
	 * @param facilityMetadata entity meta data
	 * @param databaseDocument orient schema
	 */
	private void updateProducers(FacilityMetadata facilityMetadata, ODatabaseDocument databaseDocument) {
		facilityMetadata.getTypes().forEach(entityType -> {
			final FacilityMetadataInspector analyzer = new FacilityMetadataInspector(new OrientVisitorFactory(), entityType);
			analyzer.getProducers(entityType).forEach((field, producer) -> {
				final String type = producer.generator();
				String tableName = "id_producer";
				String keyColumn = "id";
				String valueColumn = "value";

				if (producer.strategy() == IDENTITY) {
					throw new OrmException("No support available primary key producing.");
				} else if (producer.strategy() == TABLE) {
					for (TableGenerator table : searchAnnotations(entityType, TableGenerator.class)) {
						if (type.equalsIgnoreCase(table.name())) {
							if (!table.table().isEmpty()) {
								tableName = table.table();
							}

							if (!table.pkColumnName().isEmpty()) {
								keyColumn = table.pkColumnName();
							}

							if (!table.valueColumnName().isEmpty()) {
								valueColumn = table.valueColumnName();
							}
						}
					}
				}

				final String fieldName = field.getName();
				final String indexName = tableName + "." + keyColumn;
				final ColumnMetadata columnMetadata = facilityMetadata.findColumnMetadata(fieldName);

				if (columnMetadata == null) {
					throw new OrmException("Unable to locate primary key [" + fieldName + "] for entity [" + facilityMetadata + "].");
				}

				if (ddl == dropCreate) {
					if (hasCluster(databaseDocument, tableName)) {
						databaseDocument.command(new OCommandSQL("DELETE FROM " + tableName)).execute();
						databaseDocument.getMetadata().getSchema().dropClass(tableName);
					}

					if (hasIndex(databaseDocument, indexName)) {
						databaseDocument.command(new OCommandSQL("DROP INDEX " + indexName)).execute();
						databaseDocument.getMetadata().getIndexManager().dropIndex(indexName);
					}
				}

				final OClass createClass = databaseDocument.getMetadata().getSchema().getOrCreateClass(tableName);
				if (!createClass.existsProperty(keyColumn)) {
					createClass.createProperty(keyColumn, STRING);
				}

				if (!createClass.existsProperty(valueColumn)) {
					createClass.createProperty(valueColumn, OrientUtils.columnType(columnMetadata));
				}

				if (!createClass.areIndexed(keyColumn)) {
					createClass.createIndex(indexName, UNIQUE, keyColumn);
				}

				final IdProducer orientIdProducer = new OrientIdProducer(tableName, indexName, keyColumn, valueColumn, this);
				facilityMetadata.setProducer(columnMetadata, orientIdProducer);
				log.info("Set counter id generator for [{}] on entity [{}].", columnMetadata, facilityMetadata);
			});
		});
	}

	/**
	 * Refresh entity tables in schema database.
	 *
	 * @param facilityMetadata entity meta data
	 * @param databaseDocument orient schema
	 */
	private void updateFacilityWithoutTransaction(FacilityMetadata facilityMetadata, ODatabaseDocument databaseDocument) {
		final String keyIndex = keyIndex(facilityMetadata);
		final String schemaName = facilityMetadata.getTable();

		if (ddl == dropCreate) {
			if (hasClass(databaseDocument, schemaName)) {
				databaseDocument.command(new OCommandSQL("DELETE FROM " + schemaName + " UNSAFE")).execute();
			}

			if (hasIndex(databaseDocument, keyIndex)) {
				databaseDocument.command(new OCommandSQL("DROP INDEX " + keyIndex)).execute();
			}

			for (IndexMetadata indexMetadata : facilityMetadata.getIndexMetadataList()) {
				final String indexName;
				if (indexMetadata.getMetadataList().size() == 1) {
					final ColumnMetadata column = indexMetadata.getMetadataList().iterator().next();
					indexName = schemaName + "." + column.getName();
				} else {
					indexName = schemaName + "." + indexMetadata.getName();
				}

				if (hasIndex(databaseDocument, indexName)) {
					databaseDocument.command(new OCommandSQL("DROP INDEX " + indexName)).execute();
				}
			}
		}

		final OClass schemaClass = databaseDocument.getMetadata().getSchema().getOrCreateClass(schemaName);
		final Set<ColumnMetadata> metadataSet = new ArrayListSet<>();
		for (ColumnMetadata columnMetadata : facilityMetadata) {
			final String property = columnMetadata.getName();
			final OType type = OrientUtils.columnType(columnMetadata);
			if (type != null && !schemaClass.existsProperty(property)) {
				schemaClass.createProperty(property, type);
			}

			if (columnMetadata.isPrimaryKey()) {
				metadataSet.add(columnMetadata);
			}
		}

		if (!hasIndex(databaseDocument, keyIndex)) {
			final Collection<String> names = metadataSet.stream()
					.filter(Objects::nonNull)
					.map(ColumnMetadata::getName)
					.collect(Collectors.toList());
			if (!names.isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug("Adding primary index [{}] with {}", keyIndex, names);
				}

				final String indexQuery = "install index " + keyIndex + " on " + schemaName + " (" + join(names, ",")
						+ ") unique";

				databaseDocument.command(new OCommandSQL(indexQuery)).execute();
			}
		}

		facilityMetadata.getIndexMetadataList().forEach(index -> {
			final String uniqueness = index.isUnique() ? "unique" : "notunique";
			final Collection<String> names = index.getMetadataList()
					.stream()
					.map(ColumnMetadata::getName)
					.collect(Collectors.toList());

			final String indexName = schemaName + "." + index.getName();
			if (!names.isEmpty() && !hasIndex(databaseDocument, indexName)) {
				if (log.isDebugEnabled()) {
					log.debug("Adding property index [{}] with {} ({})", indexName, names, uniqueness);
				}

				final String indexQuery = "install index " + indexName + " on " + schemaName + " (" + join(names, ",")
						+ ") " + uniqueness;

				databaseDocument.command(new OCommandSQL(indexQuery)).execute();
			}
		});
	}
}
