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
package org.ioc.web.handler.metadata.request;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpRequest;
import org.ioc.annotations.web.MappingMethod;
import org.ioc.web.handler.HttpRequestHeaders;
import org.ioc.web.handler.IHttpRequest;
import org.ioc.web.handler.metadata.ProtocolType;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class NettyHttpRequest implements IHttpRequest {
	private final HttpRequest nettyRequest;

	public NettyHttpRequest(HttpRequest nettyRequest) {
		this.nettyRequest = nettyRequest;
	}

	@Override
	public HttpRequestHeaders headers() {
		return null;
	}

	@Override
	public ProtocolType protocolType() {
		return null;
	}

	@Override
	public String path() {
		return null;
	}

	@Override
	public MappingMethod method() {
		return null;
	}

	@Override
	public ByteBuf content() {
		return null;
	}
}
