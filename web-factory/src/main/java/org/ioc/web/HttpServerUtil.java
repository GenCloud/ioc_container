package org.ioc.web;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class HttpServerUtil {
	private static final String HTTP_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss zzz";

	private static final int HTTP_CACHE_SECONDS = 60;

	@SuppressWarnings("unused")
	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

	private static final SimpleDateFormat FMT = new SimpleDateFormat(HTTP_DATE_FORMAT);

	@SuppressWarnings("unused")
	public static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
		response.headers().set(HttpHeaderNames.LOCATION, newUri);

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
				Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	@SuppressWarnings("unused")
	public static void sendNotModified(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);

		Calendar time = new GregorianCalendar();
		response.headers().set(HttpHeaderNames.DATE, FMT.format(time.getTime()));

		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
		Calendar time = new GregorianCalendar();
		time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);

		response.headers().set(HttpHeaderNames.DATE, FMT.format(time.getTime()))
				.set(HttpHeaderNames.EXPIRES, FMT.format(time.getTime()))
				.set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS)
				.set(HttpHeaderNames.LAST_MODIFIED, FMT.format(new Date(fileToCache.lastModified())));
	}

	private static void setContentTypeHeader(HttpResponse response, File file) {
		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
	}

	static String listToString(List<String> list) {
		if (list == null || list.size() == 0) {
			return null;
		}

		if (list.size() == 1) {
			return list.get(0);
		}

		return String.join(",", list);
	}

	static File readFile(HttpRequest request) {
		HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
		HttpContent chunk = (HttpContent) request;
		HttpPostMultipartRequestDecoder decoder;
		File file;

		try {
			decoder = new HttpPostMultipartRequestDecoder(factory, request);
			decoder.setDiscardThreshold(0);
		} catch (ErrorDataDecoderException e) {
			e.printStackTrace();
			return null;
		}

		decoder.offer(chunk);

		file = readChunk(decoder);

		return file;
	}

	private static File readChunk(HttpPostMultipartRequestDecoder decoder) {
		OutputStream os = null;
		while (decoder.hasNext()) {
			InterfaceHttpData data = decoder.next();
			if (data != null) {
				try {
					switch (data.getHttpDataType()) {
						case Attribute:
							break;
						case FileUpload:
							final FileUpload fileUpload = (FileUpload) data;
							final ByteBuf buf = fileUpload.getByteBuf();

							final byte[] bytes = new byte[buf.readableBytes()];
							if (bytes.length == 0) {
								return null;
							}

							final File file = new File(SystemPropertyUtil.get("user.dir") + File.separator + UUID.randomUUID());
							os = new FileOutputStream(file);

							buf.readBytes(bytes);
							os.write(bytes);
							return file;
					}
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				} finally {
					close(os);
					data.release();
				}
			}
		}

		return null;
	}

	static void write(final File file, final ChannelHandlerContext ctx, final FullHttpRequest request) throws IOException {
		final HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);

		final RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException ignore) {
			sendError(ctx, NOT_FOUND);
			return;
		}

		final long fileLength = raf.length();

		HttpUtil.setContentLength(httpResponse, fileLength);
		setContentTypeHeader(httpResponse, file);
		setDateAndCacheHeaders(httpResponse, file);

		write(httpResponse, ctx, request, new DefaultFileRegion(raf.getChannel(), 0, fileLength));
	}

	/**
	 * Find and static resources to client.
	 *
	 * @param path    path to res
	 * @param ctx     channel context
	 * @param request http request
	 * @return bool value
	 */
	static boolean resource(String path, ChannelHandlerContext ctx, FullHttpRequest request) {
		InputStream is = null;

		String workingDirectory = "./site/template";

		try {
			final Path p = Paths.get(workingDirectory + path);
			is = new FileInputStream(p.toFile());

			byte[] bytes = new byte[1024];
			int len;

			ByteBuf message = Unpooled.buffer();
			while ((len = is.read(bytes)) != -1) {
				message.writeBytes(bytes, 0, len);
			}

			final HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);
			if (path.endsWith(".html") || path.endsWith(".js")) {
				httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
			}

			write(httpResponse, ctx, request, message);

			return true;
		} catch (IOException e) {
			return false;
		} finally {
			close(is);
		}
	}

	static void writeJson(Object object, ChannelHandlerContext ctx, FullHttpRequest request) {
		final String json = new Gson().toJson(object);

		ByteBuf message = Unpooled.buffer();
		message.writeBytes(json.getBytes(), 0, json.length());

		final HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);

		httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

		write(httpResponse, ctx, request, message);
	}

	private static void write(final HttpResponse httpResponse, final ChannelHandlerContext ctx, final FullHttpRequest request, final Object msg) {
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

	private static void close(Object obj) {
		if (obj == null) {
			return;
		}

		try {
			Method method = obj.getClass().getMethod("close");
			if (method != null) {
				method.invoke(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}