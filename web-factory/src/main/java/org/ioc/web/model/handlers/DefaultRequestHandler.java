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
package org.ioc.web.model.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.ioc.web.exception.ConsumesException;
import org.ioc.web.exception.RateLimitException;
import org.ioc.web.exception.SessionNotFoundException;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.http.RequestEntry;
import org.ioc.web.model.http.ResponseEntry;
import org.ioc.web.model.mapping.Mapping;
import org.ioc.web.model.mapping.MappingContainer;
import org.ioc.web.model.resolvers.ArgumentResolver;
import org.ioc.web.model.resolvers.WebArgumentResolver;
import org.ioc.web.model.session.HttpSession;
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
@ChannelHandler.Sharable
@SuppressWarnings("all")
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

		final RequestEntry requestEntry = new RequestEntry(ctx.channel(), httpRequest);
		final HttpSession session = securityConfigureAdapter.getContext().findSession(requestEntry);
		if (session != null && session.hasExpires()) {
			String expiredPath = securityConfigureAdapter.getContainer().configureSession().getExpiredPath();
			expiredPath = expiredPath != null && !expiredPath.isEmpty() ? expiredPath : "/";
			sessionManager.remove(session.getSessionId());
			sendRedirect(ctx, requestEntry, expiredPath);
			return;
		}

		final ResponseEntry preparedResponseEntry = new ResponseEntry(sessionManager, requestEntry);

		final ModelAndView intercepted = new ModelAndView();

		Mapping mapping;
		try {
			final String uri = httpRequest.uri();
			mapping = mappingContainer.findMapping(requestEntry);

			if (mapping != null && mapping.getConsumes() != null && !mapping.getConsumes().isEmpty()) {
				if (Arrays.asList(httpRequest.headers().get("Accept").split(","))
						.contains(mapping.getConsumes())) {
					final Throwable t = new ConsumesException("Can't authorize request - wrong MIME type!");
					buildDefaultError(ctx, httpRequest, NOT_FOUND, preparedResponseEntry, t);
				}
			}

			final CheckResult result = securityConfigureAdapter.secureRequest(requestEntry, preparedResponseEntry, intercepted, mapping);
			if (!result.isOk()) {
				if (result.getThrowable() instanceof SessionNotFoundException) {
					sendRedirect(ctx, preparedResponseEntry, "/");
				} else if (result.getThrowable() instanceof SessionNotFoundException) {

				} else {
					buildDefaultError(ctx, httpRequest, BAD_REQUEST, preparedResponseEntry, result.getThrowable());
				}

				return;
			}

			if (mapping == null && result.isResource()) {
				final String resource = templateResolver.findResource(uri);
				if (resource != null) {
					if (!HttpServerUtil.resource(resource, ctx, httpRequest)) {
						final Exception e = new FileNotFoundException("Cant find resource from path: " + uri);
						buildDefaultError(ctx, httpRequest, NOT_FOUND, preparedResponseEntry, e);
						return;
					}
				}
			}

			webArgumentResolver.resolve(securityConfigureAdapter, mapping, requestEntry);

			for (Object r : resolvers) {
				((ArgumentResolver) r).resolve(securityConfigureAdapter, mapping, requestEntry);
			}
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				buildDefaultError(ctx, httpRequest, NO_CONTENT, preparedResponseEntry, e);
			} else if (e instanceof RateLimitException) {
				buildDefaultError(ctx, httpRequest, TOO_MANY_REQUESTS, preparedResponseEntry, e);
			} else {
				buildDefaultError(ctx, httpRequest, INTERNAL_SERVER_ERROR, preparedResponseEntry, e);
			}
			return;
		}

		handleMapping(ctx, mapping, requestEntry, httpRequest, preparedResponseEntry, intercepted);
	}

	private void handleMapping(ChannelHandlerContext ctx, Mapping mapping, RequestEntry requestEntry, FullHttpRequest httpRequest, ResponseEntry responseEntry, ModelAndView intercepted) throws Exception {
		final Object[] params = Objects.requireNonNull(mapping).getParameters();
		final Object instance = Objects.requireNonNull(mapping).getInstance();
		final Object invoked = Objects.requireNonNull(mapping).getMethod().invoke(instance, params);

		securityConfigureAdapter.postHandle(requestEntry, responseEntry, intercepted, mapping);

		String produces = mapping.getProduces();
		produces = produces != null && produces.isEmpty() ? "text/html" : produces;

		if (invoked instanceof File) {
			write((File) invoked, ctx, httpRequest);
			return;
		} else if (invoked instanceof ResponseEntry) {
			responseEntry = mergeResponse(responseEntry, (ResponseEntry) invoked);
		} else if (invoked instanceof ModelAndView) {
			final ModelAndView model = (ModelAndView) invoked;
			tryMergeModel(intercepted, model);

			responseEntry.setViewPage(templateResolver.resolveArguments(model.getView(), model));
			responseEntry.addHeader(CONTENT_TYPE, produces);
		} else if (mapping.isView()) {
			final ModelAndView model = responseEntry.getModel();
			tryMergeModel(intercepted, model);

			responseEntry.setViewPage(templateResolver.resolveArguments(invoked.toString(), model));
			responseEntry.addHeader(CONTENT_TYPE, produces);
		} else {
			responseEntry.setBody(invoked);
			responseEntry.setResponseStatus(OK);
			responseEntry.addHeader(CONTENT_TYPE, "application/json;charset=utf-8");
		}

		HttpResponse httpResponse;
		if (responseEntry.getViewPage() != null) {
			HttpServerUtil.resource(responseEntry.getViewPage(), ctx, httpRequest);
			return;
		}

		httpResponse = buildDefaultFullHttpResponse(responseEntry);
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

	private void buildDefaultError(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, ResponseEntry responseEntry, Throwable cause) throws IOException {
		final String uri = request == null ? "" : request.uri();
		final String view = formatErrorPage(status, uri, cause);
		responseEntry.setViewPage(view);
		responseEntry.setResponseStatus(status);
		responseEntry.setBody(cause);

		final HttpResponse response = HttpServerUtil.buildDefaultFullHttpResponse(responseEntry);
		ctx.writeAndFlush(response);

		HttpServerUtil.resource(responseEntry.getViewPage(), ctx, request);
	}
}
