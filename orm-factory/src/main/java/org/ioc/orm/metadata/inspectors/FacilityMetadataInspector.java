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
package org.ioc.orm.metadata.inspectors;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.metadata.type.*;
import org.ioc.orm.metadata.visitors.column.ColumnVisitor;
import org.ioc.orm.metadata.visitors.column.ColumnVisitorFactory;
import org.ioc.orm.metadata.visitors.column.type.*;
import org.ioc.utils.Assertion;
import org.ioc.utils.ReflectionUtils;
import org.ioc.utils.collections.ArrayListSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static javax.persistence.DiscriminatorType.CHAR;
import static javax.persistence.DiscriminatorType.INTEGER;
import static javax.persistence.FetchType.LAZY;
import static org.ioc.utils.ReflectionUtils.searchAnnotations;
import static org.ioc.utils.ReflectionUtils.toClassHierarchy;
import static org.ioc.utils.StringUtils.camelToSnakeCase;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class FacilityMetadataInspector {
	private static final Logger log = LoggerFactory.getLogger(FacilityMetadataInspector.class);

	private final SecureRandom secureRandom = new SecureRandom();
	private final List<Class<?>> classList;
	private final ColumnVisitorFactory columnVisitorFactory;
	private final Map<Class<?>, FacilityMetadata> entityMetadataMap = new LinkedHashMap<>();

	public FacilityMetadataInspector(ColumnVisitorFactory columnVisitorFactory, Class<?> clazz,
									 final Class<?>... list) {
		Assertion.checkNotNull(columnVisitorFactory, "visitor factory");
		Assertion.checkNotNull(clazz, "class");

		this.columnVisitorFactory = columnVisitorFactory;

		classList = new ArrayList<>();
		classList.add(clazz);

		if (list != null && list.length > 0) {
			Arrays.stream(list).filter(Objects::nonNull).forEachOrdered(classList::add);
		}
	}

	public FacilityMetadataInspector(ColumnVisitorFactory columnVisitorFactory, Collection<Class<?>> classList) {
		Assertion.checkNotNull(columnVisitorFactory, "visitor factory");
		Assertion.checkNotNull(classList, "classes");

		this.columnVisitorFactory = columnVisitorFactory;
		this.classList = new ArrayList<>(classList);
	}

	public Set<FacilityMetadata> inspect() {
		final List<Class<?>> simpleTypes = new ArrayList<>();
		final List<Class<?>> inheritedTypes = new ArrayList<>();
		classList.forEach(clazz -> {
			final int num = findHierarchy(clazz).size();
			if (num > 1) {
				inheritedTypes.add(clazz);
			} else if (num == 1) {
				simpleTypes.add(clazz);
			}
		});

		simpleTypes.forEach(entityClass -> {
			final String name = getFacility(entityClass);
			final String table = getTable(entityClass);
			final FacilityMetadata facilityMetadata = new FacilityMetadata(name, table, Collections.singleton(entityClass));
			entityMetadataMap.put(entityClass, facilityMetadata);
			firstVisit(facilityMetadata);
			readIdVisit(facilityMetadata);
		});

		while (!inheritedTypes.isEmpty()) {
			final List<Class<?>> relatedTypes = new ArrayList<>();
			final Class<?> entityClass = inheritedTypes.remove(0);
			final Map<Class<?>, Entity> entityHierarchy = findHierarchy(entityClass);
			relatedTypes.add(entityClass);

			new ArrayList<>(inheritedTypes).forEach(otherType -> {
				final Collection<Class<?>> otherHierarchy = new HashSet<>(findHierarchy(otherType).keySet());
				otherHierarchy.retainAll(entityHierarchy.keySet());
				if (!otherHierarchy.isEmpty() && inheritedTypes.remove(otherType)) {
					relatedTypes.add(otherType);
				}
			});

			final FacilityMetadata facilityMetadata;
			if (relatedTypes.size() > 1) {
				final Set<Class<?>> allTypes = new HashSet<>();
				relatedTypes
						.stream()
						.map(type -> findHierarchy(type).keySet())
						.forEach(allTypes::addAll);

				final Set<Class<?>> commonTypes = new HashSet<>(allTypes);
				relatedTypes
						.stream()
						.map(type -> findHierarchy(type).keySet())
						.forEach(commonTypes::retainAll);

				final Class<?> commonType = commonTypes.isEmpty() ? entityClass : commonTypes.iterator().next();
				final String name = getFacility(commonType);
				final String table = getTable(commonType);
				facilityMetadata = new FacilityMetadata(name, table, relatedTypes);
			} else {
				final Class<?> singleType = relatedTypes.get(0);
				final String name = getFacility(singleType);
				final String table = getTable(singleType);
				facilityMetadata = new FacilityMetadata(name, table, Collections.singleton(singleType));
			}

			relatedTypes.forEach((x) -> entityMetadataMap.put(x, facilityMetadata));

			firstVisit(facilityMetadata);
			readIdVisit(facilityMetadata);
		}

		final Set<FacilityMetadata> set = new TreeSet<>(entityMetadataMap.values());
		set.forEach(this::secondVisit);

		set.forEach(entity -> analyzeIndex(entity).forEach(entity::addIndex));

		classList.forEach(clazz -> {
			final FacilityMetadata facilityMetadata = entityMetadataMap.get(clazz);
			if (facilityMetadata != null) {
				installQueries(clazz).forEach(facilityMetadata::addQuery);
			}
		});

		return Collections.unmodifiableSet(set);
	}

	private Collection<IndexMetadata> analyzeIndex(FacilityMetadata facilityMetadata) {
		final Set<Index> indexSet = new ArrayListSet<>();
		facilityMetadata.getTypes().forEach(entityType -> {
			searchAnnotations(entityType, Table.class)
					.stream()
					.filter(table -> table.indexes().length > 0)
					.map(table -> Arrays.asList(table.indexes()))
					.forEach(indexSet::addAll);

			indexSet.addAll(searchAnnotations(entityType, Index.class));
		});

		final List<IndexMetadata> indices = new ArrayList<>();
		for (Index index : indexSet) {
			final Set<String> columnNames = new ArrayListSet<>(indexSet.size());

			Arrays.stream(index.columnList().split(",|;|\\s+"))
					.filter(trimmed -> !trimmed.isEmpty())
					.forEach(columnNames::add);

			final List<ColumnMetadata> columnMetadataList = columnNames.stream()
					.map(facilityMetadata::findColumnMetadata)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			if (!columnMetadataList.isEmpty()) {
				String defaultName = index.name();
				if (defaultName.isEmpty()) {
					if (columnMetadataList.size() == 1) {
						defaultName = columnMetadataList.get(0).getName();
					} else {
						defaultName = "index_" + new BigInteger(64, secureRandom).toString(32).toLowerCase();
					}
				}
				indices.add(new IndexMetadata(defaultName, columnMetadataList, index.unique()));
			}
		}
		return Collections.unmodifiableCollection(indices);
	}

	private <T> FacilityMetadata findMetadata(Class<T> entityClass) {
		final FacilityMetadata facilityMetadata = entityMetadataMap.get(entityClass);
		if (facilityMetadata != null) {
			return facilityMetadata;
		}

		final Set<FacilityMetadata> facilityMetadataSet = new ArrayListSet<>();
		entityMetadataMap.forEach((clazz, metadata) -> {
			if (entityClass.isAssignableFrom(clazz)) {
				facilityMetadataSet.add(metadata);
			}
		});

		if (facilityMetadataSet.isEmpty()) {
			return null;
		} else if (facilityMetadataSet.size() == 1) {
			return facilityMetadataSet.iterator().next();
		} else {
			throw new OrmException("Multiple entityMetadataMap found for [" + entityClass + "].");
		}
	}

	private Map<Class<?>, Entity> findHierarchy(Class<?> entityClass) {
		final Map<Class<?>, Entity> map = new HashMap<>();
		toClassHierarchy(entityClass)
				.forEach(clazz -> {
					final Entity entity = clazz.getAnnotation(Entity.class);
					if (entity != null) {
						map.put(clazz, entity);
					}
				});
		return map;
	}

	private boolean isEntity(Class<?> entityClass) {
		return findMetadata(entityClass) != null;
	}

	private String getFacility(Class<?> entityClass) {
		final Entity entity = entityClass.getAnnotation(Entity.class);
		if (entity == null) {
			return null;
		}

		final String name = entity.name();
		if (!name.isEmpty()) {
			return name;
		}
		return entityClass.getSimpleName();
	}

	private String getTable(Class<?> clazz) {
		return toClassHierarchy(clazz)
				.stream()
				.map(type -> type.getAnnotation(Table.class))
				.filter(table -> table != null && !table.name().isEmpty())
				.findFirst()
				.map(Table::name)
				.orElseGet(() -> camelToSnakeCase(getFacility(clazz)));
	}

	private Collection<QueryMetadata> installQueries(Class<?> entityClass) {
		final FacilityMetadata facilityMetadata = findMetadata(entityClass);
		if (facilityMetadata != null) {
			final Set<QueryMetadata> queryMetadataSet = new TreeSet<>();
			toClassHierarchy(entityClass)
					.forEach(type -> {
						final List<NamedQuery> namedQueryList = new ArrayList<>();
						final NamedQueries namedQueries = type.getAnnotation(NamedQueries.class);
						if (namedQueries != null) {
							namedQueryList.addAll(Arrays.asList(namedQueries.value()));
						}

						final NamedQuery namedQuery = type.getAnnotation(NamedQuery.class);
						if (namedQuery != null) {
							namedQueryList.add(namedQuery);
						}

						namedQueryList
								.stream()
								.map(annotation -> new QueryMetadata(type, annotation.name(), annotation.query()))
								.forEach(queryMetadataSet::add);
					});

			return Collections.unmodifiableCollection(queryMetadataSet);
		}

		return Collections.emptyList();
	}

	public Map<Field, GeneratedValue> getProducers(Class<?> entityClass) {
		final Map<Field, GeneratedValue> fieldGeneratedValueMap = new LinkedHashMap<>();
		toClassHierarchy(entityClass)
				.stream()
				.flatMap(type -> getFields(type).stream())
				.forEach(field -> {
					final GeneratedValue generatedValue = field.getAnnotation(GeneratedValue.class);
					if (generatedValue != null) {
						fieldGeneratedValueMap.put(field, generatedValue);
					}
				});
		return Collections.unmodifiableMap(fieldGeneratedValueMap);
	}

	private void secondVisit(FacilityMetadata facilityMetadata) {
		if (facilityMetadata != null) {
			readFields(facilityMetadata, supportedAnnotations());
		}
	}

	private void firstVisit(FacilityMetadata facilityMetadata) {
		if (facilityMetadata != null) {
			final List<Class<? extends Annotation>> list = new ArrayList<>(2);
			list.add(EmbeddedId.class);
			list.add(Id.class);
			readFields(facilityMetadata, list);

			facilityMetadata.getTypes().forEach(entityClazz -> {
				final ColumnMetadata columnMetadata = getInheritColumn(entityClazz);
				if (columnMetadata != null) {
					facilityMetadata.addColumnMetadata(columnMetadata);
					final InheritMetadata inheritMetadata = ofDiscriminatorValue(columnMetadata, entityClazz);
					facilityMetadata.setInherited(entityClazz, inheritMetadata);
				}
			});
		}
	}

	private void readFields(FacilityMetadata entity, Collection<Class<? extends Annotation>> annotations) {
		if (entity != null) {
			for (Class<?> entityClazz : entity.getTypes()) {
				for (Class<?> clazz : toClassHierarchy(entityClazz)) {
					for (Field field : ReflectionUtils.getFields(clazz, annotations)) {
						if (inspectField(entity, field)) {
							if (log.isDebugEnabled()) {
								log.debug("Configured metadata for [" + field + "] in [" + entity + "].");
							}
						}
					}
				}
			}
		}
	}

	private void readIdVisit(FacilityMetadata facilityMetadata) {
		for (Class<?> entityClass : facilityMetadata.getTypes()) {
			final List<Field> embeddedKeys = new ArrayList<>();
			final List<Field> regularKeys = new ArrayList<>();
			toClassHierarchy(entityClass)
					.stream()
					.flatMap(type -> getFields(type).stream())
					.forEach(field -> {
						if (field.getAnnotation(EmbeddedId.class) != null) {
							embeddedKeys.add(field);
						} else if (field.getAnnotation(Id.class) != null) {
							regularKeys.add(field);
						}
					});

			if (embeddedKeys.isEmpty() && regularKeys.isEmpty()) {
				return;
			}

			if (!embeddedKeys.isEmpty() && !regularKeys.isEmpty()) {
				throw new OrmException("Cannot specify both @Id or @EmbeddedId annotations for class [" + entityClass + "].");
			}

			if (embeddedKeys.size() > 1 || regularKeys.size() > 1) {
				log.warn("Multiple @Id or @EmbeddedId annotations found for class [" + entityClass + "] - you may be unable to access entityMetadata by #visit api.");
				facilityMetadata.setIdVisitor(NullIdVisitor.getInstance());
				return;
			}

			if (embeddedKeys.size() > 0) {
				final Field key = embeddedKeys.get(0);
				final Map<ColumnMetadata, ColumnVisitor> visitorMap = ofPrimaryKey(key, false);
				facilityMetadata.setIdVisitor(new CompoundIdVisitor(key, visitorMap));
				return;
			} else {
				final Field key = regularKeys.get(0);
				final Collection<ColumnMetadata> keyColumns = ofPrimaryKey(key, false).keySet();
				if (!keyColumns.isEmpty()) {
					facilityMetadata.setIdVisitor(new BaseIdVisitor(key, keyColumns.iterator().next()));
					return;
				}
			}
		}

	}

	private String toSchemaColumnName(Field field) {
		final Column column = field.getAnnotation(Column.class);
		final JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

		String name = camelToSnakeCase(field.getName());
		if (column != null && !column.name().isEmpty()) {
			name = column.name();
		} else if (joinColumn != null && !joinColumn.name().isEmpty()) {
			name = joinColumn.name();
		}
		return name;
	}

	private Map<ColumnMetadata, ColumnVisitor> ofPrimaryKey(Field field, boolean nested) {
		final Class<?> type = field.getType();
		final Id id = field.getAnnotation(Id.class);
		if (id != null) {
			final String name = toSchemaColumnName(field);
			final String property = field.getName();
			final ColumnMetadata columnMetadata = new ColumnMetadata(name, property, type, true,
					false, false);
			final ColumnVisitor visitor = columnVisitorFactory.of(field, type);
			final Map<ColumnMetadata, ColumnVisitor> visitorMap = new HashMap<>(1);
			visitorMap.put(columnMetadata, visitor);
			return Collections.unmodifiableMap(visitorMap);
		}

		final EmbeddedId embeddedId = field.getAnnotation(EmbeddedId.class);
		if (embeddedId != null) {
			final Embeddable embeddable = type.getAnnotation(Embeddable.class);
			if (embeddable == null) {
				throw new OrmException("Class [" + type + "] does not have Embeddable annotation.");
			}

			final Map<ColumnMetadata, ColumnVisitor> visitorMap = new LinkedHashMap<>();
			final List<Field> fields = new FacilityMetadataInspector(columnVisitorFactory, type).getFields(type);
			fields.forEach(f -> {
				final Class<?> embeddedClass = f.getType();
				final String embeddedName = toSchemaColumnName(f);
				final String property = field.getName() + "." + f.getName();
				final ColumnMetadata columnMetadata = new ColumnMetadata(embeddedName, property, embeddedClass,
						true, false, false);
				final ColumnVisitor baseVisitor = columnVisitorFactory.of(f, embeddedClass);
				final ColumnVisitor visitor;
				if (nested) {
					visitor = new NestedColumnVisitor(field, baseVisitor);
				} else {
					visitor = baseVisitor;
				}

				visitorMap.put(columnMetadata, visitor);
			});

			return Collections.unmodifiableMap(visitorMap);
		}

		return Collections.emptyMap();
	}

	private boolean inspectField(FacilityMetadata facilityMetadata, Field field) {
		final String name = toSchemaColumnName(field);
		final Class<?> type = field.getType();

		if (field.isAnnotationPresent(Transient.class)) {
			return false;
		}

		if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
			return facilityMetadata.addColumns(ofPrimaryKey(field, true));
		}

		if (field.isAnnotationPresent(ElementCollection.class)) {
			return facilityMetadata.addColumns(configureElementCollection(field, name, field.getName()));
		}

		if (field.isAnnotationPresent(ManyToMany.class)) {
			log.info("Dont support relation typeof ManyToMany. Coming soon!");
			return false;
		}

		if (field.isAnnotationPresent(OneToOne.class)) {
			return facilityMetadata.addColumns(installOTORelation(field, name, field.getName()));
		} else if (field.isAnnotationPresent(ManyToOne.class)) {
			return facilityMetadata.addColumns(installMTORelation(field, name, field.getName()));
		} else if (field.isAnnotationPresent(OneToMany.class)) {
			return installOTMRelation(facilityMetadata, field, name, field.getName());
		}

		if (field.getAnnotation(Column.class) != null || field.getAnnotation(Embedded.class) != null) {
			if (searchAnnotations(type, Embeddable.class).isEmpty()) {
				final ColumnVisitor visitor = columnVisitorFactory.of(field, type);
				final ColumnMetadata column = new ColumnMetadata(name, field.getName(), type, false, false, false);
				if (facilityMetadata.addColumnMetadata(column)) {
					facilityMetadata.setVisitor(column, visitor);
					return true;
				}
			} else {
				boolean modified = false;
				for (Field embeddedColumn : new FacilityMetadataInspector(columnVisitorFactory, type).getFields(type)) {
					final Class<?> embeddedClass = embeddedColumn.getType();
					final String embeddedName = toSchemaColumnName(embeddedColumn);
					final String property = field.getName() + "." + embeddedColumn.getName();
					final ColumnMetadata columnMetadata = new ColumnMetadata(embeddedName, property, embeddedClass,
							false, false, false);
					final ColumnVisitor baseVisitor = columnVisitorFactory.of(embeddedColumn, embeddedClass);
					final ColumnVisitor visitor = new NestedColumnVisitor(field, baseVisitor);
					if (facilityMetadata.addColumnMetadata(columnMetadata)) {
						modified = true;
						facilityMetadata.setVisitor(columnMetadata, visitor);
					}
				}
				return modified;
			}
		}

		return false;
	}

	private Map<ColumnMetadata, ColumnVisitor> installOTORelation(Field field, String name, String property) {
		final OneToOne oneToOne = field.getAnnotation(OneToOne.class);
		final boolean isLazyLoading = oneToOne.fetch() == LAZY;
		final String mappedBy = oneToOne.mappedBy();
		if (mappedBy.isEmpty()) {
			return getColumnMetadataColumnVisitorMap(field, name, property, isLazyLoading);
		} else {
			return Collections.emptyMap();
		}
	}

	private Map<ColumnMetadata, ColumnVisitor> installMTORelation(Field field, String name, String property) {
		final ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
		final boolean isLazyLoading = manyToOne.fetch() == LAZY;
		return getColumnMetadataColumnVisitorMap(field, name, property, isLazyLoading);
	}

	private boolean installOTMRelation(FacilityMetadata parentEntity, Field field, String name, String property) {
		final OneToMany oneToMany = field.getAnnotation(OneToMany.class);
		final boolean isLazyLoading = oneToMany.fetch() == LAZY;
		final Class<?> parameterizedClass;
		if (oneToMany.targetEntity() != void.class) {
			parameterizedClass = oneToMany.targetEntity();
		} else {
			final ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
			final Type[] types = parameterizedType != null ? parameterizedType.getActualTypeArguments() : null;
			parameterizedClass = types != null && types.length > 0 ? (Class<?>) types[0] : null;
		}

		if (parameterizedClass == null) {
			return false;
		}

		final FacilityMetadata relationEntity = findMetadata(parameterizedClass);
		if (relationEntity == null) {
			throw new OrmException("Type [" + parameterizedClass + "] is not a registered entityMetadata.");
		}

		final JoinTable joinTable = field.getAnnotation(JoinTable.class);
		if (joinTable != null) {
			throw new OrmException("Unable to add column [" + field + "] - join table no supported.");
		} else if (!oneToMany.mappedBy().isEmpty()) {
			secondVisit(relationEntity);

			final ColumnMetadata mappedColumn = relationEntity.findColumnMetadata(oneToMany.mappedBy());
			if (mappedColumn == null) {
				throw new OrmException("Unable to locate column [" + oneToMany.mappedBy() + "] within [" + relationEntity + "].");
			}

			final ColumnVisitor visitor = columnVisitorFactory.manyVisit(field, relationEntity, isLazyLoading);
			final ColumnMetadata mappedColumnMetadata = new MappedColumnMetadata(relationEntity, mappedColumn, name, property, field.getType(), isLazyLoading);
			if (parentEntity.addColumnMetadata(mappedColumnMetadata)) {
				parentEntity.setVisitor(mappedColumnMetadata, visitor);
				relationEntity.addIndex(new IndexMetadata(mappedColumn.getName(), Collections.singleton(mappedColumn), false));
				return true;
			}
		} else {
			final ColumnVisitor visitor = columnVisitorFactory.manyVisit(field, relationEntity, isLazyLoading);
			final ColumnMetadata column = new JoinBagMetadata(name, property, field.getType(), relationEntity, false, isLazyLoading, true);
			if (parentEntity.addColumnMetadata(column)) {
				parentEntity.setVisitor(column, visitor);
				return true;
			}
		}

		return false;
	}

	private Map<ColumnMetadata, ColumnVisitor> getColumnMetadataColumnVisitorMap(Field field, String name, String property, boolean isLazyLoading) {
		final Class<?> type = field.getType();
		final FacilityMetadata facilityMetadata = findMetadata(type);
		if (facilityMetadata == null) {
			throw new OrmException("Type [" + type + "] is not a registered facilityMetadata.");
		}

		final ColumnVisitor visitor = columnVisitorFactory.singleVisit(field, facilityMetadata, isLazyLoading);
		final ColumnMetadata primary = facilityMetadata.getPrimaryKeys().iterator().next();
		final ColumnMetadata columnMetadata = new JoinColumnMetadata(name, property, primary.getType(), facilityMetadata, false);
		final Map<ColumnMetadata, ColumnVisitor> map = new HashMap<>(1);
		map.put(columnMetadata, visitor);
		return Collections.unmodifiableMap(map);
	}

	private Map<ColumnMetadata, ColumnVisitor> configureElementCollection(Field field, String name, String property) {
		final ElementCollection elementCollection = field.getAnnotation(ElementCollection.class);
		final boolean isLazyLoading = elementCollection != null && elementCollection.fetch() == LAZY;
		final Class<?> type = field.getType();
		final ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
		final Type[] types = parameterizedType.getActualTypeArguments();
		final Class<?> parameterizedClass = types != null && types.length > 0 ? (Class<?>) types[0] : null;
		if (parameterizedClass == null) {
			return Collections.emptyMap();
		}

		if (isEntity(parameterizedClass)) {
			return Collections.emptyMap();
		}

		final ColumnVisitor visitor;
		if (Set.class.isAssignableFrom(type)) {
			visitor = new SetColumnVisitor(field, parameterizedClass, isLazyLoading);
		} else {
			visitor = new ListColumnVisitor(field, parameterizedClass, isLazyLoading);
		}

		final Map<ColumnMetadata, ColumnVisitor> visitorMap = new HashMap<>(1);
		final ColumnMetadata column = new EmbeddedBagMetadata(name, property, type, parameterizedClass,
				false, isLazyLoading, false);
		visitorMap.put(column, visitor);
		return Collections.unmodifiableMap(visitorMap);
	}

	private ColumnMetadata getInheritColumn(Class<?> entityClass) {
		DiscriminatorColumn discriminatorColumn = null;
		for (Class<?> type : toClassHierarchy(entityClass)) {
			discriminatorColumn = type.getAnnotation(DiscriminatorColumn.class);
			if (discriminatorColumn != null) {
				break;
			}
		}

		if (discriminatorColumn != null) {
			final String property = "#DiscriminatorColumn";
			final String name = discriminatorColumn.name();
			final DiscriminatorType discriminatorType = discriminatorColumn.discriminatorType();
			if (discriminatorType == CHAR) {
				return new ColumnMetadata(name, property, Character.class, false, false, false);
			} else if (discriminatorType == INTEGER) {
				return new ColumnMetadata(name, property, Integer.class, false, false, false);
			} else {
				return new ColumnMetadata(name, property, String.class, false, false, false);
			}
		}

		return null;
	}

	private InheritMetadata ofDiscriminatorValue(ColumnMetadata columnMetadata, Class<?> entityClass) {
		final String orElse = toClassHierarchy(entityClass)
				.stream()
				.map(type -> type.getAnnotation(DiscriminatorValue.class))
				.filter(Objects::nonNull)
				.findFirst()
				.map(DiscriminatorValue::value)
				.orElseGet(() -> camelToSnakeCase(entityClass.getSimpleName()));

		if (columnMetadata != null) {
			final Class<?> type = columnMetadata.getType();
			if (type == Character.class) {
				return new InheritMetadata(columnMetadata, orElse.charAt(0));
			} else if (type == Short.class) {
				return new InheritMetadata(columnMetadata, Short.parseShort(orElse));
			} else if (type == Integer.class) {
				return new InheritMetadata(columnMetadata, Integer.parseInt(orElse));
			} else if (type == Long.class) {
				return new InheritMetadata(columnMetadata, Long.parseLong(orElse));
			} else {
				return new InheritMetadata(columnMetadata, orElse);
			}
		}

		throw new OrmException("Discriminator column cannot be null.");
	}

	private List<Field> getFields(Class<?> clazz) {
		return ReflectionUtils.getFields(clazz, supportedAnnotations());
	}

	private Collection<Class<? extends Annotation>> supportedAnnotations() {
		return Arrays.asList(Column.class, Embedded.class, EmbeddedId.class, Id.class, ElementCollection.class, JoinColumn.class, OneToMany.class, ManyToOne.class, ManyToMany.class, OneToOne.class, Transient.class);
	}
}
