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
package org.ioc.orm.factory.orient.session;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.iterator.ORecordIteratorCluster;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLQuery;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.tx.OTransaction;
import org.ioc.orm.cache.FacilityCacheManager;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.factory.DatabaseSessionFactory;
import org.ioc.orm.factory.SchemaQuery;
import org.ioc.orm.factory.facility.FacilityBuilder;
import org.ioc.orm.factory.facility.FacilityMapper;
import org.ioc.orm.factory.orient.query.AutoClosingQuery;
import org.ioc.orm.factory.orient.query.OrientQuery;
import org.ioc.orm.factory.orient.query.OrientSchemaQuery;
import org.ioc.orm.metadata.transaction.AbstractTx;
import org.ioc.orm.metadata.transaction.Tx;
import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.FacilityMetadata;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.container.type.OrientContainerFactory;
import org.ioc.orm.metadata.visitors.handler.type.OrientFacilityAdder;
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
public class OrientDatabaseSessionFactory extends AbstractTx implements DatabaseSessionFactory {
	private static final Logger log = LoggerFactory.getLogger(OrientDatabaseSessionFactory.class);

	private final ODatabaseDocument databaseDocument;
	private final FacilityCacheManager facilityCacheManager;
	private final Map<String, OrientQuery> queryMap;

	private final FacilityMapper facilityMapper = new FacilityMapper(this);

	public OrientDatabaseSessionFactory(ODatabaseDocument databaseDocument, Map<String, OrientQuery> queryMap, FacilityCacheManager facilityCacheManager) {
		this.facilityCacheManager = facilityCacheManager;
		this.databaseDocument = databaseDocument;
		this.queryMap = new LinkedHashMap<>(queryMap);
	}

	@Override
	public void close() {
		databaseDocument.close();
	}

	@Override
	public void clear() {
		databaseDocument.getLocalCache().invalidate();
	}

	@Override
	public boolean pending() {
		if (!databaseDocument.isActiveOnCurrentThread()) {
			synchronized (databaseDocument) {
				databaseDocument.activateOnCurrentThread();
			}
		}

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
	public void save(FacilityMetadata facilityMetadata, Object element) {
		try (Tx tx = openTx()) {
			final OrientFacilityAdder orientEntityAdder = new OrientFacilityAdder(this);
			final Object instance = facilityMapper.save(facilityMetadata, element, orientEntityAdder);
			final Object key = facilityMetadata.getIdVisitor().fromObject(instance);
			if (facilityCacheManager.add(facilityMetadata, key, instance)) {
				tx.success();
			}
		} catch (Exception e) {
			throw new OrmException("Unable to create/save new document.", e);
		}
	}

	@Override
	public void delete(FacilityMetadata facilityMetadata, Object element) throws OrmException {
		Assertion.checkNotNull(facilityMetadata);

		try (Tx tx = openTx()) {
			final Map<ColumnMetadata, Object> metadataObjectMap = new LinkedHashMap<>();
			for (ColumnMetadata columnMetadata : facilityMetadata.getPrimaryKeys()) {
				final ColumnVisitor accessor = facilityMetadata.getVisitor(columnMetadata);
				final Object o = accessor != null ? accessor.getValue(element, this) : null;
				if (o != null) {
					metadataObjectMap.put(columnMetadata, o);
				}
			}

			final OIdentifiable identifiable = findIdentifyByMap(facilityMetadata, metadataObjectMap);
			if (identifiable != null) {
				databaseDocument.delete(identifiable.getIdentity());
			}

			final Object key = facilityMetadata.getIdVisitor().fromObject(element);
			facilityCacheManager.delete(facilityMetadata, key);
			tx.success();
		} catch (Exception e) {
			throw new OrmException("Unable to delete database document.", e);
		}
	}

	@Override
	public SchemaQuery query(FacilityMetadata facilityMetadata, String query, Map<String, Object> params) throws OrmException {
		final OrientQuery orientQuery = queryMap.get(query);
		if (orientQuery != null) {
			return ofNamedQuery(facilityMetadata, orientQuery, params);
		} else {
			return ofNullQuery(facilityMetadata, query, params);
		}
	}

	@Override
	public boolean exists(FacilityMetadata facilityMetadata, Object key) throws OrmException {
		Assertion.checkNotNull(facilityMetadata);

		if (key == null) {
			return false;
		}

		try {
			return findIdentifyByKey(facilityMetadata, key) != null;
		} catch (Exception e) {
			throw new OrmException("Unable to query document.", e);
		}
	}

	@Override
	public Object fetch(FacilityMetadata facilityMetadata, Object key) throws OrmException {
		Assertion.checkNotNull(facilityMetadata);

		if (key == null) {
			return null;
		}

		final Object existing = facilityCacheManager.get(facilityMetadata, key, Object.class);
		if (existing != null) {
			return existing;
		}

		try {
			final OIdentifiable rid = findIdentifyByKey(facilityMetadata, key);
			final ODocument document = findDocument(rid);
			if (document != null) {
				final Map<ColumnMetadata, Object> data = new LinkedHashMap<>(convertValues(facilityMetadata, document));
				if (facilityMetadata.validate(data)) {
					final FacilityBuilder builder = new FacilityBuilder(this, new OrientContainerFactory(this));
					final Object instance = builder.build(facilityMetadata, data);
					facilityCacheManager.add(facilityMetadata, key, instance);
					return instance;
				}
			}
		} catch (Exception e) {
			throw new OrmException("Unable to fetch document by key [" + key + "].", e);
		}

		return null;
	}

	@Override
	public List<Object> fetch(FacilityMetadata facilityMetadata, Object... keys) throws OrmException {
		Assertion.checkNotNull(facilityMetadata);

		if (keys == null || keys.length <= 0) {
			return Collections.emptyList();
		}

		final Set<Object> hashSet = new HashSet<>(Arrays.asList(keys));
		final Map<Object, Object> cached = facilityCacheManager.get(facilityMetadata, Arrays.asList(keys), Object.class);
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
		for (OIdentifiable rid : findIdentifyByKeys(facilityMetadata, hashSet)) {
			final ODocument document = findDocument(rid);
			if (document != null) {
				final Map<ColumnMetadata, Object> data = convertValues(facilityMetadata, document);
				final Object key = facilityMetadata.getIdVisitor().ofKey(data);
				if (key != null) {
					final Map<ColumnMetadata, Object> objectMap = entityData.computeIfAbsent(key, k -> new LinkedHashMap<>());
					objectMap.putAll(data);
				}
			}
		}

		final List<Object> result = new ArrayList<>(cachedElements);
		entityData.forEach((o, data) -> {
			if (facilityMetadata.validate(data)) {
				final FacilityBuilder builder = new FacilityBuilder(this, new OrientContainerFactory(this));
				final Object instance = builder.build(facilityMetadata, data);
				if (instance != null) {
					final Object key = facilityMetadata.getIdVisitor().fromObject(data);
					facilityCacheManager.add(facilityMetadata, key, instance);
					result.add(instance);
				}
			}
		});
		return Collections.unmodifiableList(result);
	}

	@Override
	public List<Object> fetchAll(FacilityMetadata facilityMetadata) throws OrmException {
		Assertion.checkNotNull(facilityMetadata);

		final List<Object> objects = new ArrayList<>();
		final ORecordIteratorClass<ODocument> oRecords = databaseDocument.browseClass(facilityMetadata.getTable());
		while (oRecords.hasNext()) {
			final ODocument record = oRecords.next();
			if (record != null) {
				final Map<ColumnMetadata, Object> data = new LinkedHashMap<>(convertValues(facilityMetadata, record));

				if (facilityMetadata.validate(data)) {
					final Object key = facilityMetadata.getIdVisitor().ofKey(data);
					final Object cached = facilityCacheManager.get(facilityMetadata, key, Object.class);
					if (cached != null) {
						objects.add(cached);
					} else {
						final FacilityBuilder builder = new FacilityBuilder(this, new OrientContainerFactory(this));
						final Object instance = builder.build(facilityMetadata, data);
						if (instance != null) {
							facilityCacheManager.add(facilityMetadata, key, instance);
							objects.add(instance);
						}
					}
				}
			}
		}

		return Collections.unmodifiableList(objects);
	}

	/**
	 * Function for execute custom named entity queries.
	 *
	 * @param facilityMetadata queering entity meta data
	 * @param orientQuery      query to execute
	 * @param params           query arguments
	 * @return result of executing
	 */
	private SchemaQuery ofNamedQuery(FacilityMetadata facilityMetadata, OrientQuery orientQuery, Map<String, Object> params) {
		final AutoClosingQuery q = new AutoClosingQuery(databaseDocument, orientQuery.getQuery(), params);
		return new OrientSchemaQuery(this, facilityMetadata, q);
	}

	/**
	 * Function for execute custom another entity queries.
	 *
	 * @param facilityMetadata queering entity meta data
	 * @param query            query to execute
	 * @param params           query arguments
	 * @return result of executing
	 */
	private SchemaQuery ofNullQuery(FacilityMetadata facilityMetadata, String query, Map<String, Object> params) {
		final AutoClosingQuery closingQuery = new AutoClosingQuery(databaseDocument, query, params);
		return new OrientSchemaQuery(this, facilityMetadata, closingQuery);
	}

	/**
	 * @return database schema
	 */
	public final ODatabaseDocument getDocument() {
		return databaseDocument;
	}

	/**
	 * Find entity schema by primary key.
	 *
	 * @param item primary key entity
	 * @return instance schema
	 */
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

	/**
	 * @param type
	 * @return
	 */
	private ORecordIteratorCluster<ORecord> findAllDocuments(String type) {
		return databaseDocument.browseCluster(type);
	}

	public OIdentifiable findIdentifyByMap(FacilityMetadata facilityMetadata, Map<ColumnMetadata, Object> keys) {
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
			final String indexName = keyIndex(facilityMetadata);
			final String schemaName = facilityMetadata.getTable();
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
				.append("SELECT FROM INDEX:").append(keyIndex(facilityMetadata)).append(" ")
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

	private Collection<OIdentifiable> findIdentifyByKeys(FacilityMetadata facilityMetadata, Set<Object> keys) {
		if (facilityMetadata == null || keys == null || keys.isEmpty()) {
			return Collections.emptyList();
		}

		final Collection<OIdentifiable> docs = keys
				.stream()
				.map(key -> findIdentifyByKey(facilityMetadata, key))
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(() -> new ArrayList<>(keys.size())));
		return Collections.unmodifiableCollection(docs);
	}

	public final OIdentifiable findIdentifyByKey(FacilityMetadata facilityMetadata, Object key) {
		if (facilityMetadata == null || key == null) {
			return null;
		}

		if (key instanceof OIdentifiable) {
			return fixIdentifyRecord((OIdentifiable) key);
		}

		final OIdentifiable identifiable = findIdentifyByMap(facilityMetadata, facilityMetadata.getIdVisitor().fromKey(key));
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
	public final <T> T install(FacilityMetadata facilityMetadata, ODocument document) throws OrmException {
		if (document == null) {
			return null;
		}

		final Map<ColumnMetadata, Object> objectMap = convertValues(facilityMetadata, document);
		if (objectMap == null || objectMap.isEmpty()) {
			return null;
		}

		final OrientContainerFactory containerFactory = new OrientContainerFactory(this);
		final T element = (T) new FacilityBuilder(this, containerFactory).build(facilityMetadata, objectMap);
		if (element == null) {
			return null;
		}

		final Object key = facilityMetadata.getIdVisitor().fromObject(element);
		facilityCacheManager.add(facilityMetadata, key, element);
		return element;
	}
}
