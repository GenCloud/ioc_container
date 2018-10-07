package org.ioc.orm.repositories;

import org.ioc.orm.factory.SchemaQuery;

import java.util.Map;

/**
 * @author GenCloud
 * @date 10/2018
 */
public interface ProxyRepository<Entity, ID> extends CrudRepository<Entity, ID> {
	SchemaQuery<Entity> executePreparedQuery(String prepared, Map<String, Object> params);

	SchemaQuery<Entity> executePreparedQueryWithoutParams(String prepared);
}
