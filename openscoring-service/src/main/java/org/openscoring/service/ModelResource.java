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

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.dmg.pmml.FieldName;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jpmml.evaluator.*;
import org.openscoring.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.prefs.CsvPreference;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Path("model")
@PermitAll
public class ModelResource {

	@Context
	private UriInfo uriInfo = null;

	private ModelRegistry modelRegistry = null;

	private MetricRegistry metricRegistry = null;


	@Inject
	public ModelResource(ModelRegistry modelRegistry, MetricRegistry metricRegistry){
		this.modelRegistry = modelRegistry;
		this.metricRegistry = metricRegistry;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public BatchModelResponse queryBatch(){
		Subject currentUser = SecurityUtils.getSubject();

		if (currentUser.isAuthenticated()) {
			BatchModelResponse batchResponse = new BatchModelResponse();

			List<ModelResponse> responses = new ArrayList<>();

			Collection<Map.Entry<String, Model>> entries = this.modelRegistry.entries();
			for (Map.Entry<String, Model> entry : entries) {
				ModelResponse response = createModelResponse(entry.getKey(), entry.getValue(), false);
				responses.add(response);
			}

			Comparator<ModelResponse> comparator = new Comparator<ModelResponse>() {
				@Override
				public int compare(ModelResponse left, ModelResponse right) {
					return (left.getId()).compareToIgnoreCase(right.getId());
				}
			};
			Collections.sort(responses, comparator);

			batchResponse.setResponses(responses);

			return batchResponse;
		} else {
			throw new NotAuthorizedException("Sorry, You don't have permission to view this content!");
		}
	}

	@GET
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@Produces(MediaType.APPLICATION_JSON)
	public ModelResponse query(@PathParam("id") String id){
		Subject currentUser = SecurityUtils.getSubject();

		if (!currentUser.isAuthenticated()) {
			throw new NotAuthorizedException("Sorry, You don't have permission to view this content!");
		}

		Model model = this.modelRegistry.get(id);
		if(model == null){
			throw new NotFoundException("Model [" + id + "] not found");
		}

		return createModelResponse(id, model, true);
	}

	@PUT
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	@Produces(MediaType.APPLICATION_JSON)
	public Response deploy(@PathParam("id") String id,
						   @DefaultValue("false") @QueryParam("persist") boolean persist, InputStream is){
		Subject currentUser = SecurityUtils.getSubject();
		if (!currentUser.isAuthenticated()) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		} else {
			// user authenticated... we need check user have permission to put model on this orgId or not
			if (currentUser.hasRole(RoleConstant.SUPER_ROLE)) {
				// add prefix orgId
				return doDeploy(id, is, persist);
			}
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deployForm(@FormDataParam("id") String id,
							   @FormDataParam("pmml") InputStream is, @DefaultValue("false") @QueryParam("persist") boolean persist){
		Subject currentUser = SecurityUtils.getSubject();

		if (!currentUser.isAuthenticated() || !currentUser.hasRole(RoleConstant.SUPER_ROLE)) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}

		if(!ModelRegistry.validateId(id)){
			throw new BadRequestException("Invalid identifier");
		}

		return doDeploy(id, is, persist);
	}

	private Response doDeploy(String id, InputStream is, boolean persist){
		// handle persist
		if (persist) {
			try {
				String storagePathProp = System.getProperty("storagePath");
				if (!storagePathProp.endsWith("/")) {
					storagePathProp = storagePathProp + "/";
				}
				String storagePath = storagePathProp + id + ".pmml";
				File file = new File(storagePath);
				file.getParentFile().mkdirs(); // create folder based on org
				Files.copy(is, Paths.get(storagePath), StandardCopyOption.REPLACE_EXISTING);
				SimpleResponse simpleResponse = new SimpleResponse();
				simpleResponse.setMessage("Persist this pmml successfully");
				return Response.ok(Entity.json(simpleResponse)).build();
			} catch (Exception e) {
				logger.error("Failed to persist PMML document", e);
				throw new InternalServerErrorException(e.getMessage());
			}
		}

		Model model;

		try {
			model = this.modelRegistry.load(is);
		} catch(Exception e){
			logger.error("Failed to load PMML document", e);

			throw new BadRequestException(e);
		}

		boolean success;

		Model oldModel = this.modelRegistry.get(id);
		if(oldModel != null){
			success = this.modelRegistry.replace(id, oldModel, model);
		} else {
			success = this.modelRegistry.put(id, model);
		} // End if

		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		ModelResponse entity = createModelResponse(id, model, true);

		if(oldModel != null){
			return (Response.ok().entity(entity)).build();
		} else {
			UriBuilder uriBuilder = (this.uriInfo.getAbsolutePathBuilder()).path(id);
			URI uri = uriBuilder.build();
			return (Response.created(uri).entity(entity)).build();
		}
	}

	@GET
	@Path("{id:" + ModelRegistry.ID_REGEX + "}/pmml")
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_XML})
	public Response download(@PathParam("id") String id){
		Subject currentUser = SecurityUtils.getSubject();

		if (!currentUser.isAuthenticated() ||
				!(currentUser.hasRole(RoleConstant.NORMAL_ROLE) || currentUser.hasRole(RoleConstant.SUPER_ROLE))) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}

		final
		Model model = this.modelRegistry.get(id, true);

		if(model == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		StreamingOutput entity = new StreamingOutput(){

			@Override
			public void write(OutputStream os) throws IOException {
				BufferedOutputStream bufferedOs = new BufferedOutputStream(os){

					@Override
					public void close() throws IOException {
						flush();

						// The closing of the underlying java.io.OutputStream is handled elsewhere
					}
				};

				try {
					ModelResource.this.modelRegistry.store(model, bufferedOs);
				} catch(JAXBException je){
					throw new InternalServerErrorException(je);
				} finally {
					bufferedOs.close();
				}
			}
		};

		return (Response.ok().entity(entity))
			.type(MediaType.APPLICATION_XML_TYPE.withCharset(ModelResource.CHARSET_UTF8.name()))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + id + ".pmml.xml") // XXX
			.build();
	}

	@POST
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public EvaluationResponse evaluate(@PathParam("id") String id, EvaluationRequest request){
		Subject currentUser = SecurityUtils.getSubject();
		if (!currentUser.isAuthenticated() ||
				!(currentUser.hasRole(RoleConstant.NORMAL_ROLE) || currentUser.hasRole(RoleConstant.SUPER_ROLE))) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}

		List<EvaluationRequest> requests = Collections.singletonList(request);

		List<EvaluationResponse> responses = doEvaluate(id, requests, true, "evaluate");

		return responses.get(0);
	}

	@POST
	@Path("{id: " + ModelRegistry.ID_REGEX + "}/batch")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public BatchEvaluationResponse evaluateBatch(@PathParam("id") String id, BatchEvaluationRequest request){
		Subject currentUser = SecurityUtils.getSubject();
		if (!currentUser.isAuthenticated() ||
				!(currentUser.hasRole(RoleConstant.NORMAL_ROLE) || currentUser.hasRole(RoleConstant.SUPER_ROLE))) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}

		BatchEvaluationResponse batchResponse = new BatchEvaluationResponse(request.getId());

		List<EvaluationRequest> requests = request.getRequests();

		List<EvaluationResponse> responses = doEvaluate(id, requests, false, "evaluate.batch");

		batchResponse.setResponses(responses);

		return batchResponse;
	}

	@POST
	@Path("{id:" + ModelRegistry.ID_REGEX + "}/csv")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
	public Response evaluateCsv(@PathParam("id") String id, @QueryParam("delimiterChar") String delimiterChar, @QueryParam("quoteChar") String quoteChar, @HeaderParam(HttpHeaders.CONTENT_TYPE) String contentType, InputStream is){
		Subject currentUser = SecurityUtils.getSubject();

		if (!currentUser.isAuthenticated() ||
				!(currentUser.hasRole(RoleConstant.NORMAL_ROLE) || currentUser.hasRole(RoleConstant.SUPER_ROLE))) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}

		com.google.common.net.MediaType mediaType = com.google.common.net.MediaType.parse(contentType);
		Charset charset = (mediaType.charset()).or(ModelResource.CHARSET_UTF8);
		return doEvaluateCsv(id, delimiterChar, quoteChar, charset, is);
	}

	@POST
	@Path("{id:" + ModelRegistry.ID_REGEX + "}/csv")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
	public Response evaluateCsvForm(@PathParam("id") String id, @QueryParam("delimiterChar") String delimiterChar, @QueryParam("quoteChar") String quoteChar, @FormDataParam("csv") InputStream is){
		Subject currentUser = SecurityUtils.getSubject();
		if (!currentUser.isAuthenticated() ||
				!(currentUser.hasRole(RoleConstant.NORMAL_ROLE) || currentUser.hasRole(RoleConstant.SUPER_ROLE))) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}

		Charset charset = ModelResource.CHARSET_UTF8;
		return doEvaluateCsv(id, delimiterChar, quoteChar, charset, is);
	}

	private Response doEvaluateCsv(String id, String delimiterChar, String quoteChar, final Charset charset, InputStream is){
		final
		CsvPreference format;

		final
		CsvUtil.Table<EvaluationRequest> requestTable;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset)){
				@Override
				public void close(){
					// The closing of the underlying java.io.InputStream is handled elsewhere
				}
			};

			try {
				if(delimiterChar != null){
					format = CsvUtil.getFormat(delimiterChar, quoteChar);
				} else {
					format = CsvUtil.getFormat(reader);
				}

				requestTable = CsvUtil.readTable(reader, format);
			} finally {
				reader.close();
			}
		} catch(Exception e){
			logger.error("Failed to load CSV document", e);
			throw new BadRequestException(e);
		}

		List<EvaluationRequest> requests = requestTable.getRows();

		List<EvaluationResponse> responses = doEvaluate(id, requests, true, "evaluate.csv");

		final
		CsvUtil.Table<EvaluationResponse> responseTable = new CsvUtil.Table<>();
		responseTable.setId(requestTable.getId());
		responseTable.setRows(responses);

		StreamingOutput entity = new StreamingOutput(){

			@Override
			public void write(OutputStream os) throws IOException {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, charset)){

					@Override
					public void close() throws IOException {
						flush();
						// The closing of the underlying java.io.OutputStream is handled elsewhere
					}
				};

				try {
					CsvUtil.writeTable(writer, format, responseTable);
				} finally {
					writer.close();
				}
			}
		};

		return (Response.ok().entity(entity))
			.type(MediaType.TEXT_PLAIN_TYPE.withCharset(charset.name()))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + id + ".csv") // XXX
			.build();
	}

	@SuppressWarnings (
		value = "resource"
	)
	private List<EvaluationResponse> doEvaluate(String id, List<EvaluationRequest> requests, boolean allOrNothing, String method){
		Model model = this.modelRegistry.get(id, true);
		if(model == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		List<EvaluationResponse> responses = new ArrayList<>();

		Timer timer = this.metricRegistry.timer(createName(id, method));

		Timer.Context context = timer.time();

		try {
			ModelEvaluator<?> evaluator = model.getEvaluator();

			if(evaluator instanceof HasGroupFields){
				HasGroupFields hasGroupFields = (HasGroupFields)evaluator;

				List<InputField> groupFields = hasGroupFields.getGroupFields();
				if(groupFields.size() == 1){
					InputField groupField = groupFields.get(0);

					requests = aggregateRequests(groupField.getName(), requests);
				} else

				if(groupFields.size() > 1){
					throw new EvaluationException("Too many group fields");
				}
			}

			for(EvaluationRequest request : requests){
				EvaluationResponse response;

				try {
					response = evaluate(evaluator, request);
				} catch(Exception e){

					if(allOrNothing){
						throw e;
					}

					response = new EvaluationResponse(request.getId());
					response.setMessage(e.toString());
				}

				responses.add(response);
			}
		} catch(Exception e){
			logger.error("Failed to evaluate", e);

			throw new BadRequestException(e);
		}

		context.stop();

		Counter counter = this.metricRegistry.counter(createName(id, "records"));

		counter.inc(responses.size());

		return responses;
	}

	@DELETE
	@Path("{id:" + ModelRegistry.ID_REGEX + "}")
	@Produces(MediaType.APPLICATION_JSON)
	public SimpleResponse undeploy(@PathParam("id") String id){
		Subject currentUser = SecurityUtils.getSubject();
		if (!currentUser.isAuthenticated() || !currentUser.hasRole(RoleConstant.SUPER_ROLE)) {
			throw new NotAuthorizedException("Sorry, You don't have permission to do this action!");
		}

		Model model = this.modelRegistry.get(id);
		if(model == null){
			throw new NotFoundException("Model \"" + id + "\" not found");
		}

		boolean success = this.modelRegistry.remove(id, model);
		if(!success){
			throw new InternalServerErrorException("Concurrent modification");
		}

		final
		String prefix = createNamePrefix(id);

		MetricFilter filter = new MetricFilter(){

			@Override
			public boolean matches(String name, Metric metric){
				return name.startsWith(prefix);
			}
		};

		this.metricRegistry.removeMatching(filter);

		SimpleResponse response = new SimpleResponse();

		return response;
	}

	static
	protected String createName(String... strings){
		return MetricRegistry.name(ModelResource.class, strings);
	}

	static
	protected String createNamePrefix(String... strings){
		return createName(strings) + ".";
	}

	static
	protected List<EvaluationRequest> aggregateRequests(FieldName groupName, List<EvaluationRequest> requests){
		Map<Object, ListMultimap<String, Object>> groupedArguments = new LinkedHashMap<>();

		String key = groupName.getValue();

		for(EvaluationRequest request : requests){
			Map<String, ?> requestArguments = request.getArguments();

			Object value = requestArguments.get(key);
			if(value == null && !requestArguments.containsKey(key)){
				logger.warn("Evaluation request {} does not specify a group field {}", request.getId(), key);
			}

			ListMultimap<String, Object> groupedArgumentMap = groupedArguments.get(value);
			if(groupedArgumentMap == null){
				groupedArgumentMap = ArrayListMultimap.create();

				groupedArguments.put(value, groupedArgumentMap);
			}

			Collection<? extends Map.Entry<String, ?>> entries = requestArguments.entrySet();
			for(Map.Entry<String, ?> entry : entries){
				groupedArgumentMap.put(entry.getKey(), entry.getValue());
			}
		}

		// Only continue with request modification if there is a clear need to do so
		if(groupedArguments.size() == requests.size()){
			return requests;
		}

		List<EvaluationRequest> resultRequests = new ArrayList<>();

		Collection<Map.Entry<Object, ListMultimap<String, Object>>> entries = groupedArguments.entrySet();
		for(Map.Entry<Object, ListMultimap<String, Object>> entry : entries){
			Map<String, Object> arguments = new LinkedHashMap<>();
			arguments.putAll((entry.getValue()).asMap());

			// The value of the "group by" column is a single Object, not a Collection (ie. java.util.List) of Objects
			arguments.put(key, entry.getKey());

			EvaluationRequest resultRequest = new EvaluationRequest();
			resultRequest.setArguments(arguments);

			resultRequests.add(resultRequest);
		}

		return resultRequests;
	}

	static
	protected EvaluationResponse evaluate(Evaluator evaluator, EvaluationRequest request){
		logger.info("Received {}", request);

		Map<String, ?> requestArguments = request.getArguments();

		EvaluationResponse response = new EvaluationResponse(request.getId());

		Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

		List<InputField> activeFields = evaluator.getActiveFields();
		for(InputField activeField : activeFields){
			FieldName activeName = activeField.getName();

			String key = activeName.getValue();

			Object value = requestArguments.get(key);
			if(value == null && !requestArguments.containsKey(key)){
				logger.warn("Evaluation request {} does not specify an active field {}", request.getId(), key);
			}

			FieldValue activeValue = activeField.prepare(value);

			arguments.put(activeName, activeValue);
		}

		logger.debug("Evaluation request {} has prepared arguments: {}", request.getId(), arguments);

		Map<FieldName, ?> result = evaluator.evaluate(arguments);

		// Jackson does not support the JSON serialization of <code>null</code> map keys
		result = replaceNullKey(result);

		logger.debug("Evaluation response {} has result: {}", response.getId(), result);

		response.setResult(EvaluatorUtil.decode(result));

		logger.info("Returned {}", response);

		return response;
	}

	static
	private <V> Map<FieldName, V> replaceNullKey(Map<FieldName, V> map){

		if(map.containsKey(null)){
			Map<FieldName, V> result = new LinkedHashMap<>(map);
			result.put(ModelResource.DEFAULT_NAME, result.remove(null));

			return result;
		}

		return map;
	}

	static
	private ModelResponse createModelResponse(String id, Model model, boolean expand){
		ModelResponse response = new ModelResponse(id);
		response.setMiningFunction(model.getMiningFunction());
		response.setSummary(model.getSummary());
		response.setProperties(model.getProperties());

		if(expand){
			response.setSchema(model.getSchema());
		}

		return response;
	}

	public static final FieldName DEFAULT_NAME = FieldName.create("_default");

	private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

	private static final Logger logger = LoggerFactory.getLogger(ModelResource.class);
}