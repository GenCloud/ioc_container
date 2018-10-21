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
import org.ioc.web.security.configuration.SecurityConfigureAdapter;
import org.ioc.web.security.encoder.bcrypt.BCryptEncoder;
import org.ioc.web.security.user.UserDetails;

import java.util.Objects;

import static org.examples.webapp.responces.IMessage.Type.ERROR;
import static org.examples.webapp.responces.IMessage.Type.OK;

/**
 * @author GenCloud
 * @date 10/2018
 */
@IoCController
@UrlMapping("/")
public class MainController {
	@IoCDependency
	private AccountService accountService;

	@IoCDependency
	private BCryptEncoder bCryptEncoder;

	@IoCDependency
	private SecurityConfigureAdapter securityConfigureAdapter;

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
		if (username == null || username.isEmpty() || password == null || password.isEmpty()
				|| repeatedPassword == null || repeatedPassword.isEmpty()) {
			return new IMessage(ERROR, "Invalid request parameters!");
		}

		if (!Objects.equals(password, repeatedPassword)) {
			return new IMessage(ERROR, "Repeated password doesn't match!");
		}

		final UserDetails userDetails = accountService.loadUserByUsername(username);
		if (userDetails != null) {
			return new IMessage(ERROR, "Account already exists!");
		}

		final TblAccount account = new TblAccount();
		account.setUsername(username);
		account.setPassword(bCryptEncoder.encode(password));

		accountService.save(account);
		return new IMessage(OK, "Successfully created!");
	}

	@UrlMapping(value = "signin", method = MappingMethod.POST)
	public IMessage auth(Request request, @RequestParam("username") String username,
						 @RequestParam("password") String password) {
		if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
			return new IMessage(ERROR, "Invalid request parameters!");
		}

		final UserDetails userDetails = accountService.loadUserByUsername(username);
		if (userDetails == null) {
			return new IMessage(ERROR, "Account not found!");
		}

		if (!bCryptEncoder.match(password, userDetails.getPassword())) {
			return new IMessage(ERROR, "Password does not match!");
		}

		securityConfigureAdapter.getContext().authenticate(request, userDetails);
		return new IMessage(OK, "Successfully authenticated");
	}

	@UrlMapping("signout")
	public IMessage signout(Request request) {
		if (securityConfigureAdapter.getContext().removeAuthInformation(request)) {
			return new IMessage(OK, "/");
		}

		return new IMessage(ERROR, "Credentials not found or not authenticated!");
	}

	@UrlMapping("loginPage")
	public ModelAndView authenticated(@Credentials TblAccount account) {
		final ModelAndView modelAndView = new ModelAndView();
		modelAndView.setView("auth");

		modelAndView.addAttribute("account", account);
		return modelAndView;
	}
}
