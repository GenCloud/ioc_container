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
package org.ioc.aop;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.ioc.aop.advice.AfterAdvice;
import org.ioc.aop.advice.AroundAdivice;
import org.ioc.aop.advice.BeforeAdvice;
import org.ioc.aop.advice.ThrowingAdvice;
import org.ioc.aop.interceptor.Interceptor;
import org.ioc.context.type.IoCContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import static org.ioc.utils.ReflectionUtils.installAspect;

/**
 * @author GenCloud
 * @date 09/2018
 */
public class DynamicProxy implements InvocationHandler, MethodInterceptor {
	private final Object target;
	private final List<Class<?>> aspectList;
	private final IoCContext context;

	private DynamicProxy(IoCContext context, Object instance, List<Class<?>> aspectList) {
		this.context = context;
		this.target = instance;
		this.aspectList = aspectList;
	}

	public static Object newProxyInstance(IoCContext context, Object instance, List<Class<?>> aspectList) {
		final DynamicProxy proxy = new DynamicProxy(context, instance, aspectList);
		final Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(instance.getClass());
		enhancer.setCallback(proxy);
		enhancer.setCallbackFilter(m -> m.isBridge() ? 1 : 0);
		return enhancer.create();
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		final List<Interceptor> interceptors = installAspect(context, method, aspectList);

		final JunctionDot point = new JunctionDot();
		point.setArgs(args);
		point.setMethod(method);
		point.setProxy(proxy);
		point.setTarget(target);

		if (interceptors == null || interceptors.size() == 0) {
			point.process();
		} else {
			interceptors
					.stream()
					.filter(i -> BeforeAdvice.class.isAssignableFrom(i.getClass()))
					.forEach(i -> i.intercept(point));

			interceptors
					.stream()
					.filter(i -> AroundAdivice.class.isAssignableFrom(i.getClass()))
					.forEach(i -> i.intercept(point));

			point.process();

			if (point.getException() != null) {
				interceptors
						.stream()
						.filter(i -> ThrowingAdvice.class.isAssignableFrom(i.getClass()))
						.forEach(i -> i.intercept(point));
			} else {
				interceptors
						.stream()
						.filter(i -> AfterAdvice.class.isAssignableFrom(i.getClass()))
						.forEach(i -> i.intercept(point));
			}
		}
		return point.getReturnValue();
	}

	@Override
	public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) {
		return invoke(o, method, objects);
	}
}
