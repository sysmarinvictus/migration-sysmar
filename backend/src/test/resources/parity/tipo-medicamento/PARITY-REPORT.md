# SAU_TIPREM (Tipo de Medicamento) — Golden-master parity report

- **Date:** 2026-06-21
- **New app:** `:8090`, profile `local`, `ddl-auto=none`, Flyway disabled
- **Legacy:** GeneXus `ReceituarioJavaEnvironment` (`host.docker.internal:8080`)
- **Method:** DB-state comparison against the shared non-prod snapshot (12 SAU_TIPREM rows; code 35 referenced by SAU_REM)
- **Auth:** HS256 JWT, role `SAUDE_CADASTRO`
- **Hygiene:** synthetic rows use marker `ZZTIP%` (codes 999990+); deleted in teardown → table restored to 12 rows.

## Result: 10 / 10 PARITY

| # | Scenario | Expected | Got | Verdict |
|---|----------|----------|-----|---------|
| 1 | list default | 200 | 200 | ✅ |
| 2 | get by id (existing) | 200 | 200 | ✅ |
| 3 | get by id unknown | 404 | 404 | ✅ |
| 4 | insert valid (codigo + descricao) | 201 | 201 | ✅ |
| 5 | insert missing descricao (R2) | reject | 400 | ✅ rejects* |
| 6 | insert duplicate codigo (R1) | 409 | 409 | ✅ |
| 7 | insert codigo > 999999 (R1) | reject | 400 | ✅ rejects* |
| 8 | update descricao | 200 | 200 | ✅ |
| 9 | delete unused | 204 | 204 | ✅ |
| 10 | delete referenced by Medicamento (R3) | 409 | 409 | ✅ |

\* #5/#7 reject via **400 (Bean Validation:** `@NotBlank descricao` / `@Max(999999) codigo`) rather than
422 — the same status nuance documented across slices (RF6). Behavior matches legacy (both reject);
the client-supplied PK (R1 range + uniqueness) and the SAU_REM delete-guard (R3) are at full parity.
No business divergences.
