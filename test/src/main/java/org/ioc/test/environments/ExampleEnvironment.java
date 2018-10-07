package org.ioc.test.environments;

import org.ioc.annotations.configuration.Property;
import org.ioc.annotations.configuration.PropertyFunction;

import java.util.Arrays;

/**
 * @author GenCloud
 * @date 09/2018
 */
@Property
public class ExampleEnvironment extends SampleEnvironmentFact {

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
