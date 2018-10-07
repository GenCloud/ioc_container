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
package org.ioc.test.aop;

import org.ioc.aop.JunctionDot;
import org.ioc.aop.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GenCloud
 * @date 09/2018
 */
@IoCAspect
public class AopTest {
	private static final Logger log = LoggerFactory.getLogger(AopTest.class);

	@PointCut("exec(void org.ioc.test.types.TypeB.init2(String))")
	public void testPostAop() {

	}

	@BeforeInvocation("testPostAop()")
	public void testBefore(JunctionDot junctionDot) {
		log.info("Before: Evaluate point - [{}]", junctionDot);
	}

	@AfterInvocation("testPostAop()")
	public void testAfter(JunctionDot junctionDot) {
		log.info("After: Evaluate point - [{}]", junctionDot);
	}

	@AroundExecution("testPostAop()")
	public void testAround(JunctionDot junctionDot) {
		log.info("Around: Evaluate point - [{}]", junctionDot);
	}
}
