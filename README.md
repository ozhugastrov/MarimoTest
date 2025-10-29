# Marimo service
This is a simple service that allows users to manage marimo notebooks in k8s

### How to run:
Simply run `make build deploy` from root folder.


### Usage examples

#### To create Marimo service
```
curl -X PUT 127.0.0.1:8081/api/v1/notebook/{notebookName}
```

#### After creation you can access your service by the following url 

```
http://localhost:8081/api/v1/user/{notebookName}/
```

#### To delete Marimo service
```
curl -X DELETE 127.0.0.1:8081/api/v1/notebook/{notebookName}
```

### TODO
1. Db integration 
2. Restart logic
3. Added persistent store for notebooks


