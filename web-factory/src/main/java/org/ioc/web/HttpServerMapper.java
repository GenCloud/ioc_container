package org.ioc.web;

import io.netty.handler.codec.http.HttpRequest;
import org.ioc.annotations.web.MappingMethod;
import org.ioc.annotations.web.Param;
import org.ioc.cache.ICache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class HttpServerMapper {
	private final Object instance;

	private final Method method;

	private final String[] names;

	public HttpServerMapper(final Object instance, final Method method) {
		this.instance = instance;
		this.method = method;

		names = new String[method.getParameterTypes().length];

		final Class<?>[] types = method.getParameterTypes();

		for (int i = 0; i < names.length; i++)
			names[i] = types[i].getName();

		final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		Arrays
				.stream(parameterAnnotations)
				.forEach(annotations ->
						Arrays
								.stream(annotations)
								.filter(annotation -> annotation instanceof Param)
								.forEach(annotation ->
										IntStream
												.range(0, names.length)
												.forEach(i ->
														names[i] = ((Param) annotation).value())));
	}

	static HttpServerMapper findMapping(final HttpRequest request, final String uri, ICache<MappingMethod, Map<String, HttpServerMapper>> mappingRequests) {
		HttpServerMapper mapping = null;

		if (request.method() == GET) {
			mapping = mappingRequests.get(MappingMethod.GET).get(uri);
		} else if (request.method() == POST) {
			mapping = mappingRequests.get(MappingMethod.POST).get(uri);
		}

		return mapping;
	}

	Object getInstance() {
		return instance;
	}

	Method getMethod() {
		return method;
	}

	String getName(int pos) {
		return names[pos];
	}
}