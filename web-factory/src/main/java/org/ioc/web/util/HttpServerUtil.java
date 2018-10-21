/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of DI (IoC) Container Project.
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
package org.ioc.web.util;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.apache.commons.io.IOUtils;
import org.ioc.web.annotations.MappingMethod;
import org.ioc.web.model.http.Cookie;
import org.ioc.web.model.http.Response;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class HttpServerUtil {
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String SET_COOKIE = "Set-Cookie";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONNECTION = "Connection";
	public static final String SERVER = "Server";

	private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz", Locale.getDefault());

	public static Response mergeResponse(Response responseTo, Response responseFrom) {
		responseTo.setBody(responseFrom.getBody());
		responseTo.setResponseStatus(responseFrom.getResponseStatus());
		responseTo.setViewPage(responseFrom.getViewPage());
		responseTo.setModel(responseFrom.getModel());

		responseFrom.getCookies().stream().filter(Objects::nonNull).forEach(responseTo::addCookie);
		responseFrom.getHeaders().forEach(responseTo::addHeader);
		return responseTo;
	}

	public static String formatErrorPage(HttpResponseStatus status, String request, Throwable cause) throws IOException {
		final InputStream in = HttpServerUtil.class.getClassLoader().getResourceAsStream("./error.html");
		final StringWriter writer = new StringWriter();
		IOUtils.copy(in, writer, Charset.forName("UTF-8"));
		String str = writer.toString();

		final Date date = new Date(System.currentTimeMillis());
		str = str.replace("${timestamp}", date.toString())
				.replace("${path}", request)
				.replace("${error}", status.reasonPhrase())
				.replace("${status}", String.valueOf(status.code()))
				.replace("${exception}", cause != null ? cause.getClass().getSimpleName() : "No exception")
				.replace("${trace}", cause != null && cause.getMessage() != null ? cause.getMessage() : "No trace");
		return str;
	}

	public static HttpResponse buildDefaultFullHttpResponse(Response from) {
		DefaultHttpResponse fullHttpResponse;

		int length = 0;
		if (from.getBody() == null && from.getViewPage() == null) {
			fullHttpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, from.getResponseStatus());
		} else {
			if (from.getBody() != null) {
				final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						from.getResponseStatus(), Unpooled.copiedBuffer(new Gson().toJson(from.getBody()).getBytes()));
				length = response.content().readableBytes();
				fullHttpResponse = response;
			} else {
				fullHttpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, from.getResponseStatus());
			}
		}

		from.getHeaders().keySet().forEach(s -> fullHttpResponse.headers().add(s, from.getHeaders().get(s)));

		if (!fullHttpResponse.headers().contains("Content-type")) {
			if (from.getBody() != null) {
				if (from.getBody() instanceof String) {
					fullHttpResponse.headers().set(CONTENT_TYPE, "text/plain");
				}
			} else {
				fullHttpResponse.headers().set(CONTENT_TYPE, "application/json;charset=utf-8");
			}
		}

		if (from.getCookies() != null) {
			fullHttpResponse.headers().add(SET_COOKIE, from.getCookies().stream()
					.map(Cookie::toString)
					.collect(Collectors.toList()));
		}

		final long contentLength = length + from.getFileLength();
		fullHttpResponse.headers().set(CONNECTION, "keep-alive");
		if (contentLength > 0) {
			fullHttpResponse.headers().set(CONTENT_LENGTH, contentLength);
		}

		fullHttpResponse.headers().set(SERVER, "127.0.0.1:8081");
		return fullHttpResponse;
	}

	public static DefaultFullHttpResponse buildDefaultFullHttpResponse(Response response, HttpResponseStatus status) {
		final DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);

		if (response != null) {
			defaultFullHttpResponse.headers().add(CONTENT_LENGTH, "0");
			if (response.getCookies() != null) {
				defaultFullHttpResponse.headers().add(SET_COOKIE, response.getCookies().stream()
						.map(Cookie::toString)
						.collect(Collectors.toList()));
			}

			response.getHeaders().keySet().forEach(head -> defaultFullHttpResponse.headers().add(head, response.getHeaders().get(head)));
		}

		defaultFullHttpResponse.headers().set(SERVER, "127.0.0.1");
		return defaultFullHttpResponse;
	}

	public static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
		final DefaultFullHttpResponse response = buildDefaultFullHttpResponse(null, FOUND);
		response.headers().set(HttpHeaderNames.LOCATION, newUri);

		ctx.write(response).addListener(ChannelFutureListener.CLOSE);
	}

	public static void sendRedirect(Channel channel, String newUri) {
		final DefaultFullHttpResponse response = buildDefaultFullHttpResponse(null, FOUND);
		response.headers().set(HttpHeaderNames.LOCATION, newUri);

		channel.write(response).addListener(ChannelFutureListener.CLOSE);
	}

	public static boolean resource(String resource, ChannelHandlerContext ctx, FullHttpRequest request) {
		try (InputStream is = new ByteArrayInputStream(resource.getBytes())) {
			byte[] bytes = new byte[1024];
			int len;

			ByteBuf message = Unpooled.buffer();
			while ((len = is.read(bytes)) != -1) {
				message.writeBytes(bytes, 0, len);
			}

			final HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);
			httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

			write(httpResponse, ctx, request, message);

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static void write(final File file, final ChannelHandlerContext ctx, final FullHttpRequest request) throws IOException {
		final HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);

		final long fileLength = file.length();

		HttpUtil.setContentLength(httpResponse, fileLength);

		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));

		final Calendar time = new GregorianCalendar();
		time.add(Calendar.SECOND, 60);

		httpResponse.headers().set(HttpHeaderNames.DATE, FMT.format(time.getTime()))
				.set(HttpHeaderNames.EXPIRES, FMT.format(time.getTime()))
				.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=60")
				.set(HttpHeaderNames.LAST_MODIFIED, FMT.format(new Date(file.lastModified())));

		final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
		write(httpResponse, ctx, request, new DefaultFileRegion(randomAccessFile.getChannel(), 0, fileLength));
	}

	private static void write(final HttpResponse httpResponse, ChannelHandlerContext ctx, FullHttpRequest request, Object msg) {
		if (HttpUtil.isKeepAlive(request)) {
			httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}

		ctx.write(httpResponse);
		ctx.writeAndFlush(msg);

		ChannelFuture lastContentFuture = ctx
				.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
				.addListener(ChannelFutureListener.CLOSE);

		if (!HttpUtil.isKeepAlive(request)) {
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	public static HttpMethod toHttpMethod(MappingMethod mappingMethod) {
		return new HttpMethod(mappingMethod.name());
	}
}