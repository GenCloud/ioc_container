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
package org.di.annotations.property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Property annotation represents ability to annotate fields and methods for lazy configuration files parsing.<br /><br />
 * <p>
 * You can use it like this:
 * <pre>
 * class PropertySet
 * {
 *     {@literal @}Property
 *     private int PROPERTY;
 *
 *     {@literal @}Property
 *     private BarClass OBJECT;
 *
 *     {@literal @}Property
 *     public void intProperty(int value) { ... }
 * }</pre>
 * <br />
 * When annotate fields, library will parse property from file and manually cast its value to field type.
 * When annotate methods, library will parse property from file and send its contents to first method argument. <b>Important!</b> Method should have only one argument.
 * When annotate fields of custom types, library will parse property and create a new instance of an object. <b>Important!</b> Constructor should be marked with @Cfg property too
 * and should have only one argument.
 *
 * <h1>Splitters</h1>
 * <p>
 * Let's assume you want to read some string with using separation token.<br />
 * For example: CONCATENATED_INT_VALUES = 1;2;3;4;5<br /><br />
 * <p>
 * Then instead of creating method handler for such property you can register field like this:<br />
 * <pre>
 * class Config
 * {
 *     // There is empty int array will be default value (if config will not be found in properties file)
 *     // And property value will be splitted with using of ";" symbol as split token.
 *     // So, in CONCATENATED_INT_VALUES will be array int[]{1, 2, 3, 4, 5}
 *     {@literal @}Property
 *     public static int[] CONCATENATED_INT_VALUES = new int[];
 * }
 * </pre>
 * You can override split token and field name.
 *
 * @author GenCloud
 * @date 06.09.2018
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Property {
    /**
     * Path to configuration file.
     */
    String path();

    /**
     * Custom property name. Can be used when real property name differs from class field.
     */
    String value() default "";

    /**
     * Splitter that can be used for splitting settings into arrays & collections.
     */
    String splitter() default ";";

    /**
     * Should be set up to true if you with to ignore some field of class.
     */
    boolean ignore() default false;

    /**
     * Prefix of property name can be used for batch loading of property sets with prefixes
     * For example, let properties be: db.name, db.user, db.password
     * then we can load them by annotating class by {@link Property} annotation:
     * <pre>{@literal @}Property(prefix = "db.")
     * class DatabaseConfig
     * {
     *     // ...
     * }
     * </pre>
     */
    String prefix() default "";

    /**
     * Allows parametrization of properties.
     * Parametrization is pre-work of parser and it affects native string values from property file.
     * Property parameters is sequence: ${key}, where key is property name from configuration file.
     */
    boolean parametrize() default false;
}
