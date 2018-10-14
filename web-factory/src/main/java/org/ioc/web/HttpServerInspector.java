package org.ioc.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.ioc.annotations.web.DateFormatter;
import org.ioc.annotations.web.MappingMethod;
import org.ioc.cache.ICache;
import org.ioc.utils.ReflectionUtils;
import org.ioc.web.engine.PageManager;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.ModelMap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class HttpServerInspector {
	private final FullHttpRequest request;
	private final ChannelHandlerContext ctx;
	private final HttpServerMapper mapping;
	private final QueryStringDecoder decoder;

	private HttpServerInspector(FullHttpRequest request, ChannelHandlerContext ctx, HttpServerMapper mapping,
								QueryStringDecoder decoder) {
		this.request = request;
		this.ctx = ctx;
		this.mapping = mapping;
		this.decoder = decoder;
	}

	public static HttpServerInspector inspectRequest(ChannelHandlerContext ctx, FullHttpRequest request,
													 ICache<MappingMethod, Map<String, HttpServerMapper>> mappingRequests) {
		final QueryStringDecoder decoder = new QueryStringDecoder(request.uri());

		final HttpServerMapper mapping = HttpServerMapper.findMapping(request, decoder.path(), mappingRequests);

		if (mapping != null) {
			return new HttpServerInspector(request, ctx, mapping, decoder);
		}

		String path = decoder.path();

		if (path.endsWith(".properties") || path.endsWith(".class")) {
			HttpServerUtil.sendError(ctx, NOT_FOUND);
			return null;
		}

		if (!HttpServerUtil.resource(path, ctx, request)) {
			HttpServerUtil.sendError(ctx, NOT_FOUND);
		}

		return null;
	}

	public Object invokeControllerMethod(ModelMap attribute) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
		final Map<String, List<String>> parameters = decoder.parameters();

		final List<File> fileCache = new ArrayList<>();
		final List<String> pathCache = new ArrayList<>();

		final Class<?>[] params = mapping.getMethod().getParameterTypes();
		final Object[] args = new Object[params.length];

		IntStream.range(0, params.length).forEach(i -> {
			final Class<?> finder = params[i];
			if (FullHttpRequest.class.isAssignableFrom(finder) || HttpRequest.class.isAssignableFrom(finder)
					|| HttpMessage.class.isAssignableFrom(finder) || HttpObject.class.isAssignableFrom(finder)) {
				args[i] = request;
			} else if (String.class.isAssignableFrom(finder)) {
				final String str = mapping.getName(i);
				final List<String> list = parameters.get(str);
				args[i] = HttpServerUtil.listToString(list);
			} else if (Date.class.isAssignableFrom(finder)) {
				final DateFormatter dateFormatter = ReflectionUtils.searchAnnotationInParameters(mapping.getMethod(), DateFormatter.class);
				final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormatter.value());
				final List<String> list = parameters.get(mapping.getName(i));
				final String p = list.get(0);
				try {
					Date parse = simpleDateFormat.parse(p);
					args[i] = parse;
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (File.class.isAssignableFrom(finder)) {
				final File file = HttpServerUtil.readFile(request);
				if (file != null) {
					fileCache.add(file);
					pathCache.add(file.getPath());
				}
				args[i] = file;
			} else if (ModelMap.class.isAssignableFrom(finder)) {
				args[i] = attribute;
			} else {
				args[i] = null;
			}
		});

		final Object result = mapping.getMethod().invoke(mapping.getInstance(), args);
		for (int i = 0; i < fileCache.size(); i++) {
			if (fileCache.get(i).getPath().equals(pathCache.get(i))) {
				fileCache.get(i).delete();
			}
		}

		return result;
	}

	public void writeResponse(ModelMap map, Object result, PageManager engine) throws IOException {
		final Class<?> resultType = mapping.getMethod().getReturnType();

		if (result == null) {
			return;
		}

		if (File.class.isAssignableFrom(resultType)) {
			HttpServerUtil.write((File) result, ctx, request);
			return;
		}

		if (ModelAndView.class.isAssignableFrom(resultType)) {
			result = engine.resolveArguments(((ModelAndView) result).getView(), (ModelMap) result);

			result = result.toString()
					.replace("\t", "")
					.replace("\r", "")
					.replace("\n", "");

			final FullHttpResponse fullResponse = new DefaultFullHttpResponse(HTTP_1_1, OK);

			final ByteBuf buffer = Unpooled.copiedBuffer(result.toString(), CharsetUtil.UTF_8);
			fullResponse.content().writeBytes(buffer);
			buffer.release();

			fullResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
			ctx.writeAndFlush(fullResponse).addListener(ChannelFutureListener.CLOSE);
			return;
		}

		if (Object.class.isAssignableFrom(resultType)) {
			HttpServerUtil.writeJson(result, ctx, request);
		}
	}
}