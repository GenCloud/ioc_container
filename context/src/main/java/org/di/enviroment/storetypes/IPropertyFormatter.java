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
package org.di.enviroment.storetypes;

import java.io.IOException;

/**
 * Interface for store configuration files in different formats.
 * <p>
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public interface IPropertyFormatter {
    /**
     * Adds property entry.
     *
     * @param key   Entry key.
     * @param value Entry value.
     */
    void addPair(String key, String value);

    /**
     * Generates configuration file text based on type of this store formatter.
     *
     * @return Generated configuration file.
     * @throws IOException Used to re-throw unusual exceptions during format.
     */
    String generate() throws IOException;
}
