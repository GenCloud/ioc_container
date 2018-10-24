/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of IoC Starter Project.
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
package org.ioc.web.model.resolvers;

import com.google.gson.Gson;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;
import org.ioc.web.annotations.*;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.http.RequestEntry;
import org.ioc.web.model.mapping.Mapping;
import org.ioc.web.security.configuration.SecurityConfigureAdapter;
import org.ioc.web.security.user.UserDetails;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class WebArgumentResolver implements ArgumentResolver {
	private static void fileArgumentResolve(FullHttpRequest request, Mapping mapping) {
		try {
			final HttpDataFactory factory = new DefaultHttpDataFactory(true);
			final HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, request);
			final Map<String, File> map = new HashMap<>();

			decoder.setDiscardThreshold(0);

			while (decoder.hasNext()) {
				final InterfaceHttpData httpData = decoder.next();
				if (httpData.getHttpDataType() == HttpDataType.FileUpload) {
					final FileUpload fileUpload = (FileUpload) httpData;
					final File file = new File(fileUpload.getFilename());

					try (FileChannel inputChannel = new FileInputStream(fileUpload.getFile()).getChannel();
						 FileChannel outputChannel = new FileOutputStream(file).getChannel()) {
						outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
					}

					map.put(httpData.getName(), file);
				}
			}

			if (!map.isEmpty()) {
				final Method method = mapping.getMethod();
				final Parameter[] parameters = method.getParameters();
				int bound = parameters.length;
				for (int i = 0; i < bound; i++) {
					final RequestParam requestParam = parameters[i].getAnnotation(RequestParam.class);
					if (requestParam != null) {
						if (File.class.isAssignableFrom(parameters[i].getType())) {
							if (map.containsKey(requestParam.value())) {
								final File file = map.get(requestParam.value());
								mapping.setParameter(file, i);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void resolve(SecurityConfigureAdapter sca, Mapping mapping, RequestEntry requestEntry) {
		final String path = requestEntry.getPath();

		requestArgumentResolver(requestEntry, mapping);

		pathArgumentResolve(path, mapping);

		final FullHttpRequest httpRequest = requestEntry.getHttpRequest();

		requestMethodArgumentResolver(mapping.getHttpMethod(), httpRequest, path, mapping);

		if (Objects.equals(httpRequest.headers().get("content-Type"), "application/json")) {
			requestJsonResolve(httpRequest.content().copy().toString(CharsetUtil.UTF_8), mapping);
		}

		if (httpRequest.method() == HttpMethod.POST) {
			if (httpRequest.headers().get("content-Type") != null
					&& httpRequest.headers().get("content-Type").startsWith("multipart/form-data")) {
				fileArgumentResolve(httpRequest, mapping);
			}
		}

		modelArgumentResolve(mapping);

		userDetailsArgumentResolve(requestEntry, sca, mapping);
	}

	private void userDetailsArgumentResolve(RequestEntry requestEntry, SecurityConfigureAdapter securityConfigureAdapter,
											Mapping mapping) {
		final UserDetails userDetails = securityConfigureAdapter.getContext().findCredentials(requestEntry);
		final Parameter[] parameters = mapping.getMethod().getParameters();
		IntStream.range(0, parameters.length).forEach(i -> {
			final Credentials credentials = parameters[i].getAnnotation(Credentials.class);
			if (credentials != null) {
				mapping.setParameter(userDetails, i);
			}
		});
	}

	private void modelArgumentResolve(Mapping mapping) {
		final Method method = mapping.getMethod();
		final Parameter[] parameters = method.getParameters();
		IntStream.range(0, parameters.length).forEach(i -> {
			final Class<?> type = parameters[i].getType();
			if (ModelAndView.class.isAssignableFrom(type)) {
				final ModelAndView model = new ModelAndView();
				mapping.setParameter(model, i);
			}
		});
	}

	private void requestJsonResolve(String s, Mapping mapping) {
		final Method method = mapping.getMethod();
		final Parameter[] parameters = method.getParameters();
		IntStream.range(0, parameters.length).forEach(i -> {
			final Json json = parameters[i].getAnnotation(Json.class);
			if (json != null) {
				final Class<?> type = parameters[i].getType();
				final Object o = new Gson().fromJson(s, type);
				mapping.setParameter(o, i);
			}
		});
	}

	private void pathArgumentResolve(String path, Mapping mapping) {
		final Method method = mapping.getMethod();
		final UrlMapping urlMapping = method.getAnnotation(UrlMapping.class);

		if (urlMapping == null) {
			return;
		}

		final String uri = urlMapping.value();
		final String[] requestPaths = path.split("/");
		final String[] originPath = uri.split("/");

		final Parameter[] parameters = method.getParameters();

		for (int i = 0; i < requestPaths.length && i < originPath.length; i++) {
			if (!requestPaths[i].equals(originPath[i])) {
				for (int j = 0; j < parameters.length; j++) {
					if (!parameters[j].getName().equals(originPath[i].substring(1, originPath[i].length() - 1))) {
						if (parameters[j].getType().equals(Integer.class)) {
							mapping.setParameter(Integer.valueOf(requestPaths[i]), j);
						} else if (parameters[j].getType().equals(Long.class)) {
							mapping.setParameter(Long.valueOf(requestPaths[i]), j);
						} else if (parameters[j].getType().equals(String.class)) {
							mapping.setParameter(requestPaths[i], j);
						} else if (parameters[j].getType().equals(Date.class)) {
							final DateFormatter dateFormatter = parameters[j].getAnnotation(DateFormatter.class);
							if (dateFormatter != null) {
								final SimpleDateFormat format = new SimpleDateFormat(dateFormatter.value());
								final String dateParam = requestPaths[i];
								try {
									final Date date = format.parse(dateParam);
									mapping.setParameter(date, j);
								} catch (ParseException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
	}

	private void requestArgumentResolver(RequestEntry requestEntry, Mapping mapping) {
		final Parameter[] parameters = mapping.getMethod().getParameters();
		IntStream.range(0, parameters.length).forEach(i -> {
			if (RequestEntry.class.isAssignableFrom(parameters[i].getType())) {
				mapping.setParameter(requestEntry, i);
			}
		});
	}

	private void requestMethodArgumentResolver(HttpMethod method, FullHttpRequest request, String path, Mapping mapping) {
		final Parameter[] parameters = mapping.getMethod().getParameters();
		if (method.name().equals("GET")) {
			final QueryStringDecoder decoder = new QueryStringDecoder(path);
			final Map<String, List<String>> map = decoder.parameters();

			IntStream.range(0, parameters.length).forEach(i -> {
				final Parameter parameter = parameters[i];
				final RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
				final DateFormatter dateFormatter = parameter.getAnnotation(DateFormatter.class);
				if (requestParam != null) {
					final List<String> list = map.get(requestParam.value());

					if (list == null || list.isEmpty()) {
						mapping.setParameter(null, i);
					} else {
						if (dateFormatter != null) {
							final SimpleDateFormat format = new SimpleDateFormat(dateFormatter.value());
							final String dateParam = list.get(0);
							try {
								final Date date = format.parse(dateParam);
								mapping.setParameter(date, i);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						} else {
							mapping.setParameter(list.get(0), i);
						}
					}
				}
			});
		} else if (method.name().equals("POST")) {
			final HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
			final List<InterfaceHttpData> interfaceHttpData = decoder.getBodyHttpDatas();

			interfaceHttpData
					.forEach(httpData -> {
						if (httpData.getHttpDataType().equals(HttpDataType.Attribute)) {
							Attribute attribute = (Attribute) httpData;

							IntStream.range(0, parameters.length).forEach(i -> {
								final Parameter parameter = parameters[i];
								final RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
								final DateFormatter dateFormatter = parameter.getAnnotation(DateFormatter.class);

								if (requestParam != null) {
									if (requestParam.value().equals(httpData.getName())) {
										try {
											final String value = attribute.getValue();
											if (dateFormatter != null) {
												final SimpleDateFormat format = new SimpleDateFormat(dateFormatter.value());
												final Date date = format.parse(value);
												mapping.setParameter(date, i);
											} else {
												mapping.setParameter(value, i);
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							});
						}
					});
		}
	}
}
