# Docker, Docker Hub & Kubernetes Guide

## Folder Structure Created

```
├── docker-compose.yml                  ← Local development stack
├── .env.example                        ← Environment variable template
├── scripts/
│   └── mysql-init.sql                  ← Creates all 12+ databases on first start
├── backEnd/
│   ├── Eureka/Dockerfile
│   ├── ConfigServer/Dockerfile
│   ├── apiGateway/
│   │   ├── Dockerfile
│   │   └── src/main/resources/
│   │       └── application-docker.yml  ← Docker routes (container names, not localhost)
│   ├── KeyCloak/Dockerfile
│   └── Microservices/<each>/Dockerfile
├── frontend/smart-freelance-app/
│   ├── Dockerfile
│   └── nginx.conf
├── k8s/
│   ├── 00-namespace.yaml
│   ├── 01-configmap.yaml
│   ├── 02-secrets.yaml               ← Fill in base64 values before applying
│   ├── 03-mysql.yaml                 ← StatefulSet + PVC
│   ├── 04-keycloak.yaml
│   ├── 05-eureka.yaml
│   ├── 06-config-server.yaml
│   ├── 07-api-gateway.yaml
│   ├── 08-microservices.yaml         ← All 14 microservices
│   ├── 09-frontend.yaml
│   └── 10-ingress.yaml
└── .github/workflows/ci-cd.yml
```

---

## Quick Start (Docker Compose)

```bash
# 1. Set up credentials
cp .env.example .env
# Edit .env — fill in passwords, tokens, credential file paths

# 2. Build all images (first time ~10–20 min due to Maven downloads)
docker-compose build

# 3. Start everything
docker-compose up -d

# 4. Watch logs
docker-compose logs -f

# 5. Check which services are healthy
docker-compose ps

# Frontend:    http://localhost:4200
# API Gateway: http://localhost:8078
# Eureka:      http://localhost:8420
# Keycloak:    http://localhost:9090  (admin/admin)
```

**Startup order** (enforced by healthchecks):
```
MySQL + Keycloak → Eureka → Config Server → Gateway/Keycloak-Auth → All Microservices → Frontend
```

---

## Docker Hub — Build, Tag & Push

### Setup
```bash
# Login once
docker login -u YOUR_USERNAME

# Set your username (or put in .env)
export DOCKER_HUB_USERNAME=YOUR_USERNAME
export TAG=v1.0.0          # or 'latest'
```

### Build all images
```bash
docker-compose build
```

### Tag strategy
```
latest   — current main branch (always overwritten)
v1.0.0   — semantic version for releases
sha-abc12 — git SHA for traceability (set by CI/CD)
```

### Push all images
```bash
# Push everything at once
docker-compose push

# Or use the repo helper from the project root
bash scripts/push-docker-images.sh

# Or push a single service
docker push YOUR_USERNAME/user:latest
docker push YOUR_USERNAME/user:v1.0.0
```

### Naming convention
```
YOUR_USERNAME/eureka:latest
YOUR_USERNAME/config-server:latest
YOUR_USERNAME/api-gateway:latest
YOUR_USERNAME/keycloak-auth:latest
YOUR_USERNAME/user:latest
YOUR_USERNAME/project:latest
YOUR_USERNAME/offer:latest
YOUR_USERNAME/contract:latest
YOUR_USERNAME/portfolio:latest
YOUR_USERNAME/review:latest
YOUR_USERNAME/planning:latest
YOUR_USERNAME/task:latest
YOUR_USERNAME/notification:latest
YOUR_USERNAME/gamification:latest
YOUR_USERNAME/chat:latest
YOUR_USERNAME/meeting:latest
YOUR_USERNAME/freelancia-job:latest
YOUR_USERNAME/vendor:latest
YOUR_USERNAME/frontend:latest
```

---

## Kubernetes Deployment

### Prerequisites
- `kubectl` configured for your cluster
- Nginx Ingress Controller installed
- (Optional) cert-manager for TLS

### 1. Fill in secrets
```bash
# base64-encode your values:
echo -n 'your-strong-password' | base64

# Edit k8s/02-secrets.yaml and paste encoded values
# For firebase/calendar JSONs:
base64 < firebase-credentials.json | tr -d '\n'
```

### 2. Replace image names
```bash
# Replace placeholder in all K8s manifests:
sed -i 's/YOUR_DOCKERHUB_USERNAME/your-actual-username/g' k8s/*.yaml
```

### 3. Apply in order
```bash
kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-configmap.yaml
kubectl apply -f k8s/02-secrets.yaml
kubectl apply -f k8s/03-mysql.yaml

# Wait for MySQL to be ready
kubectl wait --for=condition=ready pod -l app=mysql -n smart-freelance --timeout=120s

kubectl apply -f k8s/04-keycloak.yaml
kubectl apply -f k8s/05-eureka.yaml
kubectl apply -f k8s/06-config-server.yaml
kubectl apply -f k8s/07-api-gateway.yaml
kubectl apply -f k8s/08-microservices.yaml
kubectl apply -f k8s/09-frontend.yaml
kubectl apply -f k8s/10-ingress.yaml
```

### 4. Verify
```bash
kubectl get pods -n smart-freelance
kubectl get services -n smart-freelance
kubectl get ingress -n smart-freelance

# Logs for a specific pod
kubectl logs -f deployment/api-gateway -n smart-freelance

# Shell into a pod
kubectl exec -it deployment/user -n smart-freelance -- sh
```

### 5. Rolling update (new image)
```bash
kubectl set image deployment/user user=YOUR_USERNAME/user:v1.1.0 -n smart-freelance
kubectl rollout status deployment/user -n smart-freelance
kubectl rollout undo deployment/user -n smart-freelance   # rollback if needed
```

### 6. Scaling
```bash
kubectl scale deployment/user --replicas=3 -n smart-freelance
kubectl scale deployment/api-gateway --replicas=3 -n smart-freelance
```

---

## CI/CD (GitHub Actions)

### Required Secrets (Settings → Secrets → Actions)
| Secret | Value |
|--------|-------|
| `DOCKERHUB_USERNAME` | Your Docker Hub username |
| `DOCKERHUB_TOKEN` | Docker Hub access token |
| `KUBECONFIG_DATA` | `base64 < ~/.kube/config` |

### Pipeline triggers
- **Push to `main`** → build all images in parallel → push to Docker Hub → deploy to K8s
- **Pull request** → build only (no push, no deploy)

### Parallel matrix build
All 19 images build simultaneously, cutting total CI time by ~10x.

---

## Keycloak Setup Notes

After starting the stack, configure Keycloak manually:
1. Open `http://localhost:9090` → log in as `admin/admin`
2. Create realm: `smart-freelance`
3. Create roles: `CLIENT`, `FREELANCER`, `ADMIN`
4. Create client: `smart-freelance-backend` (confidential, service accounts enabled)
5. Set client secret to match `KEYCLOAK_ADMIN_CLIENT_SECRET` in `.env`

See `backEnd/KeyCloak/README.md` for full Keycloak setup instructions.

---

## Security Best Practices

- **Never commit `.env`** — it's gitignored; use `.env.example` as template
- **K8s Secrets** — use SealedSecrets or HashiCorp Vault for real clusters
- **Credential files** (Firebase, Google Calendar) — mounted as read-only volumes, never baked into images
- **Non-root containers** — all Spring Boot images run as `appuser` (uid 1001)
- **Container-aware JVM** — `UseContainerSupport` + `MaxRAMPercentage=75` prevents OOMKills
- **Resource limits** — all K8s deployments have CPU/memory requests and limits set

## Monitoring Suggestions

- **Logs**: `docker-compose logs -f [service]` locally; use EFK stack (Elasticsearch + Fluentd + Kibana) in K8s
- **Metrics**: Add `spring-boot-starter-actuator` + Micrometer → Prometheus → Grafana
- **Tracing**: Add Spring Cloud Sleuth + Zipkin for distributed tracing
- **Health**: Eureka dashboard at `http://localhost:8420` shows all registered services
