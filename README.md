# k8s-demo-app

A full-stack microservices application demonstrating a production-like Kubernetes setup with CI/CD pipelines, GitOps deployments, and code quality gates.

![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-green)
![Angular](https://img.shields.io/badge/Angular-21-red)
![Kubernetes](https://img.shields.io/badge/Kubernetes-1.29-blue)

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
        └── Deployed via ArgoCD (GitOps)
            Built via GitHub Actions + SonarCloud
```

### Services

| Service | Technology | Port | Description |
|---|---|---|---|
| `frontend` | Angular 21, NgRx Signal Store | 80 | SPA served by nginx |
| `api-gateway` | Spring Cloud Gateway 2025.1.1 | 8080 | Single entry point, routing, CORS |
| `product-service` | Spring Boot 4.0.5, JPA, Flyway | 8081 | Product CRUD API |
| `order-service` | Spring Boot 4.0.5, JPA, Flyway | 8082 | Order management API |
| `products-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Products database |
| `orders-db` | PostgreSQL 16 (CloudNativePG) | 5432 | Orders database |

---

## Tech Stack

### Backend
- **Java 21** with virtual threads
- **Spring Boot 4.0.5** (Spring Framework 7.0)
- **Spring Cloud Gateway 2025.1.1** for API routing
- **Spring Data JPA** + **Hibernate 7**
- **Flyway** for database migrations
- **MapStruct 1.6** for DTO mapping
- **Lombok** for boilerplate reduction
- **JaCoCo** for test coverage

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
- **NGINX Ingress Controller**

### CI/CD & Quality
- **GitHub Actions** for CI/CD pipelines
- **SonarCloud** for code quality gates
- **JaCoCo** for test coverage reports
- **GitHub Container Registry (ghcr.io)** for Docker images

---

## Project Structure

```
k8s-demo-app/
├── services/
│   ├── product-service/          # Spring Boot microservice
│   ├── order-service/            # Spring Boot microservice
│   └── api-gateway/              # Spring Cloud Gateway
├── frontend/                     # Angular 21 application
├── helm/                         # Helm charts
│   ├── product-service/
│   ├── order-service/
│   ├── api-gateway/
│   └── frontend/
├── k8s/                          # Raw Kubernetes manifests
│   ├── namespaces.yaml
│   ├── databases.yaml
│   ├── ingress.yaml
│   └── argocd-apps.yaml
└── .github/
    └── workflows/                # CI/CD pipelines
        ├── product-service.yml
        ├── order-service.yml
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
| `POST` | `/api/orders` | Create order |
| `PATCH` | `/api/orders/{id}/status` | Update order status |
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

### 6. Apply Kubernetes Manifests

```bash
# Create namespaces
kubectl apply -f k8s/namespaces.yaml

# Deploy databases
kubectl apply -f k8s/databases.yaml

# Wait for databases to be ready
kubectl get pods -n databases -w
```

### 7. Configure ArgoCD

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

### 8. Add Local DNS Entries

```bash
echo "127.0.0.1 demo-app.local" | sudo tee -a /etc/hosts
```

### 9. Access the Application

| URL | Description |
|---|---|
| http://demo-app.local | Angular frontend |
| http://demo-app.local/api/products | Products API |
| http://demo-app.local/api/orders | Orders API |
| https://argocd.local | ArgoCD dashboard |

---

## CI/CD Pipeline

Each service has its own GitHub Actions workflow that runs on every push to `main` when files in that service's directory change.

```
Developer pushes code
        │
        ▼
GitHub Actions
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
| `SONAR_TOKEN` | SonarCloud global analysis token |
| `GHCR_TOKEN` | GitHub PAT with `write:packages` scope |

---

## Running Tests

### Backend Services

```bash
# Product Service
cd services/product-service
mvn clean test

# Order Service
cd services/order-service
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
docker build -t api-gateway:local services/api-gateway/
docker build -t frontend:local frontend/

# Check image sizes
docker images | grep -E "product|order|api-gateway|frontend"
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
| `databases` | PostgreSQL clusters |
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