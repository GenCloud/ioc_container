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
package org.ioc.test.web;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.ioc.annotations.context.IoCDependency;
import org.ioc.annotations.web.DateFormatter;
import org.ioc.annotations.web.IoCController;
import org.ioc.annotations.web.Param;
import org.ioc.annotations.web.UrlMapping;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.processors.DestroyProcessor;
import org.ioc.context.sensible.ContextSensible;
import org.ioc.context.type.IoCContext;
import org.ioc.exceptions.IoCException;
import org.ioc.test.database.DatabaseComponent;
import org.ioc.test.database.entity.SampleEntity;
import org.ioc.web.model.ModelAndView;

import java.io.File;
import java.util.*;

import static org.ioc.annotations.web.MappingMethod.POST;

/**
 * @author GenCloud
 * @date 12.10.2018
 */
@IoCController
@UrlMapping
public class SampleController implements ContextSensible, DestroyProcessor {
	private final String home = "./file/";
	@IoCDependency
	private DatabaseComponent databaseComponent;

	private IoCContext context;

	/**
	 * Set the {@link IoCContext} to component.
	 *
	 * @param context initialized application contexts
	 * @throws IoCException throw if contexts throwing by methods
	 */
	@Override
	public void contextInform(IoCContext context) throws IoCException {
		this.context = context;
	}

	@UrlMapping(value = "/")
	public ModelAndView index() {
		final ModelAndView modelAndView = new ModelAndView();
		final File directory = new File(home);

		final List<SampleEntity> sampleEntities = databaseComponent.findAll();

		final List<TypeMetadata> converted = new ArrayList<>();

		Map<String, TypeMetadata> proto = context.getPrototypeFactory().getTypes();

		Map<String, TypeMetadata> sing = context.getSingletonFactory().getTypes();

		Map<String, TypeMetadata> req = context.getRequestFactory().getTypes();

		converted.addAll(proto.values());
		converted.addAll(sing.values());
		converted.addAll(req.values());

		modelAndView.addAttribute("types", converted);
		modelAndView.addAttribute("entities", sampleEntities);
		modelAndView.addAttribute("dir", directory);
		modelAndView.setView("index");

		return modelAndView;
	}

	@UrlMapping(value = "/date")
	public IMessage testDate(@DateFormatter("yyyy-MM-dd HH:mm") @Param("date") Date date) {
		return new IMessage(date.toString());
	}

	@UrlMapping(value = "/upload", method = POST)
	public IMessage upload(File file) {
		if (file == null) {
			return new IMessage(IMessage.Type.ERROR, "Can't upload");
		}

		File directory = new File(home);
		if (!directory.exists()) {
			directory.mkdir();
		}

		File newFile = new File(home + file.getName());
		file.renameTo(newFile);

		return new IMessage("Uploaded: " + file.getName());
	}

	@UrlMapping("/download")
	public File download(HttpRequest request) {
		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());

		return new File(home + decoder.path().substring(10));
	}

	@UrlMapping("/remove")
	public IMessage remove(@Param("name") String name) {
		File directory = new File(home);
		if (!directory.exists()) {
			return new IMessage(IMessage.Type.ERROR, "File don't exists");
		}

		File[] files = directory.listFiles((dir, filterName) -> name.equals(filterName));

		if (files == null || files.length == 0) {
			return new IMessage(IMessage.Type.ERROR, "File don't exists");
		}

		return files[0].delete() ? new IMessage("Deleted") : new IMessage(IMessage.Type.ERROR, "Delete error");
	}

	@UrlMapping("/clear")
	public IMessage clear() {
		File directory = new File(home);
		if (directory.exists()) {
			Arrays.stream(Objects.requireNonNull(directory.listFiles())).forEach(File::delete);
			directory.delete();
		}

		return new IMessage("Successful cleared");
	}

	@Override
	public void destroy() {
		databaseComponent.findAll().forEach(e -> databaseComponent.deleteSampleEntity(e));
	}
}
