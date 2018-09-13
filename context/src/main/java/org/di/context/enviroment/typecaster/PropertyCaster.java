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
package org.di.context.enviroment.typecaster;

import org.di.context.enviroment.typecaster.exception.IllegalPropertyException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The Type Caster is small utility that helps put string values into object fields with different types.
 * <p>
 *
 * @author GenCloud
 * @date 05.09.2018
 */
public class PropertyCaster {
    private final static Class[] ALLOWED_TYPES = {
            Integer.class, int.class,
            Short.class, short.class,
            Float.class, float.class,
            Double.class, double.class,
            Long.class, long.class,
            Boolean.class, boolean.class,
            String.class,
            Character.class, char.class,
            Byte.class, byte.class,
            AtomicInteger.class, AtomicBoolean.class, AtomicLong.class,
            BigInteger.class, BigDecimal.class,
            Class.class
    };

    /**
     * Puts value to field.
     *
     * @param object Object, whom field value should be changed.
     * @param field  Class field.
     * @param value  Value to cast.
     */
    @SuppressWarnings("unchecked")
    public static void cast(Object object, Field field, String value) throws Exception {
        if (!isCastable(field))
            throw new IllegalPropertyException("Unsupported type [" + field.getType().getName() + "] for field [" + field.getName() + "]");

        Class type = field.getType();
        boolean oldAccess = field.isAccessible();

        field.setAccessible(true);

        if (type.isEnum()) {
            field.set(object, Enum.valueOf((Class<Enum>) type, value));
        } else if (type == Integer.class || type == int.class) {
            field.set(object, Integer.decode(value));
        } else if (type == Short.class || type == short.class) {
            field.set(object, Short.decode(value));
        } else if (type == Float.class || type == float.class) {
            field.set(object, Float.parseFloat(value));
        } else if (type == Double.class || type == double.class) {
            field.set(object, Double.parseDouble(value));
        } else if (type == Long.class || type == long.class) {
            field.set(object, Long.decode(value));
        } else if (type == Boolean.class || type == boolean.class) {
            field.set(object, Boolean.parseBoolean(value));
        } else if (type == String.class) {
            field.set(object, value);
        } else if (type == Character.class || type == char.class) {
            field.set(object, value.charAt(0));
        } else if (type == Byte.class || type == byte.class) {
            field.set(object, Byte.parseByte(value));
        } else if (type == AtomicInteger.class) {
            field.set(object, new AtomicInteger(Integer.decode(value)));
        } else if (type == AtomicBoolean.class) {
            field.set(object, new AtomicBoolean(Boolean.parseBoolean(value)));
        } else if (type == AtomicLong.class) {
            field.set(object, new AtomicLong(Long.decode(value)));
        } else if (type == BigInteger.class) {
            field.set(object, new BigInteger(value));
        } else if (type == BigDecimal.class) {
            field.set(object, new BigDecimal(value));
        } else if (type == Class.class) {
            field.set(object, Class.forName(value));
        } else {
            field.setAccessible(oldAccess);
            throw new IllegalPropertyException("Unsupported type [" + type.getName() + "] for field [" + field.getName() + "]");
        }

        field.setAccessible(oldAccess);
    }

    /**
     * Changes targets' value to new given value with type casting.
     *
     * @param type  Cast type.
     * @param value Value to cast.
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Class<T> type, String value) throws IllegalPropertyException {
        if (!isCastable(type))
            throw new IllegalPropertyException("Unsupported type [" + type.getName() + "]");

        if (type.isEnum()) {
            return (T) Enum.valueOf((Class<Enum>) type, value);
        } else if (type == Integer.class || type == int.class) {
            return (T) Integer.decode(value);
        } else if (type == Short.class || type == short.class) {
            return (T) Short.decode(value);
        } else if (type == Float.class || type == float.class) {
            return (T) (Float) Float.parseFloat(value);
        } else if (type == Double.class || type == double.class) {
            return (T) (Double) Double.parseDouble(value);
        } else if (type == Long.class || type == long.class) {
            return (T) Long.decode(value);
        } else if (type == Boolean.class || type == boolean.class) {
            return (T) (Boolean) Boolean.parseBoolean(value);
        } else if (type == String.class) {
            return (T) value;
        } else if (type == Character.class || type == char.class) {
            return (T) ((Object) value.charAt(0));
        } else if (type == Byte.class || type == byte.class) {
            return (T) Byte.decode(value);
        } else if (type == AtomicInteger.class) {
            return (T) new AtomicInteger(Integer.decode(value));
        } else if (type == AtomicBoolean.class) {
            return (T) new AtomicBoolean(Boolean.parseBoolean(value));
        } else if (type == AtomicLong.class) {
            return (T) new AtomicLong(Long.decode(value));
        } else if (type == BigInteger.class) {
            return (T) new BigInteger(value);
        } else if (type == BigDecimal.class) {
            return (T) new BigDecimal(value);
        } else {
            throw new IllegalPropertyException("Unsupported type [" + type.getName() + "]");
        }
    }

    /**
     * Checks whether class can be used for casting with TypeCaster.
     *
     * @param type Class or type to check.
     * @return True, if class or type can be casted, false in other case.
     */
    public static boolean isCastable(Class type) {
        if (type.isEnum())
            return true;

        for (Class t : ALLOWED_TYPES) {
            if (t == type)
                return true;
        }
        return false;
    }

    /**
     * Checks whether object can be casted by TypeCaster.
     *
     * @param object Object to check.
     * @return True, if object can be casted, false in other case.
     */
    public static boolean isCastable(Object object) {
        return isCastable(object.getClass());
    }

    /**
     * Checks whether field castable by type caster or not.
     *
     * @param field Field to check is castable.
     * @return True, if field can be casted, false in other case.
     */
    public static boolean isCastable(Field field) {
        return isCastable(field.getType());
    }
}
