build:
	eval $$(minikube docker-env) && docker build -t marimo-local:latest .

deploy:
	kubectl apply -f k8s/
	kubectl rollout status deployment/marimo-test --timeout=30s
	kubectl port-forward service/marimo-test 8081:80

