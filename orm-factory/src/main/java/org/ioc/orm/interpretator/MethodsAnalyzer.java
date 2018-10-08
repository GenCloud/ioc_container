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
package org.ioc.orm.interpretator;

import org.ioc.orm.exceptions.OrmException;
import org.ioc.orm.interpretator.specs.KeyWord;
import org.ioc.orm.interpretator.specs.KeyWordResolver;
import org.ioc.orm.metadata.type.FacilityMetadata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class MethodsAnalyzer {
	private static final Integer FROM_TOKEN_POSITION = 1;

	private KeyWordResolver keyWordResolver;

	public MethodsAnalyzer(FacilityMetadata facilityMetadata) {
		keyWordResolver = new KeyWordResolver(facilityMetadata);
	}

	public String toQuery(Method method, Object[] args) throws IllegalArgumentException {
		final List<KeyWord> keyWords = new ArrayList<>();
		KeyWord currKeyWord = null;
		KeyWord potentialKeyWord;
		final StringBuilder tokenBuilder = new StringBuilder(100);
		int index = 0;
		do {
			for (int i = index; i < method.getName().length(); i++) {
				tokenBuilder.append(method.getName().charAt(i));
				potentialKeyWord = keyWordResolver.find(tokenBuilder.toString());
				if (potentialKeyWord != null) {
					currKeyWord = potentialKeyWord;
					index = i + 1;
				}
			}

			if (currKeyWord == null) {
				throw new OrmException("Invalid token " + method.getName().substring(index));
			}

			keyWords.add(currKeyWord);
			currKeyWord = null;
			tokenBuilder.setLength(0);
		} while (index < method.getName().length());

		keyWords.add(FROM_TOKEN_POSITION, keyWordResolver.fromOperator());

		final StringBuilder queryBuilder = new StringBuilder(50);
		for (KeyWord keyWord : keyWords) {
			queryBuilder.append(keyWord.getRepresentation());
		}

		String query = queryBuilder.toString();
		for (int i = 0; i < method.getParameters().length; i++) {
			final Object value = args[i];
			query = query.replaceFirst("value", "'" + value + "'");
		}

		return query;
	}
}
