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
package org.ioc.orm.interpretator.specs;

import org.ioc.orm.metadata.type.ColumnMetadata;
import org.ioc.orm.metadata.type.FacilityMetadata;

import java.util.Arrays;
import java.util.List;

import static org.ioc.orm.interpretator.specs.enums.KeyWordType.RESERVED_KEYWORDS;
import static org.ioc.orm.interpretator.specs.enums.KeyWordType.VARIABLE;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class KeyWordResolver {
	private List<KeyWord> keywords = Arrays.asList(
			new KeyWord("FIND", RESERVED_KEYWORDS, "select *"),
			new KeyWord("COUNT", RESERVED_KEYWORDS, "select count(*)"),
			new KeyWord("BY", RESERVED_KEYWORDS, " where"),
			new KeyWord("AND", RESERVED_KEYWORDS, " and"),
			new KeyWord("OR", RESERVED_KEYWORDS, " or"),
			new KeyWord("EQ", RESERVED_KEYWORDS, " = value"),
			new KeyWord("LT", RESERVED_KEYWORDS, " < value"),
			new KeyWord("LTE", RESERVED_KEYWORDS, " <= value"),
			new KeyWord("GT", RESERVED_KEYWORDS, " > value"),
			new KeyWord("GTE", RESERVED_KEYWORDS, " >= value"),
			new KeyWord("NE", RESERVED_KEYWORDS, " != value"),
			new KeyWord("BEFORE", RESERVED_KEYWORDS, " < value"),
			new KeyWord("AFTER", RESERVED_KEYWORDS, " > value"),
			new KeyWord("BETWEEN", RESERVED_KEYWORDS, " between (value, value)"),
			new KeyWord("ORDER", RESERVED_KEYWORDS, " order by"),
			new KeyWord("LIMIT", RESERVED_KEYWORDS, " limit value"),
			new KeyWord("LIMITFROM", RESERVED_KEYWORDS, " limit value, value"),
			new KeyWord("STARTWITH", RESERVED_KEYWORDS, " like value"),
			new KeyWord("ENDWITH", RESERVED_KEYWORDS, " like value"),
			new KeyWord("CONTAINS", RESERVED_KEYWORDS, " like value")
	);

	private FacilityMetadata facilityMetadata;

	public KeyWordResolver(FacilityMetadata facilityMetadata) {
		this.facilityMetadata = facilityMetadata;
	}

	public KeyWord find(String input) {
		for (KeyWord keyWord : keywords) {
			if (keyWord.getName().equalsIgnoreCase(input)) {
				return keyWord;
			}
		}

		for (ColumnMetadata columnMetadata : facilityMetadata) {
			final String columnName = columnMetadata.getName();
			if (columnName.equalsIgnoreCase(input)) {
				return new KeyWord(input, VARIABLE, " " + columnName);
			}
		}
		return null;
	}

	public KeyWord fromOperator() {
		return new KeyWord("FROM", RESERVED_KEYWORDS, " from " + facilityMetadata.getTable());
	}
}
