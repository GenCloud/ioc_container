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
package org.ioc.orm.factory.orient.session;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.iterator.ORecordIteratorCluster;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.tx.OTransaction;
import org.ioc.orm.cache.EntityCache;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.DatabaseSessionFactory;
import org.ioc.orm.factory.EntityBuilder;
import org.ioc.orm.factory.EntityPersistence;
import org.ioc.orm.factory.SchemaQuery;
import org.ioc.orm.factory.orient.query.OrientClosingQuery;
import org.ioc.orm.factory.orient.query.OrientQuery;
import org.ioc.orm.factory.orient.query.OrientSchemaQuery;
import org.ioc.orm.metadata.transaction.AbstractTransactional;
import org.ioc.orm.metadata.transaction.Tx;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.EntityMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.container.type.OrientContainerFactory;
import org.ioc.orm.metadata.visitors.handler.type.OrientEntityAdder;
import org.ioc.utils.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.ioc.orm.util.OrientUtils.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientDatabaseSession extends AbstractTransactional implements DatabaseSessionFactory {
	private static final Logger log = LoggerFactory.getLogger(OrientDatabaseSession.class);
	protected final ODatabaseDocument databaseDocument;
	private final EntityCache entityCache;
	private final Map<String, OrientQuery> queryMap;

	private final EntityPersistence entityPersistence = new EntityPersistence(this);

	public OrientDatabaseSession(ODatabaseDocument databaseDocument, Map<String, OrientQuery> queryMap, EntityCache entityCache) {
		this.entityCache = entityCache;
		this.databaseDocument = databaseDocument;
		this.queryMap = new LinkedHashMap<>(queryMap);
	}

	@Override
	public void close() {
		databaseDocument.close();
	}

	@Override
	public void clear() {
		entityCache.invalidateAll();
		databaseDocument.getLocalCache().invalidate();
	}

	@Override
	public boolean pending() {
		final OTransaction transaction = databaseDocument.getTransaction();
		if (transaction == null) {
			if (log.isDebugEnabled()) {
				log.debug("Unable to locate databaseDocument transaction.");
			}
			return false;
		} else {
			return transaction.isActive();
		}
	}

	@Override
	public void start() {
		if (log.isDebugEnabled()) {
			if (pending()) {
				log.debug("Beginning transaction, but already in pending-work state.");
			}
		}

		databaseDocument.begin();
	}

	@Override
	public void commit() {
		if (log.isDebugEnabled()) {
			if (!pending()) {
				log.debug("Committing transaction, but not in pending-work state.");
			}
		}
		databaseDocument.commit();
	}

	@Override
	public void rollback() {
		if (log.isDebugEnabled()) {
			if (!pending()) {
				log.debug("Rolling back transaction, but not in pending-work state.");
			}
		}
		databaseDocument.rollback();
	}

	@Override
	public void save(EntityMetadata entityMetadata, Object element) {
		try (Tx tx = openTx()) {
			final OrientEntityAdder orientEntityAdder = new OrientEntityAdder(this);
			entityPersistence.save(entityMetadata, element, orientEntityAdder);
			tx.success();
		} catch (Exception e) {
			throw new OrmException("Unable to create/save new orientdb document.", e);
		}
	}

	@Override
	public void delete(EntityMetadata entityMetadata, Object element) throws OrmException {
		Assertion.checkNotNull(entityMetadata);

		try (Tx tx = openTx()) {
			final Map<ColumnMetadata, Object> metadataObjectMap = new LinkedHashMap<>();
			for (ColumnMetadata columnMetadata : entityMetadata.getPrimaryKeys()) {
				final ColumnVisitor accessor = entityMetadata.getVisitor(columnMetadata);
				final Object value = accessor != null ? accessor.getValue(element, this) : null;
				if (value != null) {
					metadataObjectMap.put(columnMetadata, value);
				}
			}

			final OIdentifiable identifiable = findIdentifyByMap(entityMetadata, metadataObjectMap);
			if (identifiable != null) {
				databaseDocument.delete(identifiable.getIdentity());
			}

			tx.success();

			final Object key = entityMetadata.getIdVisitor().fromObject(element);
			entityCache.remove(entityMetadata, key);
		} catch (Exception e) {
			throw new OrmException("Unable to delete orientdb document.", e);
		}
	}

	@Override
	public SchemaQuery query(EntityMetadata meta, String queryOrName, Map<String, Object> params) throws OrmException {
		final OrientQuery query = queryMap.get(queryOrName);
		if (query != null) {
			return ofNamedQuery(meta, query, params);
		} else {
			return ofNullQuery(meta, queryOrName, params);
		}
	}

	private SchemaQuery ofNamedQuery(EntityMetadata meta, OrientQuery query, Map<String, Object> params) {
		final OrientClosingQuery q = new OrientClosingQuery(databaseDocument, query.getQuery(), params);
		return new OrientSchemaQuery(this, meta, q);
	}

	private SchemaQuery ofNullQuery(EntityMetadata meta, String query, Map<String, Object> params) {
		final OrientClosingQuery q = new OrientClosingQuery(databaseDocument, query, params);
		return new OrientSchemaQuery(this, meta, q);
	}

	protected final OrientClosingQuery ofQuery(String query) {
		return new OrientClosingQuery(databaseDocument, query, Collections.emptyList());
	}

	protected final OrientClosingQuery ofQuery(String query, Collection<?> args) {
		return new OrientClosingQuery(databaseDocument, query, args);
	}

	protected final OrientClosingQuery ofQuery(String query, Map<String, Object> args) {
		return new OrientClosingQuery(databaseDocument, query, args);
	}

	@Override
	public boolean exists(EntityMetadata entityMetadata, Object key) throws OrmException {
		Assertion.checkNotNull(entityMetadata);

		if (key == null) {
			return false;
		}

		try {
			return findIdentifyByKey(entityMetadata, key) != null;
		} catch (Exception e) {
			throw new OrmException("Unable to query document.", e);
		}
	}

	@Override
	public Object fetch(EntityMetadata entityMetadata, Object key) throws OrmException {
		Assertion.checkNotNull(entityMetadata);

		if (key == null) {
			return null;
		}

		final Object existing = entityCache.find(entityMetadata, key, Object.class);
		if (existing != null) {
			return existing;
		}

		try {
			final OIdentifiable rid = findIdentifyByKey(entityMetadata, key);
			final ODocument document = findDocument(rid);
			if (document != null) {
				final Map<ColumnMetadata, Object> data = new LinkedHashMap<>(convertValues(entityMetadata, document));
				if (entityMetadata.validate(data)) {
					final EntityBuilder builder = new EntityBuilder(this, new OrientContainerFactory(this));
					final Object instance = builder.build(entityMetadata, data);
					entityCache.put(entityMetadata, key, instance);
					return instance;
				}
			}
		} catch (Exception e) {
			throw new OrmException("Unable to fetch document by key [" + key + "].", e);
		}

		return null;
	}

	@Override
	public List<Object> fetch(EntityMetadata entityMetadata, Object... keys) throws OrmException {
		Assertion.checkNotNull(entityMetadata);

		if (keys == null || keys.length <= 0) {
			return Collections.emptyList();
		}

		final Set<Object> hashSet = new HashSet<>(Arrays.asList(keys));
		final Map<Object, Object> cached = entityCache.find(entityMetadata, Arrays.asList(keys), Object.class);
		final List<Object> cachedElements;
		hashSet.removeAll(cached.keySet());
		try {
			cachedElements = new ArrayList<>(cached.values());

			if (hashSet.isEmpty()) {
				return Collections.unmodifiableList(cachedElements);
			}
		} catch (Exception e) {
			throw new OrmException("Unable to map and convert entities.", e);
		}

		final Map<Object, Map<ColumnMetadata, Object>> entityData = new HashMap<>();
		for (OIdentifiable rid : findIdentifyByKeys(entityMetadata, hashSet)) {
			final ODocument document = findDocument(rid);
			if (document != null) {
				final Map<ColumnMetadata, Object> data = convertValues(entityMetadata, document);
				final Object key = entityMetadata.getIdVisitor().ofKey(data);
				if (key != null) {
					final Map<ColumnMetadata, Object> objectMap = entityData.computeIfAbsent(key, k -> new LinkedHashMap<>());
					objectMap.putAll(data);
				}
			}
		}

		final List<Object> result = new ArrayList<>(cachedElements);
		entityData.forEach((o, data) -> {
			if (entityMetadata.validate(data)) {
				final EntityBuilder builder = new EntityBuilder(this, new OrientContainerFactory(this));
				final Object instance = builder.build(entityMetadata, data);
				if (instance != null) {
					final Object key = entityMetadata.getIdVisitor().fromObject(data);
					entityCache.put(entityMetadata, key, instance);
					result.add(instance);
				}
			}
		});
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<Object> fetchAll(EntityMetadata entityMetadata) throws OrmException {
		Assertion.checkNotNull(entityMetadata);
		final ORecordIteratorCluster<ORecord> allDocuments = findAllDocuments(entityMetadata.getTable());
		final List<Object> objects = new ArrayList<>();
		if (allDocuments != null) {
			for (ORecord record : allDocuments) {
				if (record != null) {
					if (record instanceof ODocument) {
						final Map<ColumnMetadata, Object> data = new LinkedHashMap<>(convertValues(entityMetadata,
								(ODocument) record));

						if (entityMetadata.validate(data)) {
							final Object key = entityMetadata.getIdVisitor().fromObject(data);
							final Object cached = entityCache.find(entityMetadata, key, Object.class);
							if (cached != null) {
								objects.add(cached);
								continue;
							}

							final EntityBuilder builder = new EntityBuilder(this, new OrientContainerFactory(this));
							final Object instance = builder.build(entityMetadata, data);
							if (instance != null) {
								entityCache.put(entityMetadata, key, instance);
								objects.add(instance);
							}
						}
					}
				}
			}
		}

		return Collections.unmodifiableList(objects);
	}

	public final ODatabaseDocument getDocument() {
		return databaseDocument;
	}

	public final ODocument findDocument(OIdentifiable item) {
		if (item == null) {
			return null;
		}

		if (item instanceof ODocument) {
			return (ODocument) item;
		}

		final ORID identity = item.getIdentity();
		if (identity == null) {
			return null;
		}

		return databaseDocument.load(identity);
	}

	public final ORecordIteratorCluster<ORecord> findAllDocuments(String type) {
		return databaseDocument.browseCluster(type);
	}

	public OIdentifiable findIdentifyByMap(EntityMetadata entityMetadata, Map<ColumnMetadata, Object> keys) {
		final List<ColumnMetadata> columnMetadataList = new ArrayList<>();
		keys.keySet().stream()
				.filter(ColumnMetadata::isPrimaryKey)
				.forEach(columnMetadataList::add);

		final List<Object> arguments = columnMetadataList.stream()
				.map(column -> convertValue(column, keys.get(column)))
				.collect(Collectors.toList());

		if (arguments.isEmpty()) {
			return null;
		}

		if (arguments.size() == 1) {
			final String indexName = keyIndex(entityMetadata);
			final String schemaName = entityMetadata.getTable();
			final OIndex keyIdx = databaseDocument.getMetadata().getIndexManager().getClassIndex(schemaName, indexName);
			if (keyIdx != null) {
				final Object packedKey = arguments.iterator().next();
				final Object value = keyIdx.get(packedKey);
				if (value == null) {
					return null;
				} else if (value instanceof OIdentifiable) {
					final OIdentifiable fixed = fixIdentifyRecord((OIdentifiable) value);
					if (fixed != null) {
						return fixed;
					}
				}
			}
		}

		final StringBuilder queryBuilder = new StringBuilder()
				.append("SELECT FROM INDEX:").append(keyIndex(entityMetadata)).append(" ")
				.append("WHERE key");
		queryBuilder.append(" = [");
		for (int i = 0; i < arguments.size(); i++) {
			if (i > 0) {
				queryBuilder.append(",");
			}
			queryBuilder.append("?");
		}

		queryBuilder.append("]");

		final OSQLQuery synchQuery = new OSQLSynchQuery(queryBuilder.toString());
		final Iterable objects = databaseDocument.query(synchQuery, arguments.toArray());
		if (objects == null) {
			return null;
		}

		int numMatching = 0;
		for (Object item : objects) {
			if (item instanceof ODocument) {
				return fixIdentifyRecord((ODocument) item);
			} else if (item instanceof ORID) {
				return databaseDocument.load((ORID) item);
			}

			numMatching++;
		}

		if (numMatching > 0) {
			throw new OrmException("Found matching index keys but unable to install identifiable record.");
		}
		return null;
	}

	private Collection<OIdentifiable> findIdentifyByKeys(EntityMetadata meta, Set<Object> keys) {
		if (meta == null || keys == null || keys.isEmpty()) {
			return Collections.emptyList();
		}

		final Collection<OIdentifiable> docs = keys
				.stream()
				.map(key -> findIdentifyByKey(meta, key))
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> new ArrayList<>(keys.size())));
		return Collections.unmodifiableCollection(docs);
	}

	public final OIdentifiable findIdentifyByKey(EntityMetadata meta, Object key) {
		if (meta == null || key == null) {
			return null;
		}

		if (key instanceof OIdentifiable) {
			return fixIdentifyRecord((OIdentifiable) key);
		}

		final OIdentifiable identifiable = findIdentifyByMap(meta, meta.getIdVisitor().fromKey(key));
		if (identifiable != null) {
			return fixIdentifyRecord(identifiable);
		} else {
			return null;
		}
	}

	private OIdentifiable fixIdentifyRecord(OIdentifiable record) {
		if (record == null) {
			return null;
		}

		if (record instanceof ODocument) {
			final ODocument document = (ODocument) record;
			if (document.fields() <= 1) {
				final Object value = document.fieldValues()[0];
				if (value instanceof OIdentifiable) {
					return findDocument((OIdentifiable) value);
				}
			}

			final Object field = document.field("rid");
			if (field instanceof OIdentifiable) {
				return findDocument((OIdentifiable) field);
			}
		}

		return record;
	}

	@SuppressWarnings("unchecked")
	public final <T> T install(EntityMetadata meta, ODocument document) throws OrmException {
		if (document == null) {
			return null;
		}

		final Map<ColumnMetadata, Object> objectMap = convertValues(meta, document);
		if (objectMap == null || objectMap.isEmpty()) {
			return null;
		}

		final OrientContainerFactory containerFactory = new OrientContainerFactory(this);
		final T element = (T) new EntityBuilder(this, containerFactory).build(meta, objectMap);
		if (element == null) {
			return null;
		}

		final Object key = meta.getIdVisitor().fromObject(element);
		entityCache.put(meta, key, element);
		return element;
	}
}
