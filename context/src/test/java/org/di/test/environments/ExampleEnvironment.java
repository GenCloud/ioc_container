package org.di.test.environments;

import org.di.annotations.property.Property;
import org.di.annotations.property.PropertyFunction;

import java.util.Arrays;

/**
 * @author GenCloud
 * @date 09.09.2018
 */
@Property(path = "configs/ExampleEnvironment.properties")
public class ExampleEnvironment extends SamplePropertyListener {

    private String nameApp;

    private String[] components;

    @PropertyFunction
    public Integer value() {
        return new Integer("158");
    }

    @Override
    public String toString() {
        return "ExampleEnvironment{hash: " + Integer.toHexString(hashCode()) + ", nameApp='" + nameApp + '\'' +
                ", components=" + Arrays.toString(components) +
                '}';
    }
}
