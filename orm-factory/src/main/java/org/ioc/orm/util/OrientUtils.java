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
package org.ioc.orm.util;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ORecordLazyList;
import com.orientechnologies.orient.core.db.record.ORecordLazySet;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.type.*;
import org.ioc.utils.collections.ArrayListSet;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class OrientUtils {
	public static String toUrl(String url, String database) {
		if (null == url) {
			throw new OrmException();
		}

		final String s = url.toLowerCase();
		if (s.startsWith("plocal:") || s.startsWith("embedded:") || s.startsWith("memory:")) {
			return url;
		} else {
			return url + "/" + database;
		}
	}

	public static Object convertValue(ODocument document, ColumnMetadata columnMetadata) {
		if (null == document || null == columnMetadata) {
			return null;
		}

		final String name = columnMetadata.getName();
		final Object raw = document.field(name);
		if (null == raw) {
			return null;
		}

		return OrientUtils.convertRaw(columnMetadata, raw);
	}

	public static Object convertKey(FacilityMetadata facilityMetadata, ODocument document) {
		if (facilityMetadata == null || document == null) {
			return null;
		}

		final Map<ColumnMetadata, Object> data = new LinkedHashMap<>();
		facilityMetadata.getPrimaryKeys().forEach(column -> {
			final Object value = convertValue(document, column);
			if (value != null) {
				data.put(column, value);
			}
		});
		return facilityMetadata.getIdVisitor().ofKey(data);
	}

	public static Map<ColumnMetadata, Object> convertValues(FacilityMetadata facilityMetadata, ODocument document) {
		if (facilityMetadata == null || document == null) {
			return Collections.emptyMap();
		}

		final Map<ColumnMetadata, Object> data = new LinkedHashMap<>();
		facilityMetadata.getColumnMetadataCollection().forEach(column -> {
			final String name = column.getName();
			if (document.containsField(name)) {
				final Object raw = document.field(name);
				final Object value = raw != null ? OrientUtils.convertRaw(column, raw) : null;
				data.put(column, value);
			}
		});
		return Collections.unmodifiableMap(data);
	}

	@SuppressWarnings("unchecked")
	private static Object convertRaw(ColumnMetadata columnMetadata, Object value) {
		if (columnMetadata == null || value == null) {
			return null;
		}

		if (value instanceof ORecordLazySet) {
			return value;
		}

		if (value instanceof ORecordLazyList) {
			return value;
		}

		final Class<?> clazz = columnMetadata.getType();

		if (columnMetadata.isBag() && value instanceof Collection) {
			final Collection list = (Collection) value;
			final Class<?> generic;

			if (columnMetadata instanceof EmbeddedBagMetadata) {
				generic = ((EmbeddedBagMetadata) columnMetadata).getType();
			} else if (columnMetadata instanceof JoinBagMetadata) {
				generic = ((JoinBagMetadata) columnMetadata).getFacilityMetadata().getPrimaryKey().getType();
			} else {
				generic = columnMetadata.getType();
			}

			final Collection packed;
			if (value instanceof List) {
				packed = new ArrayList<>(list.size());
			} else {
				packed = new ArrayListSet<>(list.size());
			}

			for (Object item : list) {
				final Object pack = convertType(columnMetadata, generic, item);
				if (pack != null) {
					packed.add(pack);
				}
			}
			return packed;
		}

		return convertType(columnMetadata, clazz, value);
	}

	public static Object convertValue(ColumnMetadata columnMetadata, Object value) {
		if (columnMetadata == null || value == null) {
			return null;
		}

		if (value instanceof ORecordLazySet) {
			return value;
		}
		if (value instanceof ORecordLazyList) {
			return value;
		}

		final Class<?> clazz = columnMetadata.getType();

		if (columnMetadata.isBag() && value instanceof Collection) {
			final Collection list = (Collection) value;
			final List<Object> packed = new ArrayList<>(list.size());
			for (Object item : list) {
				final Object pack = packPrimitive(columnMetadata, item);
				packed.add(pack);
			}

			if (List.class.isAssignableFrom(clazz)) {
				return new ArrayList<>(packed);
			} else if (Collection.class.isAssignableFrom(clazz)) {
				return new ArrayListSet<>(packed);
			}
		}

		return packPrimitive(columnMetadata, value);
	}

	@SuppressWarnings("unchecked")
	private static Object convertType(ColumnMetadata columnMetadata, Class<?> clazz, Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof OIdentifiable) {
			return ((OIdentifiable) value).getIdentity();
		}

		if (UUID.class.equals(clazz)) {
			return DataUtils.bytesToUUID((byte[]) value);
		}

		if (Date.class.equals(clazz)) {
			return value;
		}

		if (Enum.class.isAssignableFrom(clazz)) {
			return Enum.valueOf((Class) clazz, value.toString());
		}

		if (columnMetadata.isJsonString()) {
			return DataUtils.jsonToObject(value.toString(), clazz);
		}

		if (String.class.equals(clazz)) {
			return value;
		}

		if (Number.class.isAssignableFrom(clazz)) {
			return value;
		}

		if (byte[].class.equals(clazz)) {
			return value;
		}

		if (Boolean.class.equals(clazz)) {
			return value;
		}

		if (Serializable.class.isAssignableFrom(clazz)) {
			return DataUtils.bytesToObject((Class) clazz, (byte[]) value);
		}

		return value;
	}

	private static Object packPrimitive(ColumnMetadata columnMetadata, Object value) {
		if (value == null) {
			return null;
		}

		if (value instanceof OIdentifiable) {
			return ((OIdentifiable) value).getIdentity();
		}

		if (value instanceof Date) {
			return value;
		}

		if (value instanceof Enum) {
			return ((Enum) value).name();
		}

		if (value instanceof String) {
			return value;
		}

		if (value instanceof Boolean) {
			return value;
		}

		if (value instanceof UUID) {
			return DataUtils.uuidToBytes((UUID) value);
		}

		if (value instanceof Number) {
			final Class<?> clazz = value.getClass();
			final Number number = (Number) value;
			if (Long.class.equals(clazz)) {
				return number.longValue();
			} else if (Integer.class.equals(clazz)) {
				return number.intValue();
			} else if (Short.class.equals(clazz)) {
				return number.shortValue();
			} else if (Float.class.equals(clazz)) {
				return number.floatValue();
			} else if (Double.class.equals(clazz)) {
				return number.doubleValue();
			} else {
				throw new OrmException("Unexpected numeric type [" + value + "].");
			}
		}

		if (byte[].class.equals(value.getClass())) {
			return value;
		}

		if (columnMetadata.isJsonString()) {
			return DataUtils.objectToJson(value);
		}

		if (value instanceof Serializable) {
			return DataUtils.objectToBytes((Serializable) value);
		}

		throw new OrmException("Unexpected value [" + value + "].");
	}

	public static OType columnType(ColumnMetadata columnMetadata) {
		if (columnMetadata == null) {
			return null;
		}

		if (columnMetadata instanceof MappedColumnMetadata) {
			return null;
		}

		final Class<?> clazz = columnMetadata.getType();

		if (columnMetadata instanceof EmbeddedBagMetadata) {
			if (List.class.isAssignableFrom(clazz)) {
				return OType.EMBEDDEDLIST;
			} else {
				return OType.EMBEDDEDSET;
			}
		} else if (columnMetadata instanceof JoinColumnMetadata) {
			return OType.LINK;
		} else if (columnMetadata instanceof JoinBagMetadata) {
			if (columnMetadata.isEmbedded()) {
				if (List.class.isAssignableFrom(clazz)) {
					return OType.LINKLIST;
				} else {
					return OType.LINKSET;
				}
			} else {
				return OType.LINK;
			}
		} else {
			return mapType(columnMetadata, clazz);
		}
	}

	private static OType mapType(ColumnMetadata columnMetadata, Class<?> clazz) {
		if (String.class.equals(clazz) || columnMetadata.isJsonString()) {
			return OType.STRING;
		}

		if (Enum.class.isAssignableFrom(clazz)) {
			return OType.STRING;
		}

		if (long.class.equals(clazz) || Long.class.equals(clazz)) {
			return OType.LONG;
		}

		if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
			return OType.INTEGER;
		}

		if (short.class.equals(clazz) || Short.class.equals(clazz)) {
			return OType.SHORT;
		}

		if (double.class.equals(clazz) || Double.class.equals(clazz)) {
			return OType.DOUBLE;
		}

		if (float.class.equals(clazz) || Float.class.equals(clazz)) {
			return OType.FLOAT;
		}

		if (BigDecimal.class.equals(clazz)) {
			return OType.DECIMAL;
		}

		if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
			return OType.BOOLEAN;
		}

		if (Date.class.equals(clazz)) {
			return OType.DATETIME;
		}
		return OType.BINARY;
	}

	public static String keyIndex(FacilityMetadata facilityMetadata) {
		if (facilityMetadata == null) {
			return null;
		}

		final Collection<ColumnMetadata> keys = facilityMetadata.getPrimaryKeys();
		if (keys.size() == 1) {
			final ColumnMetadata key = keys.iterator().next();
			return facilityMetadata.getTable() + "." + key.getName();
		} else {
			return facilityMetadata.getTable() + ".key";
		}
	}

	public static boolean databaseIsLocal(String url) {
		if (url == null || url.isEmpty()) {
			return false;
		}

		return url.toLowerCase().startsWith("embedded:") ||
				url.toLowerCase().startsWith("plocal:") ||
				url.toLowerCase().startsWith("local:");
	}

	private static Collection<String> getClasses(ODatabaseDocument databaseDocument) {
		final Collection<OClass> types = databaseDocument.getMetadata().getSchema().getClasses();
		if (types == null || types.isEmpty()) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(types.stream().map(OClass::getName).collect(Collectors.toList()));
	}

	private static Collection<String> getClusters(ODatabaseDocument databaseDocument) {
		final Collection<String> names = databaseDocument.getClusterNames();
		if (names == null || names.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableCollection(names);
	}

	private static Collection<String> getIndices(ODatabaseDocument databaseDocument) {
		return Collections.unmodifiableList(databaseDocument
				.getMetadata()
				.getIndexManager()
				.getIndexes()
				.stream()
				.map(OIndex::getName)
				.collect(Collectors.toList()));
	}

	public static boolean hasIndex(ODatabaseDocument database, String clusterName) {
		if (clusterName == null || clusterName.isEmpty()) {
			return false;
		}

		return getIndices(database).stream().anyMatch(clusterName::equalsIgnoreCase);
	}

	public static boolean hasCluster(ODatabaseDocument database, String clusterName) {
		if (clusterName == null || clusterName.isEmpty()) {
			return false;
		}

		return getClusters(database).stream().anyMatch(clusterName::equalsIgnoreCase);
	}

	public static boolean hasClass(ODatabaseDocument database, String className) {
		if (className == null || className.isEmpty()) {
			return false;
		}

		return getClasses(database).stream().anyMatch(className::equalsIgnoreCase);
	}
}
