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
* One web application (and setup users/roles for the first time).
* Multiple model deployer (openscoring-client) instances, one per tenant, on the same host instances with the web application. These instances will deploy models and track changes in the tenant's model storage directories.

##### Start web application and setup authentication/authorization
The build produces a WAR file `openscoring-webapp/target/openscoring-webapp-1.3-SNAPSHOT.war`. This WAR file can be deployed using any Java web container.

###### Setup database for storing authentication information (for the first time only)
=====TODO: complete this description====

You must bee able to create a database on a RDBMS to store authentication and authorization information. In our example, we use mysql but you can use database of your choice.
* Create a database (for example: test) to store authentication information.
* In the created database, create 3 tables with string columns like below:
   - User(username, password, salt, orgId)
   - RolesPermission(permission, rolename)
   - UserRole(roleName, username)
* Open file `openscoring-service/src/main/resources/hibernate.cfg.xml`. Edit the database connection infomation to the database you created in previous step.

The web application can be launced using [Jetty Maven Plugin] (http://eclipse.org/jetty/documentation/current/jetty-maven-plugin.html). Change the working directory to `openscoring-webapp` and execute the following command:
```
mvn jetty:run-war
```

###### Create users/roles
Authentication & authorization functions on openscoring is developed based on 2 java frameworks:
* Apache Shiro
* Apache Hibernate

We used Hibernate to provide functions for persist/update/get user information as well as user roles and role permissions. And Apache Shiro with our customize Realm to perform authenticate & authorize based on supplied principal & credential from the database through Hibernate. 
To make it easier to understand, let assume that our multi-tenancy openscoring provides model services for 2 companies called companyX & companyY. And each company will have some users who can deploy/evaluate/undeploy model for on their company only. 

* When web application running for the 1st time, admin account with default password ‘secret’ will be created.
```
POST http://localhost:8080/openscoring/user
{
  "username" : "admin",
  "password" : "secret"
}
```
* Only admin account can create user, role permission, and add role to user. So, after login with admin account, we will create user first
* Create new user for companyX
```
PUT http://localhost:8080/openscoring/user
{
  	"username" : "userx",
    "password" : "secret",
  	"orgId": "companyX"
}
```
* Create new role & permission belong to this role
```
PUT http://localhost:8080/openscoring/role
{
	"roleName": "companyXdev",
	"permission": "companyX:model"
}
```
* Add role to user
```
PUT http://localhost:8080/openscoring/user/role
{
	"roleName": "companyXdev",
	"username": "userx"
}
```
* Repeat step 3 -> 5 to create for user usery with roleName = companyYdev, with permission companyY:model.

Notice that currently we have just supported permission having format structure <tenantId>:model. It mean that if user have permission companyX:model, he/she will have all privileges to create/update/delete/persist model on companyX. 


##### Start model deployer
Once the web application starts and the users are setup, start multiple openscoring clients serving as model deployers for tenants. For each tenant, the model deployer is started as follow:
* Change the working directory to `openscoring-client`
* java -cp target/client-executable-1.3-SNAPSHOT.jar org.openscoring.client.DirectoryDeployer --dir <path_to_model_storage_directory_of_the_tenant> --model-collection http://localhost:8080/openscoring/model/<tenant_id> --username <user_name> --password <password>


# REST API #

### Overview

Model REST API endpoints:

| HTTP method | Endpoint | Required role(s) | Description |
| ----------- | -------- | ---------------- | ----------- |
| POST | /user | - | User login |
| DELETE | /user | - | User logout |
| GET | /model/${tenant_id} | - | Get the summaries of all models |
| POST | /model/${tenant_id}/${id} | - | Deploy a model |
| PUT | /model/${tenant_id}/${model_id}?persist=tru | - | Deploy a model and persist to tenant's model storage|
| GET | /model/${tenant_id}/${model_id} | - | Get the summary of a model |
| GET | /model/${tenant_id}/${model_id}/pmml | - | Download a model as a PMML document |
| POST | /model/${tenant_id}/${model_id} | - | Evaluate data in "single prediction" mode |
| POST | /model/${tenant_id}/${model_id}/batch | - | Evaluate data in "batch prediction" mode |
| POST | /model/${tenant_id}/${model_id}/csv | - | Evaluate data in "CSV prediction" mode |
| DELETE | /model/${tenant_id}/${model_id} | - | Undeploy a model |


Most of the details of the API remains the same as the original openscoring. The only different is the URL which must contain the tenant_id.
For example: a get command to list all models should be made with:
`curl -X GET 'http://localhost:8080/openscoring/model/companyX'`

Get command to get a DecisionTreeIris model:
`curl -X GET http://localhost:8080/openscoring/model/companyX/DecisionTreeIris`

One can just refer to the original openscoring document and modify the URL accordingly.

### User authentication
Login
`curl -H "Content-Type: application/json" -X POST -d '{"username":"xyz","password":"xyz"}' http://localhost:8080/openscoring/user`

Logout
curl -X DELETE `http://localhost:8080/openscoring/user`

### Model deployment
Similar to original openscoring but add orgId to the url
=====TODO: describe the endpoint and give examples====

### Model query
Similar to original openscoring but add orgId to the url
=====TODO: describe the endpoint and give examples====

### Model evaluation
Similar to original openscoring but add orgId to the url
=====TODO: describe the endpoint and give examples====

### Model undeployment
=====TODO: describe the endpoint and give examples====