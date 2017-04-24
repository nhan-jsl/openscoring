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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import com.beust.jcommander.Parameter;
import org.openscoring.common.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Deployer extends ModelApplication {

	@Parameter (
		names = {"--file"},
		description = "The PMML file",
		required = true
	)
	private File file = null;


	static
	public void main(String... args) throws Exception {
		run(Deployer.class, args);
	}

	@Override
	public void run() throws Exception {
		ModelResponse response = deploy();

		String message = response.getMessage();
		if(message != null){
			logger.warn("Deployment failed: {}", message);

			return;
		}

		logger.info("Deployment succeeded: {}", response);
	}

	public ModelResponse deploy() throws Exception {
		Operation<ModelResponse> operation = new Operation<ModelResponse>(){

			@Override
			public ModelResponse perform(WebTarget target, Response authenResponse) throws IOException {

				try(PushbackInputStream is = new PushbackInputStream(new FileInputStream(getFile()), 16)){
					String encoding = getContentEncoding(is);

					Variant variant = new Variant(MediaType.APPLICATION_XML_TYPE, (Locale)null, encoding);

					Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON);

					if (authenResponse != null) {
						for (Map.Entry<String, NewCookie> entry : authenResponse.getCookies().entrySet()) {
							invocationBuilder.cookie(entry.getValue().toCookie());
						}
					}

					Invocation invocation = invocationBuilder.buildPut(Entity.entity(is, variant));
					Response response = invocation.invoke();

					return response.readEntity(ModelResponse.class);
				}
			}
		};

		return execute(operation);
	}

	public File getFile(){
		return this.file;
	}

	public void setFile(File file){
		this.file = file;
	}

	static
	private String getContentEncoding(PushbackInputStream is) throws IOException {
		byte[] signature = new byte[2];

		int count = is.read(signature);

		is.unread(signature, 0, count);

		if((count == signature.length) && Arrays.equals(Deployer.GZIP_SIGNATURE, signature)){
			return "gzip";
		}

		return null;
	}

	private static final byte[] GZIP_SIGNATURE = {
		(byte)(GZIPInputStream.GZIP_MAGIC),
		(byte)(GZIPInputStream.GZIP_MAGIC >> 8),
	};

	private static final Logger logger = LoggerFactory.getLogger(Deployer.class);
}