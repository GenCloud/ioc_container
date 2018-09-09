package org.di.test.environments;

import org.di.annotations.property.Property;

import java.util.Arrays;

/**
 * @author GenCloud
 * @date 09.09.2018
 */
@Property(path = "configs/ExampleEnvironment.properties")
public class ExampleEnvironment {
    private String nameApp;

    private String[] components;

    @Override
    public String toString() {
        return "ExampleEnvironment{hash: " + Integer.toHexString(hashCode()) + ", nameApp='" + nameApp + '\'' +
                ", components=" + Arrays.toString(components) +
                '}';
    }
}
