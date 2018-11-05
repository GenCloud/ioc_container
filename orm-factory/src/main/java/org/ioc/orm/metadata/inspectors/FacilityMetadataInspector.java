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
import org.ioc.orm.metadata.visitors.column.factory.BaseColumnVisitorFactory;
import org.ioc.orm.metadata.visitors.column.type.*;
import org.ioc.utils.Assertion;
import org.ioc.utils.ReflectionUtils;
import org.ioc.utils.StringUtils;
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
import static org.ioc.utils.ReflectionUtils.searchAnnotations;
import static org.ioc.utils.ReflectionUtils.toClassHierarchy;

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

	private FacilityMetadataInspector(Collection<Class<?>> classList) {
		this(new BaseColumnVisitorFactory(), classList);
	}

	public Set<FacilityMetadata> analyze() {
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
			final String name = getEntity(entityClass);
			final String table = getTable(entityClass);
			final FacilityMetadata facilityMetadata = new FacilityMetadata(name, table, Collections.singleton(entityClass));
			entityMetadataMap.put(entityClass, facilityMetadata);
			readFirstPass(facilityMetadata);
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
				final String name = getEntity(commonType);
				final String table = getTable(commonType);
				facilityMetadata = new FacilityMetadata(name, table, relatedTypes);
			} else {
				final Class<?> singleType = relatedTypes.get(0);
				final String name = getEntity(singleType);
				final String table = getTable(singleType);
				facilityMetadata = new FacilityMetadata(name, table, Collections.singleton(singleType));
			}

			relatedTypes.forEach((x) -> entityMetadataMap.put(x, facilityMetadata));

			readFirstPass(facilityMetadata);
			readIdVisit(facilityMetadata);
		}

		final Set<FacilityMetadata> set = new TreeSet<>(entityMetadataMap.values());
		set.forEach(this::readSecondPass);

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
		final Set<Index> annotations = new ArrayListSet<>();
		facilityMetadata.getTypes().forEach(entityType -> {
			searchAnnotations(entityType, Table.class)
					.stream()
					.filter(table -> table.indexes().length > 0)
					.map(table -> Arrays.asList(table.indexes()))
					.forEach(annotations::addAll);

			annotations.addAll(searchAnnotations(entityType, Index.class));
		});

		final List<IndexMetadata> indices = new ArrayList<>();
		for (Index annotation : annotations) {
			final Set<String> columnNames = new ArrayListSet<>(annotations.size());

			Arrays.stream(annotation.columnList().split(",|;|\\s+"))
					.filter(trimmed -> !trimmed.isEmpty())
					.forEach(columnNames::add);

			final List<ColumnMetadata> columns = columnNames.stream()
					.map(facilityMetadata::findColumnMetadata)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			if (!columns.isEmpty()) {
				String defaultName = annotation.name();
				if (defaultName.isEmpty()) {
					if (columns.size() == 1) {
						defaultName = columns.get(0).getName();
					} else {
						defaultName = "index_" + new BigInteger(64, secureRandom).toString(32).toLowerCase();
					}
				}
				indices.add(new IndexMetadata(defaultName, columns, annotation.unique()));
			}
		}
		return Collections.unmodifiableCollection(indices);
	}

	private <T> FacilityMetadata findMetadata(Class<T> entityClass) {
		final FacilityMetadata facilityMetadata = entityMetadataMap.get(entityClass);
		if (facilityMetadata != null) {
			return facilityMetadata;
		}

		final Set<FacilityMetadata> metadataSet = new ArrayListSet<>();
		entityMetadataMap.forEach((clazz, value) -> {
			if (entityClass.isAssignableFrom(clazz)) {
				metadataSet.add(value);
			}
		});

		if (metadataSet.isEmpty()) {
			return null;
		} else if (metadataSet.size() == 1) {
			return metadataSet.iterator().next();
		} else {
			throw new OrmException("Multiple entityMetadataMap found for [" + entityClass + "].");
		}
	}

	private Map<Class<?>, Entity> findHierarchy(Class<?> entityClass) {
		final Map<Class<?>, Entity> map = new HashMap<>();
		toClassHierarchy(entityClass)
				.forEach(clazz -> {
					final Entity annotation = clazz.getAnnotation(Entity.class);
					if (annotation != null) {
						map.put(clazz, annotation);
					}
				});
		return map;
	}

	private boolean isEntity(Class<?> entityClass) {
		return findMetadata(entityClass) != null;
	}

	private String getEntity(Class<?> entityClass) {
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
				.map(hierarchyClass -> hierarchyClass.getAnnotation(Table.class))
				.filter(table -> table != null && !table.name().isEmpty())
				.findFirst()
				.map(Table::name)
				.orElse(StringUtils.camelToSnakeCase(getEntity(clazz)));
	}

	private Collection<QueryMetadata> installQueries(Class<?> entityClass) {
		final FacilityMetadata facilityMetadata = findMetadata(entityClass);
		if (facilityMetadata == null) {
			return Collections.emptyList();
		}

		final Set<QueryMetadata> result = new TreeSet<>();
		toClassHierarchy(entityClass)
				.forEach(clazz -> {
					final List<NamedQuery> annotations = new ArrayList<>();
					final NamedQueries queries = clazz.getAnnotation(NamedQueries.class);
					if (queries != null) {
						annotations.addAll(Arrays.asList(queries.value()));
					}

					final NamedQuery query = clazz.getAnnotation(NamedQuery.class);
					if (query != null) {
						annotations.add(query);
					}

					annotations
							.stream()
							.map(annotation -> new QueryMetadata(clazz, annotation.name(), annotation.query()))
							.forEach(result::add);
				});
		return Collections.unmodifiableCollection(result);
	}

	public Map<Field, GeneratedValue> getGenerators(Class<?> entityClass) {
		final Map<Field, GeneratedValue> map = new LinkedHashMap<>();
		toClassHierarchy(entityClass)
				.stream()
				.flatMap(clazz -> getFields(clazz).stream())
				.forEach(field -> {
					final GeneratedValue annotation = field.getAnnotation(GeneratedValue.class);
					if (annotation != null) {
						map.put(field, annotation);
					}
				});
		return Collections.unmodifiableMap(map);
	}

	private boolean readSecondPass(FacilityMetadata entity) {
		if (entity == null) {
			return false;
		}

		return readFields(entity, jpaAnnotations()) > 0;
	}

	private boolean readFirstPass(FacilityMetadata entity) {
		if (entity == null) {
			return false;
		}

		final List<Class<? extends Annotation>> list = new ArrayList<>(2);
		list.add(EmbeddedId.class);
		list.add(Id.class);
		readFields(entity, list);

		configurePostLoad(entity);

		entity.getTypes().forEach(entityClazz -> {
			final ColumnMetadata columnMetadata = getInheritColumn(entityClazz);
			if (columnMetadata != null) {
				entity.addColumnMetadata(columnMetadata);
				final InheritMetadata inheritMetadata = getDiscriminatorValue(columnMetadata, entityClazz);
				entity.setInherited(entityClazz, inheritMetadata);
			}
		});
		return true;
	}

	private void configurePostLoad(FacilityMetadata entity) {
		if (entity == null) {
			return;
		}

		entity.getTypes()
				.stream()
				.flatMap(entityClazz ->
						Arrays.stream(entityClazz.getDeclaredMethods()))
				.filter(method -> method.isAnnotationPresent(PostLoad.class))
				.filter(method -> entity.getPostLoadMethod() == null)
				.forEach(method -> {
					if (log.isDebugEnabled()) {
						log.debug("Configuring PostLoad for [" + method.getName() + "] in [" + entity + "].");
					}
					entity.setPostLoadMethod(method);
				});
	}

	private int readFields(FacilityMetadata entity, Collection<Class<? extends Annotation>> annotations) {
		if (entity == null) {
			return 0;
		}

		int num = 0;
		for (Class<?> entityClazz : entity.getTypes()) {
			for (Class<?> clazz : toClassHierarchy(entityClazz)) {
				for (Field field : ReflectionUtils.getFields(clazz, annotations)) {
					if (configureField(entity, field)) {
						if (log.isDebugEnabled()) {
							log.debug("Configured metadata for [" + field + "] in [" + entity + "].");
						}
						num++;
					}
				}
			}
		}
		return num;
	}

	private void readIdVisit(FacilityMetadata entity) {
		for (Class<?> entityClass : entity.getTypes()) {
			final List<Field> embeddedKeys = new ArrayList<>();
			final List<Field> regularKeys = new ArrayList<>();
			toClassHierarchy(entityClass)
					.stream()
					.flatMap(clazz -> getFields(clazz).stream())
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
				entity.setIdVisitor(NullIdVisitor.getInstance());
				return;
			}

			if (embeddedKeys.size() > 0) {
				final Field key = embeddedKeys.get(0);
				final Map<ColumnMetadata, ColumnVisitor> map = configureId(key, false);
				entity.setIdVisitor(new CompoundIdVisitor(key, map));
				return;
			} else {
				final Field key = regularKeys.get(0);
				final Collection<ColumnMetadata> keyColumns = configureId(key, false).keySet();
				if (!keyColumns.isEmpty()) {
					entity.setIdVisitor(new BaseIdVisitor(key, keyColumns.iterator().next()));
					return;
				}
			}
		}

	}

	private String getColumnName(Field field) {
		final Column column = field.getAnnotation(Column.class);
		final JoinColumn join = field.getAnnotation(JoinColumn.class);

		String name = StringUtils.camelToSnakeCase(field.getName());
		if (column != null && !column.name().isEmpty()) {
			name = column.name();
		} else if (join != null && !join.name().isEmpty()) {
			name = join.name();
		}
		return name;
	}

	private Map<ColumnMetadata, ColumnVisitor> configureId(Field field, boolean useNested) {
		final Class<?> type = field.getType();
		final Id id = field.getAnnotation(Id.class);
		if (id != null) {
			final String name = getColumnName(field);
			final String property = field.getName();
			final ColumnMetadata columnMetadata = new ColumnMetadata(name, property, type, true,
					false, false);
			final ColumnVisitor visitor = columnVisitorFactory.of(field, type);
			final Map<ColumnMetadata, ColumnVisitor> map = new HashMap<>(1);
			map.put(columnMetadata, visitor);
			return Collections.unmodifiableMap(map);
		}

		final EmbeddedId embeddedId = field.getAnnotation(EmbeddedId.class);
		if (embeddedId != null) {
			final Embeddable embeddable = type.getAnnotation(Embeddable.class);
			if (embeddable == null) {
				throw new OrmException("Class [" + type + "] does not have Embeddable annotation.");
			}

			final Map<ColumnMetadata, ColumnVisitor> visitorMap = new LinkedHashMap<>();
			for (Field field1 : new FacilityMetadataInspector(columnVisitorFactory, type).getFields(type)) {
				final Class<?> embeddedClass = field1.getType();
				final String embeddedName = getColumnName(field1);
				final String property = field.getName() + "." + field1.getName();
				final ColumnMetadata columnMetadata = new ColumnMetadata(embeddedName, property, embeddedClass,
						true, false, false);
				final ColumnVisitor baseVisitor = columnVisitorFactory.of(field1, embeddedClass);
				final ColumnVisitor visitor;
				if (useNested) {
					visitor = new NestedColumnVisitor(field, baseVisitor);
				} else {
					visitor = baseVisitor;
				}

				visitorMap.put(columnMetadata, visitor);
			}
			return Collections.unmodifiableMap(visitorMap);
		}

		return Collections.emptyMap();
	}

	private boolean configureField(FacilityMetadata entity, Field field) {
		final String name = getColumnName(field);
		final Class<?> type = field.getType();

		if (field.isAnnotationPresent(Transient.class)) {
			return false;
		}

		if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
			return entity.addColumns(configureId(field, true));
		}

		if (field.isAnnotationPresent(ElementCollection.class)) {
			return entity.addColumns(configureElementCollection(field, name, field.getName()));
		}

		if (field.isAnnotationPresent(ManyToMany.class)) {
			log.info("Dont support relation typeof ManyToMany. Coming soon!");
			return false;
		}

		if (field.isAnnotationPresent(OneToOne.class)) {
			return entity.addColumns(configureOneToOne(field, name, field.getName()));
		} else if (field.isAnnotationPresent(ManyToOne.class)) {
			return entity.addColumns(configureManyToOne(field, name, field.getName()));
		} else if (field.isAnnotationPresent(OneToMany.class)) {
			return configureOneToMany(entity, field, name, field.getName());
		}

		if (field.getAnnotation(Column.class) != null || field.getAnnotation(Embedded.class) != null) {
			if (!type.isAnnotationPresent(Embeddable.class)) {
				final ColumnVisitor visitor = columnVisitorFactory.of(field, type);
				final ColumnMetadata column = new ColumnMetadata(name, field.getName(), type, false, false, false);
				if (entity.addColumnMetadata(column)) {
					entity.setVisitor(column, visitor);
					return true;
				}
			} else {
				boolean modified = false;
				for (Field embeddedColumn : new FacilityMetadataInspector(columnVisitorFactory, type).getFields(type)) {
					final Class<?> embeddedClass = embeddedColumn.getType();
					final String embeddedName = getColumnName(embeddedColumn);
					final String property = field.getName() + "." + embeddedColumn.getName();
					final ColumnMetadata columnMetadata = new ColumnMetadata(embeddedName, property, embeddedClass,
							false, false, false);
					final ColumnVisitor baseVisitor = columnVisitorFactory.of(embeddedColumn, embeddedClass);
					final ColumnVisitor visitor = new NestedColumnVisitor(field, baseVisitor);
					if (entity.addColumnMetadata(columnMetadata)) {
						modified = true;
						entity.setVisitor(columnMetadata, visitor);
					}
				}
				return modified;
			}
		}

		return false;
	}

	private Map<ColumnMetadata, ColumnVisitor> configureOneToOne(Field field, String name, String property) {
		final OneToOne oneToOne = field.getAnnotation(OneToOne.class);
		final boolean isLazyLoading = FetchType.LAZY.equals(oneToOne.fetch());
		final String mappedBy = oneToOne.mappedBy();
		if (mappedBy.isEmpty()) {
			return getColumnMetadataColumnVisitorMap(field, name, property, isLazyLoading);
		} else {
			return Collections.emptyMap();
		}
	}

	private Map<ColumnMetadata, ColumnVisitor> configureManyToOne(Field field, String name, String property) {
		final ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
		final boolean isLazyLoading = FetchType.LAZY.equals(manyToOne.fetch());
		return getColumnMetadataColumnVisitorMap(field, name, property, isLazyLoading);
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

	private boolean configureOneToMany(FacilityMetadata parentEntity, Field field, String name, String property) {
		final OneToMany oneToMany = field.getAnnotation(OneToMany.class);
		final boolean isLazyLoading = FetchType.LAZY.equals(oneToMany.fetch());
		final Class<?> parameterizedClass;
		if (!void.class.equals(oneToMany.targetEntity())) {
			parameterizedClass = oneToMany.targetEntity();
		} else {
			final ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
			final Type[] types = parameterizedType != null ? parameterizedType.getActualTypeArguments() : null;
			parameterizedClass = types != null && types.length > 0 ? (Class<?>) types[0] : null;
		}

		if (parameterizedClass == null) {
			return false;
		}

		final FacilityMetadata associatedEntity = findMetadata(parameterizedClass);
		if (associatedEntity == null) {
			throw new OrmException("Type [" + parameterizedClass + "] is not a registered entityMetadata.");
		}

		final JoinTable joinTable = field.getAnnotation(JoinTable.class);
		if (joinTable != null) {
			throw new OrmException("Unable to add column [" + field + "] - join table no supported.");
		} else if (!oneToMany.mappedBy().isEmpty()) {
			readSecondPass(associatedEntity);

			final ColumnMetadata mappedColumn = associatedEntity.findColumnMetadata(oneToMany.mappedBy());
			if (mappedColumn == null) {
				throw new OrmException("Unable to locate column [" + oneToMany.mappedBy() + "] within [" + associatedEntity + "].");
			}

			final ColumnVisitor visitor = columnVisitorFactory.manyVisit(field, associatedEntity, isLazyLoading);
			final ColumnMetadata mappedColumnMetadata = new MappedColumnMetadata(associatedEntity, mappedColumn, name, property, field.getType(), isLazyLoading);
			if (parentEntity.addColumnMetadata(mappedColumnMetadata)) {
				parentEntity.setVisitor(mappedColumnMetadata, visitor);
				associatedEntity.addIndex(new IndexMetadata(mappedColumn.getName(), Collections.singleton(mappedColumn), false));
				return true;
			}
		} else {
			final ColumnVisitor visitor = columnVisitorFactory.manyVisit(field, associatedEntity, isLazyLoading);
			final ColumnMetadata column = new JoinBagMetadata(name, property, field.getType(), associatedEntity, false, isLazyLoading, true);
			if (parentEntity.addColumnMetadata(column)) {
				parentEntity.setVisitor(column, visitor);
				return true;
			}
		}

		return false;
	}

	private Map<ColumnMetadata, ColumnVisitor> configureElementCollection(Field field, String name, String property) {
		final ElementCollection elementCollection = field.getAnnotation(ElementCollection.class);
		final boolean isLazyLoading = elementCollection != null && FetchType.LAZY.equals(elementCollection.fetch());
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
		DiscriminatorColumn column = null;
		for (Class<?> clazz : toClassHierarchy(entityClass)) {
			column = clazz.getAnnotation(DiscriminatorColumn.class);
			if (column != null) {
				break;
			}
		}

		if (column == null) {
			return null;
		}

		final String property = "#DiscriminatorColumn";
		final String name = column.name();
		DiscriminatorType discriminatorType = column.discriminatorType();
		if (discriminatorType == CHAR) {
			return new ColumnMetadata(name, property, Character.class, false, false, false);
		} else if (discriminatorType == INTEGER) {
			return new ColumnMetadata(name, property, Integer.class, false, false, false);
		} else {
			return new ColumnMetadata(name, property, String.class, false, false, false);
		}
	}

	private InheritMetadata getDiscriminatorValue(ColumnMetadata column, Class<?> entityClass) {
		final String discriminator = toClassHierarchy(entityClass)
				.stream()
				.map(clazz -> clazz.getAnnotation(DiscriminatorValue.class))
				.filter(Objects::nonNull)
				.findFirst()
				.map(DiscriminatorValue::value)
				.orElse(StringUtils.camelToSnakeCase(entityClass.getSimpleName()));

		if (column == null) {
			throw new OrmException("Discriminator column cannot be null.");
		}

		final Class<?> type = column.getType();
		if (Character.class.equals(type)) {
			return new InheritMetadata(column, discriminator.charAt(0));
		} else if (Short.class.equals(type)) {
			return new InheritMetadata(column, Short.parseShort(discriminator));
		} else if (Integer.class.equals(type)) {
			return new InheritMetadata(column, Integer.parseInt(discriminator));
		} else if (Long.class.equals(type)) {
			return new InheritMetadata(column, Long.parseLong(discriminator));
		} else {
			return new InheritMetadata(column, discriminator);
		}
	}

	private List<Field> getFields(Class<?> clazz) {
		return ReflectionUtils.getFields(clazz, jpaAnnotations());
	}

	private Collection<Class<? extends Annotation>> jpaAnnotations() {
		return Arrays.asList(Column.class, Embedded.class, EmbeddedId.class, Embeddable.class, Id.class, ElementCollection.class, JoinColumn.class, OneToMany.class, ManyToOne.class, ManyToMany.class, OneToOne.class, Transient.class, PostLoad.class);
	}
}
