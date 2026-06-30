# Deploying the modern app (production)

Strangler-fig: the modern app runs **alongside** the legacy GeneXus app against the **same**
`saude-mandaguari` database. The new app is published on **:8090** (legacy keeps **:8080**); the SPA
on **:8088**. Flyway only **validates** the existing schema — it never recreates it.

## Prerequisites
- Docker + Docker Compose on the host.
- Network reach from the host to the existing PostgreSQL `saude-mandaguari`.
- A read/write DB user for the new app (same DB as legacy).

## 1. Configure secrets
```bash
cp .env.prod.example .env.prod      # .env.prod is git-ignored — never commit it
# edit .env.prod and set:
#   DB_URL / DB_USER / DB_PASSWORD   → the existing saude-mandaguari DB
#   JWT_SECRET            → openssl rand -base64 48   (>= 32 bytes; NOT a placeholder)
#   CORS_ALLOWED_ORIGINS → the SPA's public origin(s), comma-separated
#   CERT_ENC_KEY         → openssl rand -base64 32    (AES key for SAU_PRO cert password)
```

## 2. Build & start
```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps          # both healthy?
curl -fsS http://localhost:8090/actuator/health        # backend → {"status":"UP"}
```
The backend runs `SPRING_PROFILES_ACTIVE=prod` (pinned in the compose file). `ProductionSecurityGuard`
**aborts startup** if a dev/placeholder `JWT_SECRET` (or the dev login backdoor) leaks in — so a green
boot is itself a security check.

### Frontend builds are deterministic (offline)
`orval` reads the **committed** `frontend/openapi.json`, so `npm run gen:api` — and the CI / frontend-image
builds — run **offline** with no backend. The generated client (`src/lib/generated/`) stays git-ignored and
is regenerated during the build. Both prod images build from a clean checkout.

**Refresh the spec after an API change** (the only step that needs a backend — via Testcontainers, no real DB):
```bash
cd backend && mvn -o -Dit.test=OpenApiSpecExportIT -Dopenapi.export=true \
  -Dsurefire.failIfNoSpecifiedTests=false -Dtest=NoneX -DfailIfNoTests=false verify
# rewrites frontend/openapi.json — commit it; the SPA regenerates its client from it.
```

## 3. What `prod` enforces (vs dev)
- Real SAU_USU authentication (the `admin/admin123` dev backdoor is **not** loaded).
- All secrets + CORS origins required from env — no fallbacks; the app fails fast if any is missing.
- API docs / Swagger UI **disabled**; actuator reduced to `/health` with no details.
- SQL never logged; app at INFO, framework at WARN.
- `ddl-auto=validate`; Flyway adopts the existing DB as baseline (no DDL changes).

## 4. Cut a route over
Front the two apps with a reverse proxy and move one path at a time from legacy (:8080) to the new SPA
(:8088) / backend (:8090) as each slice reaches `verified` via `/verify-parity`. Roll back by pointing
the path back at legacy.

## 5. CI
`.github/workflows/ci.yml` runs backend (`mvn verify`, incl. Testcontainers), frontend
(typecheck/test/build), and a `docker-build` job that builds both images (no push) so the Dockerfiles
stay green. Parity tests run separately (need the legacy app reachable): `mvn -B -Pparity verify`.

## ⚠ Not yet covered
- No image registry push / orchestration (k8s) manifests — add when a target platform is chosen.
- TLS termination is expected at the fronting reverse proxy / load balancer (not in these containers).
