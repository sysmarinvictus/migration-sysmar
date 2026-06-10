# Receituário — Modernized (Spring Boot 3 + React)

Modern replacement for the GeneXus webapp `Receituario`, built by the **migration factory** in
`../.claude` following a **strangler-fig** strategy (one transaction-domain at a time) against the
**existing** `saude-mandaguari` PostgreSQL schema.

- Migration plan & contracts: `../.planning/migration/` (`ARCHITECTURE.md`, `BACKLOG.md`, `slices/`).
- Reference slice implemented end-to-end: **Especialidade (`SAU_ESP`)**.

## Stack
- **Backend:** Java 21, Spring Boot 3.4, Spring Data JPA, Flyway, Spring Security + JWT, springdoc/OpenAPI, MapStruct.
- **Frontend:** React 18 + TypeScript + Vite, TanStack Query, React Hook Form + Zod, React Router, Tailwind.
- **Tests:** JUnit 5 + Mockito + AssertJ (unit), Testcontainers + RestAssured (integration), golden-master parity (profile `parity`).

## Prerequisites (install on this machine)
JDK 21, Maven, Docker (for Testcontainers + local Postgres), Node 20+. Quick JDK/Maven via SDKMAN:
```bash
curl -s "https://get.sdkman.io" | bash && source ~/.sdkman/bin/sdkman-init.sh
sdk install java 21.0.5-tem && sdk install maven
```

## Run locally
```bash
# 1) Postgres (empty; Flyway runs the V1 partial baseline)
docker compose up -d postgres

# 2) Backend  (set real secrets via env; do NOT commit them)
export DB_PASSWORD=change_me_local_only JWT_SECRET=dev-local-insecure-secret-change-me-please-32b
cd backend && mvn spring-boot:run        # http://localhost:8080  (Swagger: /swagger-ui.html)

# 3) Frontend
cd ../frontend && npm install
npm run gen:api    # generate the typed client from the running backend's OpenAPI
npm run dev        # http://localhost:5173   (dev login: admin / admin123 — DEV ONLY)
```

## Test
```bash
cd backend && mvn verify          # unit + Testcontainers integration tests
mvn -Pparity test                 # golden-master parity vs the running GeneXus app (capture first)
cd ../frontend && npm run test     # Vitest
```

## Important constraints
- **Existing schema is preserved.** Entities pin `@Table`/`@Column` to GeneXus physical names; clean
  names appear only in DTOs/UI. `ddl-auto=validate` — Hibernate never alters the schema; Flyway owns DDL.
- `V1__baseline.sql` is a **partial** baseline (only migrated tables). `gx-schema-mapper` must
  introspect the live DB and complete it before broad rollout.
- **Auth:** the `SAU_USU` password scheme is an open question (Wave 0). A `DevUserDetailsService`
  stub backs login in dev/test; replace it with the SAU_USU-backed implementation in the auth slice.
- **No secrets in the repo.** DB/JWT secrets come from env vars (a PreToolUse hook blocks committing them).
- **LGPD/PHI:** audit PHI access (`common/audit`), never log PHI. `SAU_RECESP` (controlled
  substances, Portaria 344/98) needs regulatory sign-off before cutover.

## How to migrate the next slice
From the repo root (where `.claude` lives):
```
/migration-status                 # see the backlog + recommended next slice
/gx-extract <SAU_XXX>             # produce a reviewable SLICE-SPEC
/migrate-slice <SAU_XXX>          # generate backend + frontend + tests
/verify-parity <SAU_XXX>          # golden-master vs legacy, then cut the proxy route over
```
