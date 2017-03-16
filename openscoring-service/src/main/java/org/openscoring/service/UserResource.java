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
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.subject.Subject;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openscoring.common.SimpleResponse;
import org.openscoring.common.UserResponse;
import org.openscoring.dao.HibernateUtil;
import org.openscoring.dao.model.User;
import org.openscoring.dao.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("user")
public class UserResource {
	private static final transient Logger log = LoggerFactory.getLogger(UserResource.class);

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleResponse register(UserResponse suppliedUser){
		Subject currentUser = SecurityUtils.getSubject();
		if (currentUser.isAuthenticated() && currentUser.hasRole("admin")) {
			SimpleResponse simpleResponse = new SimpleResponse();
			try {
				User user = new User();
				user.setUsername(suppliedUser.getUsername());
				user.setOrgId(suppliedUser.getOrgId());
				// below method will set password & salt for this User obj
				generatePassword(user, suppliedUser.getPassword());

				Session session = HibernateUtil.getSessionFactory().getCurrentSession();
				Transaction tx = session.beginTransaction();
				session.save(user);
				tx.commit();

				simpleResponse.setMessage("create user successfully");
				return simpleResponse;
			}
			catch (Exception e) {
				simpleResponse.setMessage("create user not successfully! Error: [" + e.getMessage() + "]");
				return simpleResponse;
			}
		} else {
			throw new NotAuthorizedException("Sorry, only admin user can add user! Contact administrator please!");
		}
	}

	@PUT
	@Path("/role")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleResponse addRole(UserRole userRole) {
		Subject currentUser = SecurityUtils.getSubject();
		if (currentUser.isAuthenticated() && currentUser.hasRole("admin")) {
			SimpleResponse simpleResponse = new SimpleResponse();
			try {
				Session session = HibernateUtil.getSessionFactory().getCurrentSession();
				Transaction tx = session.beginTransaction();
				session.persist(userRole);
				tx.commit();
				simpleResponse.setMessage("add role [" + userRole.getRoleName() + "] to user [" + userRole.getUsername() + "] successfully");
				return simpleResponse;
			}
			catch (Exception e) {
				simpleResponse.setMessage("add role not successfully! Error: [" + e.getMessage() + "]");
				return simpleResponse;
			}
		} else {
			throw new NotAuthorizedException("Sorry, only admin user can add role to user! Contact administrator please!");
		}
	}

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

	private void generatePassword(User user, String plainTextPassword) {
		RandomNumberGenerator rng = new SecureRandomNumberGenerator();
		Object salt = rng.nextBytes();

		// Now hash the plain-text password with the random salt and multiple
		// iterations and then Base64-encode the value (requires less space than Hex):
		String hashedPasswordBase64 = new Sha256Hash(plainTextPassword, salt,1024).toBase64();

		user.setPassword(hashedPasswordBase64);
		user.setSalt(salt.toString());
	}

}