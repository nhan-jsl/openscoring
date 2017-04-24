/*
 * Copyright (c) 2014 Villu Ruusmann
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
package org.openscoring.client;

import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.openscoring.common.SimpleResponse;
import org.openscoring.common.UserResponse;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

abstract
public class ModelApplication extends Application {

	@Parameter (
		names = {"--model"},
		description = "The URI of the model",
		required = true
	)
	private String model = null;

	@Parameter (
			names = {"--username"},
			description = "username for authentication"
	)
	private String username = null;

	@Parameter (
			names = {"--password"},
			description = "password for authentication"
	)
	private String password = null;

	private String authenticateEndpoint = "http://localhost:8080/openscoring/user";


	public <V extends SimpleResponse> V execute(Operation<V> operation) throws Exception {
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(JacksonJsonProvider.class);
		clientConfig.register(ObjectMapperProvider.class);

		Client client = ClientBuilder.newClient(clientConfig);

		try {
			WebTarget target = null;
			Response authenResponse = null;

			if (getUsername()!=null && getPassword()!=null) {
				// perform login here...
				target = client.target(authenticateEndpoint);
				UserResponse userResponse = new UserResponse();
				userResponse.setUsername(getUsername());
				userResponse.setPassword(getPassword());

				Invocation invocation = target.request(MediaType.APPLICATION_JSON).buildPost(Entity.json(userResponse));
				authenResponse = invocation.invoke();
			}

			// change target to model path
			target = client.target(getURI());

			return operation.perform(target, authenResponse);
		} finally {
			client.close();
		}
	}

	public String getURI(){
		return getModel();
	}

	public String getModel(){
		return this.model;
	}

	public void setModel(String model){
		this.model = model;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}