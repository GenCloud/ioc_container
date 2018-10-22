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
package org.ioc.test.database.entity;

import javax.persistence.*;

import static javax.persistence.CascadeType.ALL;

/**
 * @author GenCloud
 * @date 10/2018
 */
@Entity
public class OneToOneEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private long id;

	@OneToOne(fetch = FetchType.LAZY, cascade = ALL)
	private SampleEntity sampleEntity;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public SampleEntity getSampleEntity() {
		return sampleEntity;
	}

	public void setSampleEntity(SampleEntity sampleEntity) {
		this.sampleEntity = sampleEntity;
	}

	@Override
	public String toString() {
		return "OneToOneEntity{" +
				"id=" + id +
				'}';
	}
}