# k8s-demo-app

A full-stack microservices application demonstrating a production-like Kubernetes setup with event-driven communication via Kafka, CI/CD pipelines, GitOps deployments, and code quality gates.

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-green)
![Angular](https://img.shields.io/badge/Angular-21-red)
![Kubernetes](https://img.shields.io/badge/Kubernetes-1.29-blue)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.1.0-black)

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
        │         └── /api/orders   ──► Order Service   ──► PostgreSQL
        │
        └── Event-Driven Communication (Apache Kafka)
                  │
                  ├── order-events ──► Product Service (reserves stock)
                  │                └► Notification Service (sends emails) ──► PostgreSQL
                  └── inventory-events ──► Order Service (stock confirmation)

Deployed via ArgoCD (GitOps) · Built via GitHub Actions + SonarCloud
```

### Services

| Service | Technology | Port | Description |
|---|---|---|---|
| `frontend` | Angular 21, NgRx Signal Store | 80 | SPA served by nginx |
| `api-gateway` | Spring Cloud Gateway 2025.1.1 | 8080 | Single entry point, routing, CORS |
| `product-service` | Spring Boot 4.0.5, JPA, Flyway | 8081 | Product CRUD + stock reservation via Kafka |
| `order-service` | Spring Boot 4.0.5, JPA, Flyway | 8082 | Order management + Kafka event publisher |
| `notification-service` | Spring Boot 4.0.5, Thymeleaf, GraalVM native | 8083 | Listens to order events, sends HTML emails |
| `products-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Products database |
| `orders-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Orders database |
| `notifications-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Processed event deduplication store |
| `kafka-cluster` | Apache Kafka 4.1.0 (Strimzi, KRaft) | 9092 | Event streaming |
| `mailhog` | MailHog 1.0.1 | 1025/8025 | SMTP mock server for development |

---

## Tech Stack

### Backend
- **Java 21** with virtual threads
- **Spring Boot 4.0.5** (Spring Framework 7.0)
- **Spring Cloud Gateway 2025.1.1** for API routing
- **Spring Data JPA** + **Hibernate 7**
- **Spring Kafka** for event-driven messaging
- **Flyway** for database migrations
- **Thymeleaf** for HTML email templates
- **MapStruct 1.6** for DTO mapping
- **Lombok** for boilerplate reduction
- **JaCoCo** for test coverage
- **GraalVM Native Image** for the notification service

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
- **NGINX Ingress Controller**
- **MailHog** for development email testing

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

## Project Structure

```
k8s-demo-app/
├── services/
│   ├── kafka-events/             # Shared event library (Java records)
│   ├── product-service/          # Spring Boot + Kafka consumer
│   ├── order-service/            # Spring Boot + Kafka producer/consumer
│   ├── notification-service/     # Spring Boot + Kafka consumer + email
│   │   └── Dockerfile.native     # GraalVM native image build
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
│   └── argocd-apps.yaml
└── .github/
    └── workflows/
        ├── product-service.yml
        ├── order-service.yml
        ├── notification-service.yml
        ├── notification-service-native.yml
        ├── api-gateway.yml
        └── frontend.yml
```

---

## API Reference

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

### 7. Apply Kubernetes Manifests

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

# Wait for databases and Kafka to be ready
kubectl get pods -n databases -w
kubectl get kafka -n kafka -w
```

### 8. Configure ArgoCD

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

### 9. Add Local DNS Entries

```bash
echo "127.0.0.1 demo-app.local" | sudo tee -a /etc/hosts
echo "127.0.0.1 mailhog.local" | sudo tee -a /etc/hosts
```

### 10. Access the Application

| URL | Description |
|---|---|
| http://demo-app.local | Angular frontend |
| http://demo-app.local/api/products | Products API |
| http://demo-app.local/api/orders | Orders API |
| https://argocd.local | ArgoCD dashboard |
| http://mailhog.local:8025 | MailHog web UI (inspect sent emails) |

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

The notification service has an additional optional workflow (`notification-service-native.yml`) that compiles a GraalVM native binary and reports the resulting binary size in the GitHub Actions job summary.

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

# Build notification-service as a GraalVM native image
docker build -f services/notification-service/Dockerfile.native \
  -t notification-service:native services/

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

---

## License

MIT License — see [LICENSE](LICENSE) for details.
