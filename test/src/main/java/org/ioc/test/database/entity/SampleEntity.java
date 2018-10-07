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
package org.ioc.test.database.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.CascadeType.ALL;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Entity
@Table(name = "sample_entity")
@NamedQuery(name = "SampleEntity.findById", query = "select from sample_entity where id = :id")
public class SampleEntity implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private long id;

	@Column(name = "name")
	private String name;

	@Column(name = "year")
	private String year;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL)
	private OneToOneEntity oneToOneEntity;

	@OneToMany(fetch = FetchType.LAZY, cascade = ALL)
	private List<ChildEntity> childEntities = new ArrayList<>();

	public long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public List<ChildEntity> getChildEntities() {
		return childEntities;
	}

	public void setChildEntities(List<ChildEntity> childEntities) {
		this.childEntities = childEntities;
	}

	public OneToOneEntity getOneToOneEntity() {
		return oneToOneEntity;
	}

	public void setOneToOneEntity(OneToOneEntity oneToOneEntity) {
		this.oneToOneEntity = oneToOneEntity;
	}

	@Override
	public String toString() {
		return "SampleEntity{" +
				"id=" + id +
				", name='" + name + '\'' +
				", year='" + year + '\'' +
				'}';
	}
}
