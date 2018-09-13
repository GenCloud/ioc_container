package org.di.test.environments;

import org.di.context.annotations.property.Property;
import org.di.context.annotations.property.PropertyFunction;

import java.util.Arrays;

/**
 * @author GenCloud
 * @date 09.09.2018
 */
@Property
public class ExampleEnvironment extends SamplePropertyListener {

    private String nameApp;

    private String[] components;

    @PropertyFunction
    public SampleProperty value() {
        return new SampleProperty(158);
    }

    @Override
    public String toString() {
        return "ExampleEnvironment{hash: " + Integer.toHexString(hashCode()) + ", nameApp='" + nameApp + '\'' +
                ", components=" + Arrays.toString(components) +
                '}';
    }
}
