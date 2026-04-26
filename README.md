# k8s-demo-app

A full-stack microservices application demonstrating a production-like Kubernetes setup with event-driven communication via Kafka, CI/CD pipelines, GitOps deployments, and code quality gates.

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-green)
![Angular](https://img.shields.io/badge/Angular-21-red)
![Kubernetes](https://img.shields.io/badge/Kubernetes-1.29-blue)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.1.0-black)
![Redis](https://img.shields.io/badge/Redis-7.4-red)
![Prometheus](https://img.shields.io/badge/Prometheus-monitoring-orange)
![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-tracing-blueviolet)

---

## Architecture

```
http://demo-app.local
        │
        ├── Angular 21 Frontend (NgRx Signal Store)
        │         │
        │         ▼
        ├── API Gateway (Spring Cloud Gateway)
        │         │
        │         ├── /api/products ──► Product Service ──► PostgreSQL
        │                                   └──► Redis (product cache)
        │         └── /api/orders   ──► Order Service   ──► PostgreSQL
        │
        └── Event-Driven Communication (Apache Kafka)
                  │
                  ├── order-events ──► Product Service (reserves stock)
                  │                └► Notification Service (sends emails) ──► PostgreSQL
                  └── inventory-events ──► Order Service (stock confirmation)

Observability (monitoring namespace)
        ├── Prometheus ──► scrapes /actuator/prometheus from all backend pods
        └── Grafana Tempo ──► receives OTLP traces from all backend services

Deployed via ArgoCD (GitOps) · Built via GitHub Actions + SonarCloud
```

### Services

| Service | Technology | Port | Description |
|---|---|---|---|
| `frontend` | Angular 21, NgRx Signal Store | 80 | SPA served by nginx |
| `api-gateway` | Spring Cloud Gateway 2025.1.1 | 8080 | Single entry point, routing, CORS |
| `product-service` | Spring Boot 4.0.5, JPA, Flyway, Redis | 8081 | Product CRUD + stock reservation via Kafka |
| `order-service` | Spring Boot 4.0.5, JPA, Flyway | 8082 | Order management + Kafka event publisher |
| `notification-service` | Spring Boot 4.0.5, Thymeleaf | 8083 | Listens to order events, sends HTML emails |
| `products-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Products database |
| `orders-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Orders database |
| `notifications-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Processed event deduplication store |
| `redis` | Redis 7.4 (Alpine) | 6379 | Product cache (LRU, 128 MB) |
| `kafka-cluster` | Apache Kafka 4.1.0 (Strimzi, KRaft) | 9092 | Event streaming |
| `mailhog` | MailHog 1.0.1 | 1025/8025 | SMTP mock server for development |
| `prometheus` | Prometheus (monitoring ns) | 9090 | Metrics collection and storage |
| `tempo` | Grafana Tempo (monitoring ns) | 4318 | Distributed trace ingestion (OTLP/HTTP) |

---

## Tech Stack

### Backend
- **Java 21** with virtual threads
- **Spring Boot 4.0.5** (Spring Framework 7.0)
- **Spring Cloud Gateway 2025.1.1** for API routing
- **Spring Data JPA** + **Hibernate 7**
- **Spring Kafka** for event-driven messaging
- **Spring Data Redis** + **Spring Cache** for distributed caching
- **Flyway** for database migrations
- **Thymeleaf** for HTML email templates
- **MapStruct 1.6** for DTO mapping
- **Lombok** for boilerplate reduction
- **JaCoCo** for test coverage

### Shared Library
- **kafka-events** — shared Java records defining all event types across services (`OrderCreatedEvent`, `OrderStatusChangedEvent`, `StockUpdatedEvent`, `StockInsufficientEvent`)

### Frontend
- **Angular 21** with standalone components
- **NgRx Signal Store** for state management
- **Reactive Forms** with validation
- **OnPush change detection** throughout
- **nginx** for production serving

### Infrastructure
- **Kubernetes** (kind for local development)
- **Helm 3** charts for all services
- **ArgoCD** for GitOps deployments
- **CloudNativePG** operator for PostgreSQL
- **Strimzi** operator for Apache Kafka
- **Redis 7.4** (Alpine) for caching
- **NGINX Ingress Controller**
- **MailHog** for development email testing

### Observability
- **Micrometer + Prometheus** — metrics exposed via `/actuator/prometheus` on all backend services; p50/p95/p99 histograms for HTTP requests; application-tagged metrics for Prometheus aggregation across replicas
- **Grafana Tempo** — distributed trace backend; all services export spans via OTLP/HTTP; Kafka producer/consumer spans included via Spring Kafka observation support
- **OpenTelemetry** (`spring-boot-starter-opentelemetry`) — 100 % sampling; trace context propagated end-to-end through HTTP and Kafka

### CI/CD & Quality
- **GitHub Actions** for CI/CD pipelines
- **SonarCloud** for code quality gates
- **JaCoCo** for test coverage reports
- **GitHub Container Registry (ghcr.io)** for Docker images

---

## Kafka Event Flow

Order lifecycle is propagated asynchronously across services via two Kafka topics:

### Topics

| Topic | Partitions | Retention | Purpose |
|---|---|---|---|
| `order-events` | 3 | 7 days | Order lifecycle events |
| `inventory-events` | 3 | 7 days | Stock availability responses |

### Events

| Event | Producer | Consumers | Key Fields |
|---|---|---|---|
| `OrderCreatedEvent` | order-service | product-service, notification-service | orderId, customerName, customerEmail, items[], totalAmount |
| `OrderStatusChangedEvent` | order-service | notification-service | orderId, customerEmail, previousStatus, newStatus |
| `StockUpdatedEvent` | product-service | order-service | productId, quantityReserved, newStock |
| `StockInsufficientEvent` | product-service | order-service | productId, requiredQuantity, availableQuantity |

### Flow

```
POST /api/orders
      │
      ▼
order-service creates order
      │
      └─► publishes OrderCreatedEvent ──► product-service: reserves stock
                                      └─► notification-service: sends confirmation email

PATCH /api/orders/{id}/status
      │
      ▼
order-service updates status
      │
      └─► publishes OrderStatusChangedEvent ──► notification-service: sends status update email

product-service (after reserving)
      │
      └─► publishes StockUpdatedEvent / StockInsufficientEvent ──► order-service
```

### Idempotency

Each consuming service stores processed event IDs in PostgreSQL (`ProcessedEvent` table) to prevent duplicate handling on Kafka consumer restarts or rebalances.

---

## Redis Caching

`product-service` uses Redis as a distributed cache to reduce database load on product lookups.

### What is cached

| Cache name | Key | TTL | Populated by |
|---|---|---|---|
| `products` | product ID | 5 minutes | `GET /api/products/{id}` |

Only individual product lookups are cached. List queries hit the database directly.

### Cache invalidation

Cache entries are evicted eagerly on any write:

| Operation | Trigger | Mechanism |
|---|---|---|
| Update product | `PUT /api/products/{id}` | `@CacheEvict` |
| Delete product | `DELETE /api/products/{id}` | `@CacheEvict` |
| Stock reserved | `StockUpdatedEvent` (Kafka) | Programmatic evict via `CacheManager` |

The Kafka-driven eviction ensures the cache stays consistent when stock changes are applied asynchronously — a cached product entry is removed as soon as the `product-service` processes the inventory event.

### Configuration

| Setting | Value |
|---|---|
| Host | `redis.demo-app.svc.cluster.local:6379` |
| TTL | 300 s |
| Max memory | 128 MB |
| Eviction policy | `allkeys-lru` |
| Serialization | JSON (`GenericJacksonJsonRedisSerializer`) |
| Null caching | Disabled |

---

## Observability Stack

All four backend services are fully instrumented with metrics and distributed tracing, both deployed into the `monitoring` namespace.

### Metrics (Prometheus + Micrometer)

Every service exposes `GET /actuator/prometheus`. Prometheus scrapes all backend pods in the `demo-app` namespace via Kubernetes service discovery — configured in `k8s/prometheus-values.yaml` — without requiring individual `ServiceMonitor` resources.

**Standard metrics exposed per service:**
- HTTP request latency histograms (`http.server.requests`) with p50, p95, p99 percentiles
- JVM memory, GC, thread, and CPU metrics
- Spring Kafka consumer lag and listener metrics
- PostgreSQL connection pool metrics (HikariCP)
- Kubernetes node-level metrics via Node Exporter
- Cluster-level metrics via `kube-state-metrics`

**Business counters:**

| Metric | Service | Description |
|---|---|---|
| `business.orders.created` | order-service | Orders placed |
| `business.orders.confirmed` | order-service | Orders confirmed via Kafka |
| `business.orders.cancelled` | order-service | Orders cancelled via Kafka |
| `business.stock.reserved` | product-service | Successful stock reservations |
| `business.stock.insufficient` | product-service | Stock reservation failures |
| `business.emails.sent` | notification-service | Emails dispatched |

All metrics carry an `application` tag (e.g. `application="order-service"`) for cross-service Prometheus queries.

### Distributed Tracing (OpenTelemetry + Grafana Tempo)

All services use `spring-boot-starter-opentelemetry` and export spans via OTLP/HTTP to Grafana Tempo at `http://tempo.monitoring.svc.cluster.local:4318/v1/traces`. Sampling is set to 100 % (`management.tracing.sampling.probability=1.0`).

Trace context propagates across:
- **HTTP** — incoming requests and outgoing calls through the API Gateway
- **Kafka** — producer and consumer spans are captured because `spring.kafka.template.observation-enabled` and `spring.kafka.listener.observation-enabled` are both `true` on every service

A full order lifecycle (HTTP → order-service → Kafka → product-service / notification-service) is therefore represented as a single distributed trace in Tempo.

---

## Project Structure

```
k8s-demo-app/
├── services/
│   ├── kafka-events/             # Shared event library (Java records)
│   ├── product-service/          # Spring Boot + Kafka consumer
│   ├── order-service/            # Spring Boot + Kafka producer/consumer
│   ├── notification-service/     # Spring Boot + Kafka consumer + email
│   └── api-gateway/              # Spring Cloud Gateway
├── frontend/                     # Angular 21 application
├── helm/                         # Helm charts
│   ├── product-service/
│   ├── order-service/
│   ├── notification-service/
│   ├── api-gateway/
│   └── frontend/
├── k8s/                          # Raw Kubernetes manifests
│   ├── namespaces.yaml
│   ├── databases.yaml
│   ├── kafka.yaml
│   ├── kafka-topics.yaml
│   ├── ingress.yaml
│   ├── mailhog.yaml
│   ├── redis.yaml
│   ├── argocd-apps.yaml
│   ├── prometheus-values.yaml    # Helm values for Prometheus (pod scraping, ingress)
│   └── prometheus-rbac.yaml     # ClusterRole for Prometheus pod discovery
└── .github/
    └── workflows/
        ├── product-service.yml
        ├── order-service.yml
        ├── notification-service.yml
        ├── api-gateway.yml
        └── frontend.yml
```

---

## API Reference

Interactive API documentation is available via Swagger UI:

| Service | URL |
|---|---|
| Order Service | http://demo-app.local/api/order-service/swagger-ui.html |
| Product Service | http://demo-app.local/api/product-service/swagger-ui.html |

### Product Service — `/api/products`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/products` | List all products |
| `GET` | `/api/products/{id}` | Get product by ID |
| `POST` | `/api/products` | Create product |
| `PUT` | `/api/products/{id}` | Update product |
| `DELETE` | `/api/products/{id}` | Delete product |

### Order Service — `/api/orders`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/orders` | List all orders |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `POST` | `/api/orders` | Create order (triggers OrderCreatedEvent) |
| `PATCH` | `/api/orders/{id}/status` | Update order status (triggers OrderStatusChangedEvent) |
| `DELETE` | `/api/orders/{id}` | Delete order |

Order status values: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`

### Pagination

Both services support cursor-based pagination:

| Parameter | Description | Example |
|---|---|---|
| `cursor` | Pagination cursor from previous response | `eyJpZCI6MTV9` |
| `size` | Page size (default 10, max 100) | `20` |

Response format:
```json
{
  "data": [...],
  "nextCursor": "eyJpZCI6MTV9",
  "hasMore": true,
  "size": 10
}
```

Use `nextCursor` from the response as the `cursor` parameter in the next request. When `hasMore` is `false` you have reached the last page.

### Product Filters

| Parameter | Description | Example |
|---|---|---|
| `name` | Partial name match (case-insensitive) | `keyboard` |
| `minPrice` | Minimum price | `50.00` |
| `maxPrice` | Maximum price | `200.00` |
| `inStock` | Filter by stock availability | `true` |

### Order Filters

| Parameter | Description | Example |
|---|---|---|
| `status` | Order status | `CONFIRMED` |
| `customerEmail` | Partial email match | `kevin@demo-app.local` |
| `fromDate` | Start date (ISO format) | `2026-04-01T00:00:00` |
| `toDate` | End date (ISO format) | `2026-04-30T23:59:59` |

---

## Health Dashboard

A real-time dashboard at http://demo-app.local/health shows the status of all services, refreshing every 10 seconds.

| Metric | Description |
|---|---|
| **Status** | UP / DOWN for each service |
| **Uptime** | How long the service has been running |
| **Memory** | JVM heap usage |
| **Kafka** | Kafka consumer connection status |
| **Database** | PostgreSQL connection status |

The dashboard calls Spring Boot Actuator endpoints exposed through the API Gateway:

| Endpoint | Service |
|---|---|
| `/api/gateway/actuator/health` | API Gateway |
| `/api/order-service/actuator/health` | Order Service |
| `/api/product-service/actuator/health` | Product Service |
| `/api/notification-service/actuator/health` | Notification Service |

---

## Local Development

### Prerequisites

- Ubuntu 24.04 (or any modern Linux distribution)
- Java 21 JDK
- Maven 3.9+
- Node.js 22 (via NVM)
- Docker
- kubectl
- kind
- Helm 3
- ArgoCD CLI

### 1. Clone the Repository

```bash
git clone https://github.com/kevin-fechner/k8s-demo-app.git
cd k8s-demo-app
```

### 2. Create the Kubernetes Cluster

```bash
cat <<EOF > kind-cluster.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
- role: worker
- role: worker
- role: worker
EOF

kind create cluster --name my-cluster --config kind-cluster.yaml
```

### 3. Install the NGINX Ingress Controller

```bash
helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.hostPort.enabled=true \
  --set controller.hostPort.ports.http=80 \
  --set controller.hostPort.ports.https=443 \
  --set controller.kind=DaemonSet \
  --set controller.nodeSelector."kubernetes\.io/hostname"=my-cluster-control-plane \
  --set "controller.tolerations[0].key=node-role.kubernetes.io/control-plane" \
  --set "controller.tolerations[0].operator=Exists" \
  --set "controller.tolerations[0].effect=NoSchedule"
```

### 4. Install ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Expose ArgoCD UI
cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-ingress
  namespace: argocd
  annotations:
    nginx.ingress.kubernetes.io/ssl-passthrough: "true"
    nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
spec:
  ingressClassName: nginx
  rules:
  - host: argocd.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: argocd-server
            port:
              number: 443
EOF

echo "127.0.0.1 argocd.local" | sudo tee -a /etc/hosts
```

### 5. Install the CloudNativePG Operator

```bash
helm repo add cnpg https://cloudnative-pg.github.io/charts
helm repo update

helm install cnpg-operator cnpg/cloudnative-pg \
  --namespace cnpg-system \
  --create-namespace
```

### 6. Install the Strimzi Kafka Operator

```bash
helm repo add strimzi https://strimzi.io/charts
helm repo update

helm install strimzi-operator strimzi/strimzi-kafka-operator \
  --namespace kafka \
  --create-namespace
```

### 7. Install the Observability Stack

```bash
# Add Helm repositories
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Deploy Prometheus into the monitoring namespace
helm upgrade --install prometheus prometheus-community/prometheus \
  --namespace monitoring \
  --create-namespace \
  --values k8s/prometheus-values.yaml

# Apply RBAC so Prometheus can discover pods in demo-app namespace
kubectl apply -f k8s/prometheus-rbac.yaml

# Deploy Grafana Tempo (single-binary, no persistence required for local dev)
helm upgrade --install tempo grafana/tempo \
  --namespace monitoring \
  --set tempo.storage.trace.backend=local

# Add local DNS entry for Prometheus
echo "127.0.0.1 prometheus.local" | sudo tee -a /etc/hosts
```

### 9. Apply Kubernetes Manifests

```bash
# Create namespaces
kubectl apply -f k8s/namespaces.yaml

# Deploy databases
kubectl apply -f k8s/databases.yaml

# Deploy Kafka cluster and topics
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/kafka-topics.yaml

# Deploy MailHog (SMTP mock)
kubectl apply -f k8s/mailhog.yaml

# Deploy Redis (product cache)
kubectl apply -f k8s/redis.yaml

# Wait for databases and Kafka to be ready
kubectl get pods -n databases -w
kubectl get kafka -n kafka -w
```

### 10. Configure ArgoCD

```bash
# Port-forward ArgoCD
kubectl port-forward svc/argocd-server -n argocd 8080:443 &

# Get initial admin password
ARGOCD_PASS=$(kubectl get secret -n argocd argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 --decode)

# Login
argocd login localhost:8080 \
  --username admin \
  --password "$ARGOCD_PASS" \
  --insecure --grpc-web

# Add your GitHub repository
argocd repo add https://github.com/kevin-fechner/k8s-demo-app.git \
  --username kevin-fechner \
  --password YOUR_GITHUB_TOKEN \
  --grpc-web

# Deploy all applications
kubectl apply -f k8s/argocd-apps.yaml
```

### 11. Add Local DNS Entries

```bash
echo "127.0.0.1 demo-app.local" | sudo tee -a /etc/hosts
echo "127.0.0.1 mailhog.local" | sudo tee -a /etc/hosts
```

### 12. Access the Application

| URL | Description |
|---|---|
| http://demo-app.local | Angular frontend |
| http://demo-app.local/health | System health dashboard |
| http://demo-app.local/api/products | Products API |
| http://demo-app.local/api/orders | Orders API |
| http://demo-app.local/api/order-service/swagger-ui.html | Order Service Swagger UI |
| http://demo-app.local/api/product-service/swagger-ui.html | Product Service Swagger UI |
| https://argocd.local | ArgoCD dashboard |
| http://mailhog.local:8025 | MailHog web UI (inspect sent emails) |
| http://prometheus.local | Prometheus metrics UI |

---

## CI/CD Pipeline

Each service has its own GitHub Actions workflow that runs on every push to `main` when files in that service's directory change.

```
Developer pushes code
        │
        ▼
GitHub Actions
  ├── Build kafka-events shared library
  ├── Run unit tests (Maven / ng test)
  ├── Generate JaCoCo coverage report
  ├── SonarCloud analysis
  ├── Quality gate check  ◄── Pipeline fails here if quality is poor
  ├── Build Docker image
  ├── Push to ghcr.io with SHA tag
  └── Update image tag in helm/values.yaml
        │
        ▼
ArgoCD detects Git change
        │
        ▼
Deploys new image to cluster ✅
```

### GitHub Secrets Required

| Secret | Description |
|---|---|
| `SONAR_TOKEN_PRODUCT` | SonarCloud token for product-service |
| `SONAR_TOKEN_ORDER` | SonarCloud token for order-service |
| `SONAR_TOKEN_NOTIFICATION` | SonarCloud token for notification-service |
| `GHCR_TOKEN` | GitHub PAT with `write:packages` scope |

---

## Running Tests

### Build the shared library first

```bash
cd services/kafka-events
mvn clean install -DskipTests
```

### Backend Services

```bash
# Product Service
cd services/product-service
mvn clean test

# Order Service
cd services/order-service
mvn clean test

# Notification Service
cd services/notification-service
mvn clean test

# API Gateway
cd services/api-gateway
mvn clean test
```

### Frontend

```bash
cd frontend
npm install
ng test --watch=false --browsers=ChromeHeadless
```

---

## Building Docker Images Locally

```bash
# Build all images
docker build -t product-service:local services/product-service/
docker build -t order-service:local services/order-service/
docker build -t notification-service:local services/notification-service/
docker build -t api-gateway:local services/api-gateway/
docker build -t frontend:local frontend/

# Check image sizes
docker images | grep -E "product|order|notification|api-gateway|frontend"
```

---

## Helm Charts

Each service has its own Helm chart under `helm/`. The charts follow the same structure:

```
helm/<service>/
├── Chart.yaml          # Chart metadata
├── values.yaml         # Default configuration
└── templates/
    ├── _helpers.tpl    # Reusable template helpers
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    └── hpa.yaml        # Horizontal Pod Autoscaler
```

To render a chart locally without deploying:

```bash
helm template product-service helm/product-service --namespace demo-app
```

To lint a chart:

```bash
helm lint helm/product-service
```

---

## Namespaces

| Namespace | Contents |
|---|---|
| `demo-app` | All application services |
| `databases` | PostgreSQL clusters (products, orders, notifications) |
| `kafka` | Kafka cluster (Strimzi) |
| `monitoring` | Prometheus, Grafana Tempo |
| `argocd` | ArgoCD GitOps controller |
| `ingress-nginx` | NGINX Ingress Controller |
| `cnpg-system` | CloudNativePG operator |
| `sonarqube` | SonarQube (optional, local analysis) |

---

## Troubleshooting

### Pods not starting — ImagePullBackOff
```bash
# Check the exact error
kubectl describe pod -n demo-app <pod-name> | grep -A 5 Events

# Make sure ghcr.io packages are set to public:
# GitHub → Packages → <package> → Package settings → Change visibility → Public
```

### API calls failing from frontend
```bash
# Check ingress is routing correctly
kubectl get ingress -n demo-app
kubectl describe ingress demo-app-ingress -n demo-app

# Test API directly
curl http://demo-app.local/api/products
```

### Database not initializing
```bash
# Check CloudNativePG cluster status
kubectl get cluster -n databases

# Check pod logs
kubectl logs -n databases <pod-name> | grep -i "flyway\|error"
```

### Kafka not producing/consuming events
```bash
# Check Kafka cluster status
kubectl get kafka -n kafka

# Check topic list
kubectl get kafkatopic -n kafka

# Check consumer group lag
kubectl exec -n kafka kafka-cluster-kafka-0 -- \
  bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups

# View recent messages on a topic
kubectl exec -n kafka kafka-cluster-kafka-0 -- \
  bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic order-events --from-beginning --max-messages 10
```

### Emails not appearing in MailHog
```bash
# Check notification-service logs
kubectl logs -n demo-app -l app=notification-service

# Check MailHog is running
kubectl get pods -n demo-app -l app=mailhog

# Verify MailHog web UI is reachable
curl http://mailhog.local:8025
```

### ArgoCD not syncing
```bash
# Check app status
argocd app list --grpc-web

# Force sync
argocd app sync <app-name> --grpc-web

# Check repository connection
argocd repo list --grpc-web
```

### Prometheus not scraping services
```bash
# Check Prometheus targets (UI → Status → Targets)
# or via API:
curl http://prometheus.local/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# Verify RBAC is applied
kubectl get clusterrolebinding prometheus-pod-reader

# Confirm pods have the expected labels
kubectl get pods -n demo-app --show-labels | grep app.kubernetes.io/name

# Check Prometheus pod logs
kubectl logs -n monitoring -l app.kubernetes.io/name=prometheus -c prometheus-server
```

### Redis cache not working / product-service failing to start

```bash
# Check Redis pod is running
kubectl get pods -n demo-app -l app.kubernetes.io/name=redis

# Ping Redis from inside the cluster
kubectl exec -n demo-app deploy/product-service -- \
  redis-cli -h redis.demo-app.svc.cluster.local ping

# Inspect cache contents
kubectl exec -n demo-app -l app.kubernetes.io/name=redis -- \
  redis-cli keys "products::*"

# Flush the cache (forces DB reads on next request)
kubectl exec -n demo-app -l app.kubernetes.io/name=redis -- \
  redis-cli flushdb
```

### Traces not appearing in Tempo
```bash
# Verify Tempo pod is running
kubectl get pods -n monitoring -l app.kubernetes.io/name=tempo

# Check that a service can reach Tempo
kubectl exec -n demo-app deploy/order-service -- \
  curl -s -o /dev/null -w "%{http_code}" \
  http://tempo.monitoring.svc.cluster.local:4318/v1/traces

# Check service logs for OTLP export errors
kubectl logs -n demo-app -l app=order-service | grep -i "otlp\|trace\|export"
```

### Network Policies not enforced in kind

The network policies in `k8s/network-policies.yaml` are correctly defined 
but are **not enforced** in this local kind cluster because kind uses 
`kindnet` as its CNI plugin, which does not support NetworkPolicy enforcement.

In a production cluster using Calico, Cilium, or any other NetworkPolicy-capable 
CNI the policies would enforce:

- Only `api-gateway` can receive external traffic
- Services can only reach their own database
- Services can only communicate with Kafka
- No direct pod-to-pod communication outside defined rules

To test NetworkPolicy enforcement locally, recreate the kind cluster with 
Calico as the CNI:

```bash
# Create cluster without default CNI
cat < kind-cluster-calico.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
networking:
  disableDefaultCNI: true
  podSubnet: "192.168.0.0/16"
nodes:
- role: control-plane
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
  - containerPort: 443
    hostPort: 443
- role: worker
- role: worker
- role: worker
EOF

kind create cluster --name my-cluster --config kind-cluster-calico.yaml

# Install Calico
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.27.0/manifests/calico.yaml
```

---

## License

MIT License — see [LICENSE](LICENSE) for details.
