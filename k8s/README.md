# Homewealth · Local Kubernetes Setup

[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.33-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io)
[![Minikube](https://img.shields.io/badge/Minikube-local-brightgreen?logo=kubernetes&logoColor=white)](https://minikube.sigs.k8s.io)
[![Postgres](https://img.shields.io/badge/PostgreSQL-18.3-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Grafana](https://img.shields.io/badge/Grafana_LGTM-0.25.0-F46800?logo=grafana&logoColor=white)](https://grafana.com)

Local development environment for the Homewealth platform, running on [Minikube](https://minikube.sigs.k8s.io). Infrastructure and business services are isolated in dedicated namespaces so each layer can be deployed, updated, or torn down independently.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  Namespace: services                                                │
│                                                                     │
│  ┌──────────────────┐      ┌──────────────────┐                     │
│  │  transactions    │      │  assets (soon)   │   ← business        │
│  │  :8081           │      │  :8082           │     services        │
│  └────────┬─────────┘      └──────────────────┘                     │
│           │  cross-namespace DNS                                    │
└───────────┼─────────────────────────────────────────────────────────┘
            │
┌───────────┼─────────────────────────────────────────────────────────┐
│  Namespace: shared          │                                       │
│           │                 │                                       │
│  ┌────────▼────────┐   ┌────▼────────────┐                          │
│  │   postgres      │   │    grafana      │   ← shared               │
│  │   :5432         │   │   LGTM stack    │     infrastructure       │
│  │   PVC: 2Gi      │   │   :3000 (UI)    │                          │
│  └─────────────────┘   │   :4318 (HTTP)  │                          │
│                        │   PVC: 1Gi      │                          │
│                        └─────────────────┘                          │
└─────────────────────────────────────────────────────────────────────┘
```

### Cross-namespace DNS

Services communicate across namespaces using Kubernetes internal DNS:

```
<service>.<namespace>.svc.cluster.local:<port>
```

| Consumer         | Dependency   | Address                                  |
|------------------|--------------|------------------------------------------|
| `transactions`   | Postgres     | `postgres.shared.svc.cluster.local:5432` |
| `transactions`   | Grafana OTLP | `grafana.shared.svc.cluster.local:4318`  |

---

## Repository structure

```
k8s/
├── shared/                         # Shared infrastructure namespace
│   ├── shared-namespace.yaml       # Namespace definition
│   ├── postgres.yaml               # PostgreSQL · Deployment + PVC + Secret + Service
│   └── grafana.yaml                # Grafana LGTM · Deployment + PVC + Service + NodePort
│
└── services/                       # Business services namespace
    ├── services-namespace.yaml     # Namespace definition
    └── transactions.yaml           # Transactions API · Deployment + Service + NodePort
```

> **Adding a new service** — create a new file under `services/` and apply it. No existing files need to be modified.
>
> **Adding shared infrastructure** (e.g. RabbitMQ) — create a new file under `shared/` and apply it. Business services are unaffected.

---

## Prerequisites

| Tool        | Purpose                    | Install     |
|-------------|----------------------------|-------------|
| `minikube`  | Local Kubernetes cluster   | [docs](https://minikube.sigs.k8s.io/docs/start/) |
| `kubectl`   | Cluster CLI                | [docs](https://kubernetes.io/docs/tasks/tools/) |
| `docker`    | Container runtime          | [docs](https://docs.docker.com/get-docker/) |

```bash
minikube start --driver=docker
minikube addons enable metrics-server
```

### Using local images (no remote registry)

```bash
# Load a locally built image into Minikube
docker build -t homewealth/transactions:1.0.0 .
minikube image load homewealth/transactions:1.0.0

# Ensure the deployment has imagePullPolicy: Never
```

---

## Deploy

### 1 · Namespaces

```bash
kubectl apply -f k8s/shared/shared-namespace.yaml
kubectl apply -f k8s/services/services-namespace.yaml
```

### 2 · Secrets

Secrets are created imperatively and are never committed to version control.

```bash
kubectl create secret generic postgres-secret \
  --namespace=shared \
  --from-literal=username=<username> \
  --from-literal=password=<password>
  
kubectl create secret generic postgres-secret \
  --namespace=services \
  --from-literal=username=<username> \
  --from-literal=password=<password>
```

### 3 · Shared infrastructure

```bash
kubectl apply -f k8s/shared/postgres.yaml
kubectl apply -f k8s/shared/grafana.yaml

# Wait for both to be ready before proceeding
kubectl rollout status deployment/postgres -n shared
kubectl rollout status deployment/grafana  -n shared
```

### 4 · Business services

```bash
kubectl apply -f k8s/services/transactions.yaml
kubectl rollout status deployment/transactions -n services
```

---

## Accessing services

| Service          | NodePort | Command                                              |
|------------------|----------|------------------------------------------------------|
| Grafana UI       | `30300`  | `minikube service grafana-nodeport -n shared`        |
| Transactions API | `30081`  | `minikube service transactions-nodeport -n services` |

> The `minikube service` command creates a tunnel and must remain open in a dedicated terminal while you test. Alternatively, access services directly via the Minikube IP:
> ```bash
> curl http://$(minikube ip):30081/api/v1/transactions
> ```

---

## Useful commands

```bash
# Inspect a namespace
kubectl get all -n shared
kubectl get all -n services

# Check persistent volumes
kubectl get pvc -n shared
kubectl get pv

# Stream logs
kubectl logs -f deployment/transactions -n services

# Describe a failing pod
kubectl describe pod -l app=transactions -n services

# Resource usage (requires metrics-server)
kubectl top pods -A
kubectl top nodes
```

### Teardown

```bash
# Remove only business services — shared namespace is unaffected
kubectl delete namespace services

# Remove only shared infrastructure — services namespace is unaffected
kubectl delete namespace shared

# Destroy the entire cluster
minikube delete
```

---

## Secrets management

Secrets are intentionally excluded from this repository. For local development, create them manually with `kubectl create secret` as shown above. For team or GitOps workflows, consider:

| Approach | Complexity | Best for |
|---|---|---|
| `kubectl create secret` (manual) | Low | Local development |
| [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) | Medium | GitOps / team |
| [External Secrets](https://external-secrets.io) + Vault / AWS SM | High | Production |

---

## Observability

Grafana LGTM (`grafana/otel-lgtm`) bundles **Loki**, **Grafana**, **Tempo**, and **Mimir** in a single container, providing logs, traces, and metrics out of the box via OpenTelemetry.

| Signal  | Endpoint                                      | Spring Boot property                                         |
|---------|-----------------------------------------------|--------------------------------------------------------------|
| Metrics | `grafana.shared.svc.cluster.local:4318/v1/metrics` | `management.otlp.metrics.export.url`                    |
| Traces  | `grafana.shared.svc.cluster.local:4318/v1/traces`  | `management.opentelemetry.tracing.export.otlp.endpoint` |
| Logs    | `grafana.shared.svc.cluster.local:4318/v1/logs`    | `management.opentelemetry.logging.export.otlp.endpoin`  |