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
import org.di.context.contexts.AppContext;
import org.di.context.contexts.sensibles.ContextSensible;
import org.di.context.excepton.IoCException;

/**
 * @author GenCloud
 * @date 13.09.2018
 */
@IoCComponent
public class SensibleContextComponent implements ContextSensible {
    private AppContext appContext;

    @Override
    public void contextInform(AppContext appContext) throws IoCException {
        this.appContext = appContext;
    }

    @Override
    public String toString() {
        return "SensibleContextComponent{" +
                "appContext=" + appContext +
                '}';
    }
}
