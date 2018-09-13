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
package org.di.context.contexts.analyze.impl;

import org.di.context.contexts.analyze.results.SensibleInspectionResult;
import org.di.context.contexts.resolvers.Sensible;
import org.di.context.contexts.resolvers.sensibles.ContextSensible;
import org.di.context.factories.config.Inspector;

import java.util.ArrayList;
import java.util.List;

import static org.di.context.contexts.analyze.results.SensibleInspectionResult.SENSIBLE_CONTEXT_INJECTION;
import static org.di.context.contexts.analyze.results.SensibleInspectionResult.SENSIBLE_NOTHING;

/**
 * Inspector for find inheritance of superinterface {@link Sensible} and return type of injection.
 *
 * @author GenCloud
 * @date 13.09.2018
 */
public class SensibleInjectInspector implements Inspector<List<SensibleInspectionResult>, Object> {
    @Override
    public List<SensibleInspectionResult> inspect(Object tested) {
        final List<SensibleInspectionResult> results = new ArrayList<>();
        results.add(SENSIBLE_NOTHING);
        if (ContextSensible.class.isAssignableFrom(tested.getClass())) {
            results.add(SENSIBLE_CONTEXT_INJECTION);
        }
        return results;
    }

    @Override
    public boolean supportFor(Object tested) {
        return true;
    }
}
