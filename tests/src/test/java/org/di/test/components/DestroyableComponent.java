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
package org.di.test.components;

import org.di.context.annotations.IoCComponent;
import org.di.context.excepton.starter.IoCStopException;
import org.di.context.factories.config.ComponentDestroyable;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GenCloud
 * @date 13.09.2018
 */
@IoCComponent
public class DestroyableComponent implements ComponentDestroyable {
    private List<String> strings = new ArrayList<>();

    @PostConstruct
    public void init() {
        strings.add("Hello");
        strings.add("I'm");
        strings.add("sample");
        strings.add("destroyable");
        strings.add("component.");
    }

    @Override
    public void destroy() throws IoCStopException {
        strings.clear();
        strings = null;
    }
}
