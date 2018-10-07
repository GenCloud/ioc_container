/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of DI (IoC) Container Project.
 *
 * DI (IoC) Container Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DI (IoC) Container Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DI (IoC) Container Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.orm.factory.orient;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.io.FilenameUtils;
import org.ioc.annotations.context.IoCRepository;
import org.ioc.context.factories.Factory;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.sensible.EnvironmentSensible;
import org.ioc.context.type.IoCContext;
import org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration;
import org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.DDL;
import org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.OrientType;
import org.ioc.exceptions.IoCException;
import org.ioc.orm.cache.EntityCache;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.EntityManager;
import org.ioc.orm.factory.EntityManagerFactory;
import org.ioc.orm.factory.Schema;
import org.ioc.orm.factory.orient.pool.ConstantODBPool;
import org.ioc.orm.factory.orient.pool.LocalODBPool;
import org.ioc.orm.factory.orient.query.OrientQuery;
import org.ioc.orm.factory.orient.session.OrientDatabaseSession;
import org.ioc.orm.generator.IdGenerator;
import org.ioc.orm.generator.type.OrientIdGenerator;
import org.ioc.orm.metadata.inspectors.EntityMetadataInspector;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.EntityMetadata;
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

import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.TableGenerator;
import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static javax.persistence.GenerationType.*;
import static org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.DDL.dropCreate;
import static org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.DDL.none;
import static org.ioc.enviroment.configurations.datasource.OrientDatasourceAutoConfiguration.OrientType.*;
import static org.ioc.orm.util.OrientUtils.*;
import static org.ioc.utils.ReflectionUtils.*;
import static org.ioc.utils.StringUtils.camelToSnakeCase;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientSchemaFactory implements Schema, ContextSensible, Factory, EnvironmentSensible<OrientDatasourceAutoConfiguration> {
	private static final Logger log = LoggerFactory.getLogger(OrientSchemaFactory.class);
	private static final ColumnVisitorFactory columnFactory = new OrientVisitorFactory();

	private final EntityCache cache = new EntityCache();
	private final Map<String, OrientQuery> queryMap = new ConcurrentHashMap<>();
	private ODBPool pool;
	private DDL ddl;
	private SchemaMetadata schemaMetadata;
	private OrientDatasourceAutoConfiguration configuration;
	private IoCContext context;

	/**
	 * Default function for initialize installed object factories.
	 *
	 * @throws IoCException if factories throwing
	 */
	@Override
	public void initialize() throws IoCException {
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

		final EntityMetadataInspector parser = new EntityMetadataInspector(columnFactory, types);
		schemaMetadata = new SchemaMetadata(parser.analyze());

		createDatabase();

		final List<Class<?>> repositories = findClassesByAnnotation(IoCRepository.class, packages);
		final EntityManager entityManager = new EntityManagerFactory(this, getMetadata()).create();
		repositories.stream().filter(CrudRepository.class::isAssignableFrom).forEach(c -> {
			final ParameterizedType parameterizedType = (ParameterizedType) c.getGenericInterfaces()[0];
			final Class<?> entity = (Class<?>) parameterizedType.getActualTypeArguments()[0];
			final Object repo = installProxyRepository(entityManager, c, entity);
			final String repoName = resolveTypeName(getOrigin(repo.getClass()));
			context.setType(repoName, repo);

			if (log.isDebugEnabled()) {
				log.debug("Registered repository type - [{}]", repoName);
			}
		});

		log.info("Successful initialized [{}]", getClass().getSimpleName());
	}

	private <E, ID> Object installProxyRepository(EntityManager entityManager, Class<?> interfaceClass, Class<E> entityClass) {
		final boolean logQueries = configuration.isLogQueries();
		final EntityMetadata entityMetadata = schemaMetadata.getMetadata(entityClass);
		final SampleRepository<E, ID> repository = new SampleRepository<>(entityManager, entityClass);
		final RepositoryHandler dynamicRepository = new RepositoryHandler(entityManager, entityClass, entityMetadata,
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

	public ODatabaseDocument createDatabase() {
		return pool.acquire();
	}

	@Override
	public SchemaMetadata getMetadata() {
		return schemaMetadata;
	}

	@Override
	public OrientDatabaseSession openSession() {
		return new OrientDatabaseSession(createDatabase(), queryMap, cache);
	}

	@Override
	public void update() {
		if (ddl == none) {
			return;
		}

		try (ODatabaseDocument database = createDatabase()) {
			schemaMetadata.getEntityMetadataCollection().forEach(entity -> {
				refreshEntityWithTransaction(entity, database);
				refreshGenerators(entity, database);
			});
		} catch (Exception e) {
			throw new OrmException("Unable to refresh databaseDocument.", e);
		}
	}

	private void refreshGenerators(EntityMetadata entityMetadata, ODatabaseDocument databaseDocument) {
		entityMetadata.getTypes().forEach(entityType -> {
			final EntityMetadataInspector analyzer = new EntityMetadataInspector(new OrientVisitorFactory(), entityType);
			analyzer.getGenerators(entityType).forEach((field, generator) -> {
				final String type = generator.generator();
				String tableName = "id_generator";
				String keyColumn = "id";
				String keyValue = camelToSnakeCase(entityMetadata.getName());
				String valueColumn = "value";

				if (TABLE.equals(generator.strategy())) {
					for (TableGenerator table : findAnnotations(entityType, TableGenerator.class)) {
						if (type.equalsIgnoreCase(table.name())) {
							if (!table.table().isEmpty()) {
								tableName = table.table();
							}
							if (!table.pkColumnName().isEmpty()) {
								keyColumn = table.pkColumnName();
							}
							if (!table.pkColumnValue().isEmpty()) {
								keyValue = table.pkColumnValue();
							}
							if (!table.valueColumnName().isEmpty()) {
								valueColumn = table.valueColumnName();
							}
						}
					}
				} else if (SEQUENCE.equals(generator.strategy())) {
					for (SequenceGenerator sequence : findAnnotations(entityType, SequenceGenerator.class)) {
						if (type.equalsIgnoreCase(sequence.name())) {
							if (!sequence.sequenceName().isEmpty()) {
								keyValue = sequence.sequenceName();
							}
						}
					}
				} else if (IDENTITY.equals(generator.strategy())) {
					throw new OrmException("No support available for orient-db identity primary key generation.");
				}

				final String fieldName = field.getName();
				final String indexName = tableName + "." + keyColumn;
				final ColumnMetadata column = entityMetadata.findColumn(fieldName);

				if (column == null) {
					throw new OrmException("Unable to locate primary key [" + fieldName + "] for entity [" + entityMetadata + "].");
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

				final OClass schemaClass = databaseDocument.getMetadata().getSchema().getOrCreateClass(tableName);
				if (!schemaClass.existsProperty(keyColumn)) {
					schemaClass.createProperty(keyColumn, OType.STRING);
				}

				if (!schemaClass.existsProperty(valueColumn)) {
					schemaClass.createProperty(valueColumn, OrientUtils.columnType(column));
				}

				if (!schemaClass.areIndexed(keyColumn)) {
					schemaClass.createIndex(indexName, OClass.INDEX_TYPE.UNIQUE, keyColumn);
				}

				final IdGenerator idGenerator = new OrientIdGenerator(tableName, indexName, keyColumn, valueColumn, keyValue,
						this);
				entityMetadata.setGenerator(column, idGenerator);
				log.info("Set counter id generator for [{}] on entity [{}].", column, entityMetadata);
			});
		});
	}

	private void refreshEntityWithTransaction(EntityMetadata entityMetadata, ODatabaseDocument database) {
		final String keyIndex = OrientUtils.keyIndex(entityMetadata);
		final String schemaName = entityMetadata.getTable();

		if (ddl == dropCreate) {
			if (hasClass(database, schemaName)) {
				database.command(new OCommandSQL("DELETE FROM " + schemaName + " UNSAFE")).execute();
			}

			if (hasIndex(database, keyIndex)) {
				database.command(new OCommandSQL("DROP INDEX " + keyIndex)).execute();
			}

			for (IndexMetadata indexMetadata : entityMetadata.getIndexMetadataList()) {
				final String indexName;
				if (indexMetadata.getMetadataList().size() == 1) {
					final ColumnMetadata column = indexMetadata.getMetadataList().iterator().next();
					indexName = schemaName + "." + column.getName();
				} else {
					indexName = schemaName + "." + indexMetadata.getName();
				}

				if (hasIndex(database, indexName)) {
					database.command(new OCommandSQL("DROP INDEX " + indexName)).execute();
				}
			}
		}

		final OClass schemaClass = database.getMetadata().getSchema().getOrCreateClass(schemaName);
		final Set<ColumnMetadata> metadataSet = new ArrayListSet<>();
		for (ColumnMetadata columnMetadata : entityMetadata) {
			final String property = columnMetadata.getName();
			final OType type = OrientUtils.columnType(columnMetadata);
			if (type != null && !schemaClass.existsProperty(property)) {
				schemaClass.createProperty(property, type);
			}

			if (columnMetadata.isPrimaryKey()) {
				metadataSet.add(columnMetadata);
			}
		}

		if (!hasIndex(database, keyIndex)) {
			final Collection<String> names = metadataSet.stream()
					.filter(Objects::nonNull)
					.map(ColumnMetadata::getName)
					.collect(Collectors.toList());
			if (!names.isEmpty()) {
				if (log.isDebugEnabled()) {
					log.debug("Adding primary id index [{}] with {}", keyIndex, names);
				}
				database.command(new OCommandSQL("CREATE INDEX " + keyIndex + " ON " + schemaName + " (" + org.apache.commons.lang3.StringUtils.join(names, ",") + ") UNIQUE")).execute();
			}
		}

		entityMetadata.getIndexMetadataList().forEach(index -> {
			final String uniqueness = index.isUnique() ? "UNIQUE" : "NOTUNIQUE";
			final Collection<String> names = index.getMetadataList()
					.stream()
					.map(ColumnMetadata::getName)
					.collect(Collectors.toList());
			final String indexName = schemaName + "." + index.getName();
			if (!names.isEmpty() && !hasIndex(database, indexName)) {
				if (log.isDebugEnabled()) {
					log.debug("Adding property index [{}] with {} ({})", indexName, names, uniqueness);
				}
				database.command(new OCommandSQL("CREATE INDEX " + indexName + " ON " + schemaName + " (" + org.apache.commons.lang3.StringUtils.join(names, ",") + ") " + uniqueness)).execute();
			}
		});
	}

	@Override
	public boolean installQuery(EntityMetadata entityMetadata, String name, String query) {
		if (name == null || name.isEmpty()) {
			return false;
		}

		if (query == null || query.isEmpty()) {
			return false;
		}

		if (queryMap.containsKey(name)) {
			return false;
		}

		final String tableQuery = QueryingUtils.prepareQuery(entityMetadata, query);
		if (tableQuery == null || tableQuery.isEmpty()) {
			return false;
		}

		final OrientQuery prepared = new OrientQuery(query, tableQuery);
		queryMap.put(name, prepared);
		return true;
	}

	@Override
	public boolean removeQuery(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}

		return queryMap.remove(name) != null;
	}

	@Override
	public void closeSession() {
		pool.close();
	}
}
