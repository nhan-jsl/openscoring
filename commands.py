import requests

## All sample data files such as .pmml, .json can be found in openscoring-service/src/etc

#login with nhanndX:secret
headers={'Content-Type':'application/json'}
url = 'http://localhost:8080/openscoring/user'
data = '{"username":"nhanndX","password":"secret"}'

r = requests.post(url, headers=headers, data=data)
# >>> r.content
# '{\n  "message" : "Welcome nhanndX"\n}'

cookies = dict(JSESSIONID=r.cookies['JSESSIONID'])

# Query summary of models of an orgId orgx
url = 'http://localhost:8080/openscoring/model/orgx'
r = requests.get(url, cookies=cookies)

#Deploy a model from local file DecisionTreeIris.pmml as model id DecisionTreeIris11
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11'
with open('DecisionTreeIris.pmml','rb') as payload:
    r = requests.put(url, data=payload, headers=headers, cookies=cookies)

# Query a model whose id is DecisionTreeIris of orgx
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11'
r = requests.get(url, cookies=cookies)

# Request PMML of the model just deployed
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11/pmml'
r = requests.get(url, cookies=cookies)

# Evaluate a model
## Evaluate model using 'single prediction' mode
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11'
headers = {'Content-type': 'application/json'}
with open('EvaluationRequest.json','rb') as payload:
    r = requests.post(url, headers=headers, data=payload,cookies=cookies)

## Evaluates data in "batch prediction" mode
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11/batch'
headers = {'Content-type': 'application/json'}
with open('BatchEvaluationRequest.json','rb') as payload:
    r = requests.post(url, headers=headers, data=payload,cookies=cookies)

## Evaluates data in "CSV prediction" mode.
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11/csv'
headers = {'Content-type': 'text/plain', 'charset': 'UTF-8'}
with open('input.csv','rb') as payload:
    r = requests.post(url, headers=headers, data=payload,cookies=cookies)

## Evaluate data in "CSV Gzip format" mode
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11/csv'
headers = {'Content-encoding': 'gzip', 'Content-type': 'text/plain', 'charset': 'UTF-8','Accept-encoding': 'gzip'}
with open('input.csv.gz','rb') as payload:
    r = requests.post(url, headers=headers, data=payload,cookies=cookies)

# Undeploy model
url = 'http://localhost:8080/openscoring/model/orgx/DecisionTreeIris11'
r = requests.delete(url, cookies=cookies)

#logout
r = requests.delete(url, cookies=cookies)
# >>> r.content
# '{\n  "message" : "Logout successfully"\n}'

