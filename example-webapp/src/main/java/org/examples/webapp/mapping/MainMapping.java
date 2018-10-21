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
package org.examples.webapp.mapping;

import org.examples.webapp.domain.entity.TblAccount;
import org.examples.webapp.responces.IMessage;
import org.examples.webapp.service.AccountService;
import org.ioc.annotations.context.IoCDependency;
import org.ioc.annotations.web.IoCController;
import org.ioc.web.annotations.Credentials;
import org.ioc.web.annotations.MappingMethod;
import org.ioc.web.annotations.RequestParam;
import org.ioc.web.annotations.UrlMapping;
import org.ioc.web.model.ModelAndView;
import org.ioc.web.model.http.Request;

/**
 * @author GenCloud
 * @date 10/2018
 */
@IoCController
@UrlMapping("/")
public class MainMapping {
	@IoCDependency
	private AccountService accountService;

	@UrlMapping
	public ModelAndView index() {
		final ModelAndView modelAndView = new ModelAndView();
		modelAndView.setView("index");
		return modelAndView;
	}

	@UrlMapping(value = "signup", method = MappingMethod.POST)
	public IMessage createUser(@RequestParam("username") String username,
							   @RequestParam("password") String password,
							   @RequestParam("repeatedPassword") String repeatedPassword) {
		return accountService.tryCreateUser(username, password, repeatedPassword);
	}

	@UrlMapping(value = "signin", method = MappingMethod.POST)
	public IMessage auth(Request request,
						 @RequestParam("username") String username,
						 @RequestParam("password") String password) {
		return accountService.tryAuthenticateUser(request, username, password);
	}

	@UrlMapping("signout")
	public IMessage signout(Request request) {
		return accountService.logout(request);
	}

	@UrlMapping("loginPage")
	public ModelAndView authenticated(@Credentials TblAccount account) {
		final ModelAndView modelAndView = new ModelAndView();
		modelAndView.setView("auth");

		modelAndView.addAttribute("account", account);
		return modelAndView;
	}
}
