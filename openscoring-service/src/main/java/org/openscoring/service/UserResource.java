/*
 * Copyright (c) 2013 Villu Ruusmann
 *
 * This file is part of Openscoring
 *
 * Openscoring is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Openscoring is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Openscoring.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openscoring.service;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.openscoring.common.SimpleResponse;
import org.openscoring.common.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("user")
public class UserResource {
	private static final transient Logger log = LoggerFactory.getLogger(UserResource.class);

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleResponse authenticate(UserResponse user){
		SimpleResponse simpleResponse = new SimpleResponse();

		Subject currentUser = SecurityUtils.getSubject();
		if (!currentUser.isAuthenticated()) {
			UsernamePasswordToken token = new UsernamePasswordToken(user.getUsername(), user.getPassword());
			try {
				currentUser.login(token);
				simpleResponse.setMessage("Welcome " + token.getPrincipal());
			} catch (UnknownAccountException uae) {
				simpleResponse.setMessage("There is no user with username of " + token.getPrincipal());
			} catch (IncorrectCredentialsException ice) {
				simpleResponse.setMessage("Password for account " + token.getPrincipal() + " was incorrect!");
			} catch (LockedAccountException lae) {
				simpleResponse.setMessage("The account for username " + token.getPrincipal() + " is locked.  " +
						"Please contact your administrator to unlock it.");
			}
			// ... catch more exceptions here (maybe custom ones specific to your application?
			catch (AuthenticationException ae) {
				//unexpected condition?  error?
				simpleResponse.setMessage("Authenticate fail...");
			}
		} else {
			simpleResponse.setMessage("Hi " + user.getUsername() + ", you had been authenticated!");
		}
		return simpleResponse;
	}

	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleResponse query(UserResponse user){
		SimpleResponse simpleResponse = new SimpleResponse();
		Subject currentUser = SecurityUtils.getSubject();
		if (!currentUser.isAuthenticated()) {
			simpleResponse.setMessage("You not yet login");
		}
		else {
			currentUser.logout();
			simpleResponse.setMessage("Logout successfully");
		}

		return simpleResponse;
	}

}