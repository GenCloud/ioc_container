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
package org.examples.webapp.service;

import org.examples.webapp.domain.entity.TblAccount;
import org.examples.webapp.domain.repository.TblAccountRepository;
import org.ioc.annotations.context.IoCComponent;
import org.ioc.annotations.context.IoCDependency;
import org.ioc.web.security.user.UserDetails;
import org.ioc.web.security.user.UserDetailsProcessor;

/**
 * @author GenCloud
 * @date 10/2018
 */
@IoCComponent
public class AccountService implements UserDetailsProcessor {
	@IoCDependency
	private TblAccountRepository tblAccountRepository;

	@Override
	public UserDetails loadUserByUsername(String username) {
		return tblAccountRepository.findByUsernameEq(username);
	}

	public void save(TblAccount tblAccount) {
		tblAccountRepository.save(tblAccount);
	}

	public void delete(TblAccount tblAccount) {
		tblAccountRepository.delete(tblAccount);
	}
}
