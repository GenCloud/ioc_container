/*
 * Copyright (c) 2018 IoC Starter (Owner: Maxim Ivanov) authors and/or its affiliates. All rights reserved.
 *
 * This file is part of IoC Starter Project.
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
package org.ioc.test.database;

import org.ioc.annotations.context.IoCComponent;
import org.ioc.annotations.context.IoCDependency;
import org.ioc.test.database.entity.ChildEntity;
import org.ioc.test.database.entity.OneToOneEntity;
import org.ioc.test.database.entity.SampleEntity;
import org.ioc.test.database.repository.ChildEntityRepository;
import org.ioc.test.database.repository.OneToOneEntityRepository;
import org.ioc.test.database.repository.SampleEntityRepository;

import java.util.List;

/**
 * @author GenCloud
 * @date 10/2018
 */
@IoCComponent
public class DatabaseComponent {
	@IoCDependency
	private SampleEntityRepository sampleEntityRepository;

	@IoCDependency
	private OneToOneEntityRepository oneToOneEntityRepository;

	@IoCDependency
	private ChildEntityRepository childEntityRepository;

	public void saveOneToOneEntity(OneToOneEntity oneToOneEntity) {
		oneToOneEntityRepository.save(oneToOneEntity);
	}

	public void saveChildEntity(ChildEntity childEntity) {
		childEntityRepository.save(childEntity);
	}

	public void saveSampleEntity(SampleEntity sampleEntity) {
		sampleEntityRepository.save(sampleEntity);
	}

	public SampleEntity findSampleEntity(long id) {
		return sampleEntityRepository.fetch(id);
	}

	public SampleEntity findByNamedQuery(long id) {
		return sampleEntityRepository.namedQuery(id);
	}

	public SampleEntity findSampleEntityByName(String name) {
		return sampleEntityRepository.findByNameEqAndYearEq(name, "2018");
	}

	public List<SampleEntity> findAllByName(String name) {
		return sampleEntityRepository.findByNameEq(name);
	}

	public List<SampleEntity> findAll() {
		return sampleEntityRepository.fetchAll();
	}

	public void deleteSampleEntity(SampleEntity sampleEntity) {
		sampleEntityRepository.delete(sampleEntity);
	}
}
