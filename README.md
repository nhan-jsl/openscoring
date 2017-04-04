Openscoring - extended version
=============

REST web service for scoring PMML models, with authenticaion/authorization and multi-tenancy capabilities.

This version is an extension of openscoring project to provide the capabilities of authentication, authorization and multi-tenancy.
The original version of Openscoring is at: https://github.com/openscoring/openscoring. This document extended from the original openscoring's README.md serves this extended openscoring version only, for the purposes of running openscoring with multi-tenancy with authentication and authorization.


# Features #
* All features that openscoring offers.
* Authentication and authorization.
* Persist models to file system.
* Multi-tenancy: host multiple tenants and allow the tenant to serve only its users.

# Install and Usage #
The project requires Java 1.7 or newer to run.

Enter the project root directory and build using Apache Maven:
```
mvn clean install
```
The example PMML file DecisionTreeIris.pmml along with example JSON and CSV files can be found in the openscoring-service/src/etc directory.

### Server side
The server side requires a precreated directory for model storage, in which subdirectories, one per tenant, must be created before starting server. The subdirectory name matches with the tenant id (you create this id), which will be used as authorization permission for user (More below about authentication for web application).

The server side requires starting:
* One web application (and setup users/roles for the first time).
* Multiple model deployer (openscoring-client) instances, one per tenant, on the same host instances with the web application. These instances will deploy models and track changes in the tenant's model storage directories.

##### Start web application and setup authentication/authorization
The build produces a WAR file `openscoring-webapp/target/openscoring-webapp-1.3-SNAPSHOT.war`. This WAR file can be deployed using any Java web container.

###### Setup database for storing authentication information (for the first time only)
Configure the authentication data for the webserver. The server uses [Apache Shiro] (https://shiro.apache.org/) for authentication. To edit the authentication data, open file openscoring-webapp/src/main/webapp/WEB-INF/shiro.ini to add users, roles to the appropriate sections. Note that permission to deploy/undeploy models into a tenant's space must be named <tenant_id>:model and each user belonging to the tenant has a role with that permission.

Sample of shiro.ini
```
# -----------------------------------------------------------------------------
# Users and their (optional) assigned roles
# username = password, role1, role2, ..., roleN
# -----------------------------------------------------------------------------
[users]
admin = secret, admin
userx = secret, orgxdev
usery = secret, orgydev

# -----------------------------------------------------------------------------
# Roles with assigned permissions
# roleName = perm1, perm2, ..., permN
# -----------------------------------------------------------------------------
[roles]
admin = * 
orgxdev = orgx:model
orgydev = orgy:model
```

Notice that currently we have just supported permission having format structure <tenantId>:model. It mean that if user have permission orgx:model, he/she will have all privileges to create/update/delete/persist model on orgx. 

The web application can be launced using [Jetty Maven Plugin] (http://eclipse.org/jetty/documentation/current/jetty-maven-plugin.html). Change the working directory to `openscoring-webapp` and execute the following command:
```
mvn jetty:run-war -DstoragePath=/path/to/model/store
```

##### Start model deployer
Once the web application starts and the users are setup, start multiple openscoring clients serving as model deployers for tenants.
At this point, the model directory should be created. And in that directory, create multiple subdirectories, one per tenant if not exist, whose names matches with the tenant_id. It is IMPORTANT to keep per tenant's subdirectory name the same as <tenant_id> because this <tenant_id> is also used for permission configuration in `shiro.ini`. For example: tenant_id orgx, orgy have permissions configured in `shiro.ini` as `orgx:model` and `orgy:model`, accordingly. If you have not done these directory creation steps, do it now.

To start model deployers, for every tenant:
* Open a new terminal, change the working directory to `openscoring-client`
* Start model deployer
```
java -cp target/client-executable-1.3-SNAPSHOT.jar org.openscoring.client.DirectoryDeployer --dir <path_to_model_storage_subdirectory_of_the_tenant> --model-collection http://localhost:8080/openscoring/model/<tenant_id> --username <user_name> --password <password>
```

A sample command to start model deployer for orgx:
```
java -cp target/client-executable-1.3-SNAPSHOT.jar org.openscoring.client.DirectoryDeployer --dir /user/models/orgx --model-collection http://localhost:8080/openscoring/model/orgx --username userx --password secret
```

Whenever there is a new tenant joins the openscoring server, a subdirectory (name matches with <tenant_id>) must be created and a new model deployer should be started for that tenant.

# REST API #

### Overview

Model REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| POST | /user | - | User login |
| DELETE | /user | - | User logout |
| GET | /model/${tenant_id} | - | Get the summaries of all models |
| POST | /model/${tenant_id}/${id} | - | Deploy a model |
| PUT | /model/${tenant_id}/${model_id}?persist=true | - | Deploy a model and persist to tenant's model storage|
| GET | /model/${tenant_id}/${model_id} | - | Get the summary of a model |
| GET | /model/${tenant_id}/${model_id}/pmml | - | Download a model as a PMML document |
| POST | /model/${tenant_id}/${model_id} | - | Evaluate data in "single prediction" mode |
| POST | /model/${tenant_id}/${model_id}/batch | - | Evaluate data in "batch prediction" mode |
| POST | /model/${tenant_id}/${model_id}/csv | - | Evaluate data in "CSV prediction" mode |
| DELETE | /model/${tenant_id}/${model_id} | - | Undeploy a model |


Most of the details of the REST API remain the same as the original openscoring with two differences that should be noticed:
1. As authentication is required to perform request to openscoring server, cookie is needed to maintain the login information accross curl commands. When a user logins, save login information to a cookie file (for example `cookie.txt`)
```
curl -c cookie.txt -H "Content-Type: application/json" -X POST -d '{"username":"userx","password":"secret"}' http://localhost:8080/openscoring/user`
```
    
2. When a user performs PUT/POST/GET on the openscoring server, the cURL invocation differs from the original openscoring's invocation:
- specify the cookie file which was used to store the login information with `-b <cookie_file>` option.
- The endpoint to PUT/POST/GET request models contains tenant_id.

A sample GET request to list all models should be made with:

```
curl -b cookie.txt -X GET 'http://localhost:8080/openscoring/model/orgx'
```

Another sample GET request to get a DecisionTreeIris model:

```
curl -b cookie.txt -X GET http://localhost:8080/openscoring/model/orgx/DecisionTreeIris
```

### User authentication
Login
```
curl -c cookie.txt -H "Content-Type: application/json" -X POST -d '{"username":"userx","password":"secret"}' http://localhost:8080/openscoring/user`
```

Logout
```
curl -X DELETE `http://localhost:8080/openscoring/user
```

The requests to this openscoring server are similar to the original openscoring version, with two differences: (1) the cookie information and (2) the endpoints contains tenant_id: /model/<tenant_id>\[/<model_id\]

### Model deployment

##### PUT /model/${tenant_id}/${model_id}?persist=true

Creates or updates a model.

Sample cURL invocation:
```
curl -b cookie.txt -X PUT --data-binary @DecisionTreeIris.pmml -H "Content-type: text/xml" http://localhost:8080/openscoring/model/orgx/DecisionTreeIrisX9?persist=true
```

### Model querying

##### GET /model/${tenant_id}

Gets the summaries of all models of a tenant.

Sample cURL invocation:
```
curl  -b cookie.txt -X GET http://localhost:8080/openscoring/model/orgx
```

##### GET /model/${tenant_id}/${model_id}

Gets the summary of a model.

Sample cURL invocation:
```
curl -b cookie.txt -X GET http://localhost:8080/openscoring/model/orgx/DecisionTreeIris
```

### Model evaluation

##### POST /model/${tenant_id}/${model_id}

Sample cURL invocation:
```
curl -b cookie.txt -X POST --data-binary @EvaluationRequest.json -H "Content-type: application/json" http://localhost:8080/openscoring/model/orgx/DecisionTreeIris
```

### Model undeployment

##### DELETE /model/${tenant_id}/${model_id}

Sample cURL invocation:
```
curl -b cookie.txt -X DELETE http://localhost:8080/openscoring/model/orgx/DecisionTreeIris
```

*Other types of requests, user can refer to the original openscoring README.original.md, remember to include cookie information and the < <tenant_id> in request*
