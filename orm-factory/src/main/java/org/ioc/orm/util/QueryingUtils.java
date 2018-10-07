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
package org.ioc.orm.util;

import org.ioc.orm.metadata.type.FacilityMetadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class QueryingUtils {
	public static String prepareQuery(FacilityMetadata facilityMetadata, String query) {
		if (query == null || query.isEmpty()) {
			return "";
		}

		String result = query;
		result = changeEntityNames(facilityMetadata, result);
		result = installColumnNames(result);
		return result;
	}

	private static String changeEntityNames(FacilityMetadata facilityMetadata, String query) {
		if (facilityMetadata == null) {
			return query;
		}

		String result = query;
		for (Class<?> clazz : facilityMetadata.getTypes()) {
			result = result.replace(clazz.getSimpleName(), facilityMetadata.getTable());
			Class<?> parent = clazz.getSuperclass();
			while (parent != null && !Object.class.equals(parent)) {
				result = result.replace(parent.getSimpleName(), facilityMetadata.getTable());
				parent = parent.getSuperclass();
			}
		}
		return result;
	}

	private static String installColumnNames(String query) {
		final String upperCase = query.toUpperCase();
		final Matcher selectMatcher = Pattern.compile("SELECT").matcher(upperCase);
		if (!selectMatcher.find()) {
			return query;
		}

		final Matcher fromMatcher = Pattern.compile("FROM").matcher(upperCase);
		if (!fromMatcher.find(selectMatcher.end())) {
			return query;
		}

		final String columns = query.substring(selectMatcher.end() + 1, fromMatcher.start()).trim();
		if (!columns.isEmpty()) {
			return query;
		}

		return query.substring(0, selectMatcher.end() + 1) + " * " + query.substring(fromMatcher.start());
	}
}
