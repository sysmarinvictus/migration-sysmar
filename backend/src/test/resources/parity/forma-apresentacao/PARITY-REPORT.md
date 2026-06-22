# SAU_APRREM (Forma de Apresentação) — Golden-master parity report

- **Date:** 2026-06-21
- **New app:** Spring Boot `:8090`, profile `local`, `ddl-auto=none`, Flyway disabled
- **Legacy:** GeneXus `ReceituarioJavaEnvironment` (Tomcat `host.docker.internal:8080`)
- **Method:** DB-state comparison against the shared non-prod snapshot (`saude-mandaguari`)
- **Auth:** HS256 JWT, role `SAUDE_CADASTRO`
- **OQ-5 note:** SAU_APRREM is **empty (0 rows)** in this snapshot — the table wasn't carried over. All scenarios **self-seed** (marker `ZZAPR%`) and clean up; table restored to 0 rows, 0 residue.

## Result: 11 / 11 PARITY — no divergences ✅

| # | Scenario | Expected | Got | Verdict |
|---|----------|----------|-----|---------|
| 1 | list default (sort descricao) | 200 | 200 | ✅ |
| 2 | list filter by descricao | 200 (1 match) | 200 | ✅ |
| 3 | GET by id | 200 | 200 | ✅ |
| 4 | GET by id unknown | 404 | 404 | ✅ |
| 5 | create valid → 201, descricao+abreviacao UPPERCASE (R1/R4/R5) | 201, "ZZAPR COMPRIMIDO"/"ZAC" | 201, match | ✅ |
| 6 | create blank descricao (R2) | 422 | 422 | ✅ |
| 7 | create blank abreviacao (R3) | 422 | 422 | ✅ |
| 8 | update → 200, UPPERCASE | 200, "ZZAPR XAROPE" | 200, match | ✅ |
| 9 | delete unused | 204 | 204 | ✅ |
| 10 | delete referenced by Medicamento (R7) | 409 | 409 | ✅ |
| 11 | lookup by descricao | 200 | 200 | ✅ |

All mined rules exercised at parity: R1 (sequence code), R2/R3 (required, exact PT messages),
R4/R5 (UPPERCASE storage), R7 (delete-guard vs SAU_REM → 409), plus list/lookup/404/update.
No behavioral divergences from legacy. Optimistic-lock half of R9 remains deferred (no version
column — recorded as RD1 in the spec; out of scope for a lookup table).
