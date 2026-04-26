# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Shared library (build first before any backend service)
```bash
cd services/kafka-events && mvn clean install -DskipTests
```

### Backend services (run from the service directory)
```bash
mvn clean verify -B          # test
mvn clean package -DskipTests # build JAR
```

### Frontend
```bash
cd frontend
npm install
npm test         # unit tests
npm run lint     # ESLint
npm start        # dev server on :4200
npm run build -- --configuration=production
```

### Running a single test class
```bash
mvn test -Dtest=OrderServiceTest           # backend
ng test --include='**/order*.spec.ts'      # frontend
```

## Architecture

```
Browser
  └─ Angular 21 Frontend (nginx :80)
        └─ API Gateway (Spring Cloud Gateway :8080)
              ├─ /api/products/**        → product-service  :8081
              ├─ /api/orders/**          → order-service    :8082
              ├─ /api/notifications/**   → notification-service :8083
              └─ /api/{svc}/actuator/**  → same svc, StripPrefix=2
```

All four backend services use Spring Boot 4.0.5 / Java 21 with virtual threads. Each service owns its own PostgreSQL 16 database (CloudNativePG); there is no cross-service DB access.

### Kafka event flow

```
order-service  ──► order-events ──► product-service  (reserves stock)
                               └──► notification-service (sends email)

product-service ──► inventory-events ──► order-service (confirms or cancels order)
```

Events are Java records defined in `services/kafka-events/` and shared as a local Maven dependency. The four event types are `OrderCreatedEvent`, `OrderStatusChangedEvent`, `StockUpdatedEvent`, `StockInsufficientEvent`.

**Idempotency:** every consumer service stores processed event IDs in its own PostgreSQL `ProcessedEvent` table to prevent duplicate handling on rebalance.

### API Gateway routing details

Actuator routes use `StripPrefix=2` — `/api/notification-service/actuator/health` becomes `/actuator/health` forwarded to port 8083. The Kubernetes Services for **product-service and order-service expose port 80** (mapping to container ports 8081/8082), while **notification-service currently exposes port 8083** directly — the gateway `NOTIFICATION_SERVICE_URL` must therefore include `:8083` explicitly.

### Helm chart conventions

Each service has a Helm chart under `helm/{service}/`. The api-gateway chart reads `PRODUCT_SERVICE_URL`, `ORDER_SERVICE_URL`, and `NOTIFICATION_SERVICE_URL` from a ConfigMap generated from `helm/api-gateway/values.yaml`. CI/CD updates the `image.tag` in each chart's `values.yaml` after a successful build.

### Frontend conventions

The frontend follows these non-default Angular patterns (enforced in `frontend/.claude/CLAUDE.md`):
- Standalone components only — do **not** set `standalone: true` (it's the default in Angular v20+)
- `input()` / `output()` functions, not `@Input`/`@Output` decorators
- `ChangeDetectionStrategy.OnPush` on every component
- Native control flow (`@if`, `@for`) — not `*ngIf`, `*ngFor`
- `class` bindings — not `ngClass`; `style` bindings — not `ngStyle`
- `inject()` function — not constructor injection
- All UI must pass AXE / WCAG AA checks

### Backend conventions

- Use records for DTOs
- Always add `@Transactional` on service methods that write to the DB
- MapStruct for DTO mapping, Lombok for boilerplate reduction
- Throw custom exceptions (EntityNotFoundException, etc.)
- Never return null from service methods — use Optional or throw
- All exceptions are handled centrally via @RestControllerAdvice
- Use list.getLast() instead of list.get(list.size() - 1)
- Use list.getFirst() instead of list.get(0)
- Don't user container annotations as a wrapper to use multiple instances of the same annotation. As of Java 8, this is no longer necessary. Instead, these annotations should be used directly without a wrapper, resulting in cleaner and more readable code.

### CI/CD

GitHub Actions workflows (`.github/workflows/`) build each service independently. The trigger path includes `services/kafka-events/**` for all backend workflows. On main, a successful build pushes a Docker image to `ghcr.io/kevin-fechner/k8s-demo-app/{service}:sha-{7-char-sha}` and commits an updated `image.tag` to the relevant Helm chart, which ArgoCD picks up automatically.