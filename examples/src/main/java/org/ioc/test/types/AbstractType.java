package org.ioc.test.types;

import org.ioc.annotations.context.IoCDependency;
import org.ioc.test.environments.ExampleEnvironment;

/**
 * @author GenCloud
 * @date 11/2018
 */
public abstract class AbstractType {
	@IoCDependency
	public ExampleEnvironment exampleEnvironment;

	@Override
	public String toString() {
		return "LazyType{" +
				"exampleEnvironment=" + exampleEnvironment +
				'}';
	}
}
