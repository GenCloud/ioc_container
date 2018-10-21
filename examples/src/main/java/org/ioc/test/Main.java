/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This addView is part of IoC Starter Project.
 *
 * IoC Starter Project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IoC Starter Project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IoC Starter Project.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ioc.test;

import org.ioc.annotations.context.ScanPackage;
import org.ioc.annotations.modules.CacheModule;
import org.ioc.annotations.modules.DatabaseModule;
import org.ioc.annotations.modules.ThreadingModule;
import org.ioc.annotations.modules.WebModule;
import org.ioc.context.DefaultIoCContext;
import org.ioc.context.model.TypeMetadata;
import org.ioc.context.starter.IoCStarter;
import org.ioc.test.cache.CacheComponentTest;
import org.ioc.test.database.DatabaseComponent;
import org.ioc.test.database.entity.ChildEntity;
import org.ioc.test.database.entity.OneToOneEntity;
import org.ioc.test.database.entity.SampleEntity;
import org.ioc.test.database.entity.User;
import org.ioc.test.types.TypeB;
import org.ioc.web.factory.HttpInitializerFactory;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author GenCloud
 * @date 09/2018
 */
@WebModule
@DatabaseModule
@ThreadingModule
@CacheModule
@ScanPackage(packages = {"org.ioc.test"})
public class Main extends Assert {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	private static DefaultIoCContext context;

	public static void main(String[] args) {
		context = IoCStarter.start(Main.class);

		log.info("Initializing singleton types - {}", context.getSingletonFactory().getTypes().size());
		log.info("Initializing proto types - {}", context.getPrototypeFactory().getTypes().size());

		log.info("For Each singleton types");
		for (TypeMetadata t : context.getSingletonFactory().getTypes().values()) {
			log.info("------- {}", t.toString());
		}

		log.info("For Each proto types");
		for (TypeMetadata t : context.getPrototypeFactory().getTypes().values()) {
			log.info("------- {}", t.toString());
		}

		t02_testAOP();

		t03_testCaching();

		t04_testOrm();

		t05_testStartWeb();
	}

	private static void t02_testAOP() {
		log.info("Getting TypeB from contexts");
		final TypeB typeB = context.getType(TypeB.class);
		typeB.init2("Simple argument for AOP point's");
	}

	private static void t03_testCaching() {
		log.info("Getting CacheComponentTest from contexts");
		final CacheComponentTest cacheComponentTest = context.getType(CacheComponentTest.class);
		cacheComponentTest.initializeCache();
		assertNotNull(cacheComponentTest);
		log.info(cacheComponentTest.toString());

		cacheComponentTest.getElement("1");
		cacheComponentTest.getElement("2");
		cacheComponentTest.getElement("3");
		cacheComponentTest.getElement("4");

		cacheComponentTest.removeElement("1");
		cacheComponentTest.removeElement("2");

		cacheComponentTest.invalidate();
	}

	private static void t04_testOrm() {
		final DatabaseComponent databaseComponent = context.getType(DatabaseComponent.class);

		log.info("Inserting test dataContainer into Schema");
		final SampleEntity sampleEntity = new SampleEntity();
		sampleEntity.setName("sample27");
		sampleEntity.setYear("2018");

		final OneToOneEntity oneToOneEntity = new OneToOneEntity();
		sampleEntity.setOneToOneEntity(oneToOneEntity);
		oneToOneEntity.setSampleEntity(sampleEntity);
		databaseComponent.saveOneToOneEntity(oneToOneEntity);

		final ChildEntity childEntity = new ChildEntity();
		childEntity.setName("child5");
		childEntity.setSampleEntity(sampleEntity);
		databaseComponent.saveChildEntity(childEntity);

		sampleEntity.getChildEntities().add(childEntity);
		databaseComponent.saveSampleEntity(sampleEntity);

		final SampleEntity sampleEntity1 = new SampleEntity();
		sampleEntity1.setName("sample28");
		sampleEntity1.setYear("2018");
		databaseComponent.saveSampleEntity(sampleEntity1);

		final SampleEntity sampleEntity2 = new SampleEntity();
		sampleEntity2.setName("sample29");
		sampleEntity2.setYear("2018");
		databaseComponent.saveSampleEntity(sampleEntity2);

		final SampleEntity sampleEntity3 = new SampleEntity();
		sampleEntity3.setName("sample30");
		sampleEntity3.setYear("2018");
		databaseComponent.saveSampleEntity(sampleEntity3);

		log.info("Fetch test data from Schema by generated query");
		final SampleEntity get0 = databaseComponent.findSampleEntityByName("sample28");
		assertNotNull(get0);
		log.info(get0.toString());

		log.info("Fetch test data from Schema by named query");
		final SampleEntity customQuery = databaseComponent.findByNamedQuery(sampleEntity.getId());
		assertNotNull(customQuery);
		log.info(customQuery.toString());

		log.info("Fetch all test data from Schema");
		final List<SampleEntity> get1 = databaseComponent.findAll();
		assertNotNull(get1);
		log.info(get1.toString());

		log.info("Fetch all test data from Schema by generated query");
		final List<SampleEntity> sampleEntityList = databaseComponent.findAllByName("sample28");
		assertNotNull(sampleEntityList);
		log.info(sampleEntityList.toString());

		final User user = new User();
		user.setUsername("admin");
		user.setPassword("admin");
		databaseComponent.saveUser(user);

		final User u = databaseComponent.findUserByName("admin");
		assertNotNull(u);
	}

	private static void t05_testStartWeb() {
		final HttpInitializerFactory httpInitializerFactory = context.getType(HttpInitializerFactory.class);
		try {
			httpInitializerFactory.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
