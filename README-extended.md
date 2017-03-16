Openscoring - extension version
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
* One web application
* Multiple model deployers (openscoring-client), one per tenant, on the same host where web application is running. These clients will deploy models and track changes in the tenant's model storage directories.

##### Start web application
The build produces a WAR file `openscoring-webapp/target/openscoring-webapp-1.3-SNAPSHOT.war`. This WAR file can be deployed using any Java web container.

Configure the authentication data for the webserver. The server uses [Apache Shiro] (https://shiro.apache.org/) for authentication. To edit the authentication data, open file `openscoring-webapp/src/main/webapp/WEB-INF/shiro.ini` to add users, roles to the appropriate sections. Note that permission to deploy/undeploy models into a tenant's space must be named <tenant_id>:model and each user belonging to the tenant has a role with that permission.

Sample of `shiro.ini`
```
# -----------------------------------------------------------------------------
# Users and their (optional) assigned roles
# username = password, role1, role2, ..., roleN
# -----------------------------------------------------------------------------
[users]
admin = secret, admin
nhanndX = secret, orgxdev
nhanndY = secret, orgydev

# -----------------------------------------------------------------------------
# Roles with assigned permissions
# roleName = perm1, perm2, ..., permN
# -----------------------------------------------------------------------------
[roles]
admin = *
orgxdev = orgx:model
orgydev = orgy:model
```

The web application can be launced using [Jetty Maven Plugin] (http://eclipse.org/jetty/documentation/current/jetty-maven-plugin.html). Change the working directory to `openscoring-webapp` and execute the following command:
```
mvn jetty:run-war
```

##### Start model deployer
Once the web application starts, start multiple openscoring clients serving as model deployers for tenants. For each tenant, the model deployer is started as follow:
* Change the working directory to `openscoring-client`
* java -cp target/client-executable-1.3-SNAPSHOT.jar org.openscoring.client.DirectoryDeployer --dir <path_to_model_storage_directory_of_the_tenant> --model-collection http://localhost:8080/openscoring/model/<tenant_id> --username <user_name> --password <password>


# REST API #

### Overview

Model REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| POST | /user | - | User login |
| DELETE | /user | - | User logout |
| GET | /model/${tenant_id} | ${tenant_id}:model | Get the summaries of all models |
| POST | /model/${tenant_id}/${id} | admin | Deploy a model |
| PUT | /model/${tenant_id}/${model_id}?persist=tru | admin | Deploy a model and persist to tenant's model storage|
| GET | /model/${tenant_id}/${model_id} | - | Get the summary of a model |
| GET | /model/${tenant_id}/${model_id}/pmml | admin | Download a model as a PMML document |
| POST | /model/${tenant_id}/${model_id} | - | Evaluate data in "single prediction" mode |
| POST | /model/${tenant_id}/${model_id}/batch | - | Evaluate data in "batch prediction" mode |
| POST | /model/${tenant_id}/${model_id}/csv | - | Evaluate data in "CSV prediction" mode |
| DELETE | /model/${tenant_id}/${model_id} | admin | Undeploy a model |


### User authentication
=====TODO: describe the endpoint and give examples====

### Model deployment
=====TODO: describe the endpoint and give examples====

### Model query
=====TODO: describe the endpoint and give examples====

### Model evaluation
=====TODO: describe the endpoint and give examples====

### Model undeployment
=====TODO: describe the endpoint and give examples====