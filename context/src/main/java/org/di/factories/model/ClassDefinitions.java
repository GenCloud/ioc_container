package org.di.factories.model;

import lombok.Data;
import org.di.context.analyze.enums.ClassStateInjection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author GenCloud
 * @date 09.09.2018
 */
@Data
public class ClassDefinitions {
    private ClassStateInjection stateInjection;

    private Object type;
    private String qualifiedName;
    private List<Class<?>> dependencies = new ArrayList<>();

    private boolean singleton;
    private boolean prototype;
    private boolean lazy;

    private boolean initialized;
}
