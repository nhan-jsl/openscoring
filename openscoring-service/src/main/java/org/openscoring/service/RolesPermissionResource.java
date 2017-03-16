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
import org.apache.shiro.subject.Subject;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openscoring.common.SimpleResponse;
import org.openscoring.dao.HibernateUtil;
import org.openscoring.dao.model.RolesPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("role")
public class RolesPermissionResource {
	private static final transient Logger log = LoggerFactory.getLogger(RolesPermissionResource.class);

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleResponse addRoleAndPermission(RolesPermission rolesPermission) {
		Subject currentUser = SecurityUtils.getSubject();
		if (currentUser.isAuthenticated() && currentUser.hasRole("admin")) {
			SimpleResponse simpleResponse = new SimpleResponse();
			try {
				Session session = HibernateUtil.getSessionFactory().getCurrentSession();
				Transaction tx = session.beginTransaction();
				session.persist(rolesPermission);
				tx.commit();
				simpleResponse.setMessage("add role & permissions successfully");
				return simpleResponse;
			}
			catch (Exception e) {
				simpleResponse.setMessage("add role & permissions not successfully! Error: [" + e.getMessage() + "]");
				return simpleResponse;
			}
		} else {
			throw new NotAuthorizedException("Sorry, You don't have permission to view this content!");
		}
	}

}