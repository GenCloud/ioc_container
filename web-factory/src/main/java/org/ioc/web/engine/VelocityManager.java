package org.ioc.web.engine;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.ioc.enviroment.configurations.web.WebAutoConfiguration;
import org.ioc.web.model.ModelMap;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class VelocityManager implements PageManager {
	private VelocityEngine velocityEngine = new VelocityEngine();

	public VelocityManager(WebAutoConfiguration webAutoConfiguration) {
		velocityEngine.setProperty("input.encoding", webAutoConfiguration.getVelocityInputEncoding());
		velocityEngine.setProperty("output.encoding", webAutoConfiguration.getVelocityOutputEncoding());

		velocityEngine.setProperty("resource.loader", webAutoConfiguration.getVelocityResourceLoader());
		velocityEngine.setProperty("file.resource.loader.class", webAutoConfiguration.getVelocityResourceLoaderClass());
		velocityEngine.setProperty("file.resource.loader.path", webAutoConfiguration.getVelocityResourceLoadingPath());

		velocityEngine.init();
	}

	@Override
	public String resolveArguments(String page, ModelMap map) {
		final Template template = velocityEngine.getTemplate("template/" + page + ".vm");

		final StringWriter stringWriter = new StringWriter();
		final VelocityContext velocityContext = new VelocityContext(map.getMap());

		template.merge(velocityContext, stringWriter);

		try {
			stringWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringWriter.toString();
	}
}