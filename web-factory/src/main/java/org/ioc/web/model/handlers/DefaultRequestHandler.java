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
package org.ioc.web.model.handlers;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.ioc.web.exception.ConsumesException;
import org.ioc.web.exception.SessionNotFoundException;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.http.Request;
import org.ioc.web.model.http.Response;
import org.ioc.web.model.mapping.Mapping;
import org.ioc.web.model.mapping.MappingContainer;
import org.ioc.web.model.resolvers.ArgumentResolver;
import org.ioc.web.model.resolvers.WebArgumentResolver;
import org.ioc.web.model.session.SessionManager;
import org.ioc.web.model.view.TemplateResolver;
import org.ioc.web.security.CheckResult;
import org.ioc.web.security.configuration.SecurityConfigureAdapter;
import org.ioc.web.util.HttpServerUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static org.ioc.web.util.HttpServerUtil.*;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class DefaultRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private final MappingContainer mappingContainer;
	private final TemplateResolver templateResolver;
	private final SessionManager sessionManager;
	private final SecurityConfigureAdapter securityConfigureAdapter;
	private final List<Object> resolvers;

	private final WebArgumentResolver webArgumentResolver = new WebArgumentResolver();

	public DefaultRequestHandler(MappingContainer mappingContainer, TemplateResolver templateResolver,
								 SessionManager sessionManager, SecurityConfigureAdapter securityConfigureAdapter,
								 List<Object> resolvers) {
		this.mappingContainer = mappingContainer;
		this.templateResolver = templateResolver;
		this.sessionManager = sessionManager;
		this.securityConfigureAdapter = securityConfigureAdapter;
		this.resolvers = resolvers;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
		if (httpRequest == null) {
			return;
		}

		final Request request = new Request(ctx.channel(), httpRequest);

		Response preparedResponse = new Response(sessionManager, request);

		final ModelAndView intercepted = new ModelAndView();

		Mapping mapping;
		try {
			final String uri = httpRequest.uri();
			mapping = mappingContainer.findMapping(request);

			if (mapping != null && mapping.getConsumes() != null && !mapping.getConsumes().isEmpty()) {
				if (Arrays.asList(httpRequest.headers().get("Accept").split(","))
						.contains(mapping.getConsumes())) {
					final Throwable t = new ConsumesException("Can't authorize request - wrong MIME type!");
					buildDefaultError(ctx, httpRequest, NOT_FOUND, preparedResponse, t);
				}
			}

			final CheckResult result = securityConfigureAdapter.secureRequest(request, preparedResponse, intercepted, mapping);
			if (!result.isOk()) {
				if (result.getThrowable() instanceof SessionNotFoundException) {
					final DefaultFullHttpResponse response = buildDefaultFullHttpResponse(preparedResponse, FOUND);
					response.headers().set(HttpHeaderNames.LOCATION, "/");
					ctx.write(response).addListener(ChannelFutureListener.CLOSE);
					return;
				} else {
					buildDefaultError(ctx, httpRequest, NOT_FOUND, preparedResponse, result.getThrowable());
					return;
				}
			}

			if (mapping == null && result.isResource()) {
				final String resource = templateResolver.findResource(uri);
				if (resource != null) {
					if (!HttpServerUtil.resource(resource, ctx, httpRequest)) {
						final Exception e = new FileNotFoundException("Cant find resource from path: " + uri);
						buildDefaultError(ctx, httpRequest, NOT_FOUND, preparedResponse, e);
						return;
					}
				}
			}

			webArgumentResolver.resolve(securityConfigureAdapter, mapping, request);

			for (Object r : resolvers) {
				((ArgumentResolver) r).resolve(securityConfigureAdapter, mapping, request);
			}
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				buildDefaultError(ctx, httpRequest, NO_CONTENT, preparedResponse, e);
			} else {
				buildDefaultError(ctx, httpRequest, INTERNAL_SERVER_ERROR, preparedResponse, e);
			}
			return;
		}

		handleMapping(ctx, mapping, httpRequest, preparedResponse, intercepted);
	}

	private void handleMapping(ChannelHandlerContext ctx, Mapping mapping, FullHttpRequest httpRequest, Response preparedResponse, ModelAndView intercepted) throws Exception {
		final Object[] params = Objects.requireNonNull(mapping).getParameters();
		final Object instance = Objects.requireNonNull(mapping).getInstance();
		final Object invoked = Objects.requireNonNull(mapping).getMethod().invoke(instance, params);

		String produces = mapping.getProduces();
		produces = produces != null && produces.isEmpty() ? "text/html" : produces;

		if (invoked instanceof File) {
			write((File) invoked, ctx, httpRequest);
			return;
		} else if (invoked instanceof Response) {
			preparedResponse = mergeResponse(preparedResponse, (Response) invoked);
		} else if (invoked instanceof ModelAndView) {
			final ModelAndView model = (ModelAndView) invoked;
			tryMergeModel(intercepted, model);

			preparedResponse.setViewPage(templateResolver.resolveArguments(model.getView(), model));
			preparedResponse.addHeader(CONTENT_TYPE, produces);
		} else if (mapping.isView()) {
			final ModelAndView model = preparedResponse.getModel();
			tryMergeModel(intercepted, model);

			preparedResponse.setViewPage(templateResolver.resolveArguments(invoked.toString(), model));
			preparedResponse.addHeader(CONTENT_TYPE, produces);
		} else {
			preparedResponse.setBody(invoked);
			preparedResponse.setResponseStatus(OK);
			preparedResponse.addHeader(CONTENT_TYPE, "application/json;charset=utf-8");
		}

		HttpResponse httpResponse;
		if (preparedResponse.getViewPage() != null) {
			HttpServerUtil.resource(preparedResponse.getViewPage(), ctx, httpRequest);
			return;
		}

		httpResponse = buildDefaultFullHttpResponse(preparedResponse);
		ctx.write(httpResponse);
	}

	private void tryMergeModel(ModelAndView intercepted, ModelAndView model) {
		if (!intercepted.isEmpty()) {
			model.addAllAttributes(intercepted);
			final String view = intercepted.getView();
			if (view != null && !view.isEmpty()) {
				model.setView(view);
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		ctx.writeAndFlush(buildDefaultFullHttpResponse(null, INTERNAL_SERVER_ERROR));
		cause.printStackTrace();
	}

	private void buildDefaultError(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, Response response, Throwable cause) throws IOException {
		final String uri = request == null ? "" : request.uri();
		final String view = formatErrorPage(status, uri, cause);
		response.setViewPage(view);
		response.setResponseStatus(status);
		response.addHeader(CONTENT_TYPE, "text/html");

		HttpServerUtil.resource(response.getViewPage(), ctx, request);
	}
}
