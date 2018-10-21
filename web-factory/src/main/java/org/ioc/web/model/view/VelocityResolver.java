/*
 * Copyright (c) 2018 DI (IoC) Container (Team: GC Dev, Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of DI (IoC) Container Project.
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
package org.ioc.web.model.view;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.ioc.enviroment.configurations.web.WebAutoConfiguration;
import org.ioc.web.model.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;

/**
 * @author GenCloud
 * @date 10/2018
 */
public class VelocityResolver implements TemplateResolver {
	private final WebAutoConfiguration webAutoConfiguration;
	private VelocityEngine velocityEngine = new VelocityEngine();

	public VelocityResolver(WebAutoConfiguration webAutoConfiguration) {
		this.webAutoConfiguration = webAutoConfiguration;

		velocityEngine.setProperty("input.encoding", webAutoConfiguration.getVelocityInputEncoding());
		velocityEngine.setProperty("output.encoding", webAutoConfiguration.getVelocityOutputEncoding());

		velocityEngine.setProperty("resource.loader", webAutoConfiguration.getVelocityResourceLoader());
		velocityEngine.setProperty("file.resource.loader.class", webAutoConfiguration.getVelocityResourceLoaderClass());
		velocityEngine.setProperty("file.resource.loader.path", webAutoConfiguration.getVelocityResourceLoadingPath());

		velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
		velocityEngine.init();
	}

	@Override
	public String resolveArguments(String page, ModelAndView map) {
		final Template template = velocityEngine.getTemplate(page + ".vm");
		final StringWriter writer = new StringWriter();
		final VelocityContext velocityContext = new VelocityContext(map.getMap());

		template.merge(velocityContext, writer);
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer.toString();
	}

	@Override
	public String findResource(String path) throws Exception {
		final Template template = velocityEngine.getTemplate(path);

		final File file = Paths.get(webAutoConfiguration.getVelocityResourceLoadingPath() + "/" + template.getName()).toFile();

		if (file != null) {
			return FileUtils.readFileToString(file, "UTF-8");
		}

		return null;
	}
}