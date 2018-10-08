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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.ioc.orm.exceptions.OrmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author GenCloud
 * @date 10/2018
 */
@SuppressWarnings("deprecation")
class DataUtils {
	private static final Logger log = LoggerFactory.getLogger(DataUtils.class);

	private static ObjectMapper createMapper() {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
		mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
		mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE));
		return mapper;
	}

	public static byte[] objectToBytes(Serializable value) {
		if (value == null) {
			return new byte[0];
		}

		try {
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			final ObjectOutputStream output = new ObjectOutputStream(bytes);
			output.writeObject(value);
			IOUtils.closeQuietly(output);
			return bytes.toByteArray();
		} catch (Exception e) {
			log.warn("Unable to serialize value [" + value + "].", e);
			return new byte[0];
		}
	}

	public static <T extends Serializable> T bytesToObject(Class<T> clazz, byte[] value) {
		if (value == null || value.length <= 0) {
			return null;
		}

		try {
			final ByteArrayInputStream bytes = new ByteArrayInputStream(value);
			final ObjectInputStream input = new ClassLoaderObjectInputStream(clazz.getClassLoader(), bytes);
			final Object instance = input.readObject();
			IOUtils.closeQuietly(input);
			return clazz.cast(instance);
		} catch (Exception e) {
			log.warn("Unable to deserialize value [{}] of type [{}].", Arrays.toString(value), clazz);
			e.printStackTrace();
			return null;
		}
	}

	public static byte[] uuidToBytes(UUID uuid) {
		if (uuid == null) {
			return new byte[0];
		}

		final ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.putLong(uuid.getMostSignificantBits());
		buffer.putLong(uuid.getLeastSignificantBits());
		buffer.flip();
		return buffer.array();
	}

	public static UUID bytesToUUID(byte[] bytes) {
		if (bytes == null || bytes.length <= 0) {
			return null;
		}

		return bytesToUUID(ByteBuffer.wrap(bytes));
	}

	private static UUID bytesToUUID(ByteBuffer bytes) {
		if (bytes == null) {
			return null;
		}

		return new UUID(bytes.getLong(bytes.position()), bytes.getLong(bytes.position() + 8));
	}

	public static String objectToJson(Object obj) {
		if (null == obj) {
			return null;
		}
		try {
			return createMapper().writeValueAsString(obj);
		} catch (IOException e) {
			throw new OrmException("Unable to convert item to json.", e);
		}
	}

	public static <T> T jsonToObject(String json, Class<T> clazz) {
		if (null == json || json.isEmpty()) {
			return null;
		}
		try {
			return createMapper().readValue(json, clazz);
		} catch (IOException e) {
			throw new OrmException("Unable to install item from json.", e);
		}
	}
}
