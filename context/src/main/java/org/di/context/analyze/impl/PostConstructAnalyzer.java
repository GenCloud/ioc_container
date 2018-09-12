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
package org.di.context.analyze.impl;

import org.di.excepton.IoCException;
import org.di.factories.config.Analyzer;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Analyzer for detecting methods annotated with PostConstruct
 *
 * @author GenCloud
 * @date 12.09.2018
 * @see javax.annotation.PostConstruct
 */
public class PostConstructAnalyzer implements Analyzer<Boolean, Class<?>> {
    @Override
    public Boolean analyze(Class<?> tested) {
        final List<Method> methods = Arrays
                .stream(tested.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostConstruct.class))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            return false;
        }

        if (methods.size() > 1) {
            throw new IoCException("IoCError - Grammar error. The number of functions marked with annotation " +
                    "PostConstruct should not exceed more than one. Problem in [" + tested.getSimpleName() + "] class");
        }

        return true;
    }

    @Override
    public boolean supportFor(Class<?> tested) {
        return true;
    }
}
