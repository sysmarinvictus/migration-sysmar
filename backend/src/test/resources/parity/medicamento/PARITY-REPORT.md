# SAU_REM — Golden-master parity report

- **Date:** 2026-06-21
- **New app:** Spring Boot `:8090`, profile `local`, `ddl-auto=none`, Flyway disabled
- **Legacy:** GeneXus `ReceituarioJavaEnvironment` (Tomcat `host.docker.internal:8080`)
- **Method:** DB-state comparison against the shared non-prod snapshot (`saude-mandaguari`, 799 SAU_REM rows)
- **Auth:** HS256 JWT, role `SAUDE_CADASTRO`
- **Data hygiene:** created rows use the `ZZREM%` marker; all removed in teardown (snapshot back to 799 rows, 0 residue). Synthetic FK rows (InteracaoMedicamentosa) cleaned up.

## Result: 23 / 25 PARITY · 2 DIVERGE (both the deferred receita-lookup, RD6)

| # | Scenario | Expected | Got | Verdict |
|---|----------|----------|-----|---------|
| 1 | list default | 200 | 200 | ✅ |
| 2 | list filter nome | 200 | 200 | ✅ |
| 3 | list filter tipoMedicamentoCodigo | 200 | 200 | ✅ |
| 4 | list filter situacao | 200 | 200 | ✅ |
| 5 | list filter psicotropico | 200 | 200 | ✅ |
| 6 | list filter controleEspecial | 200 | 200 | ✅ |
| 7 | GET detail (existing) | 200 | 200 | ✅ |
| 8 | GET detail unknown | 404 | 404 | ✅ |
| 9 | POST minimal | 201 | 201 | ✅ |
| 10 | POST full (sub-levels via sub-resources, RD1) | 201 | 201 | ✅ |
| 11 | POST duplicate nome (allowed) | 201 | 201 | ✅ |
| 12 | POST invalid TipRemCod | 422 | 422 | ✅ |
| 13 | POST controleEspecial=1 | 201 | 201 | ✅ |
| 14 | PUT nome + situacao | 200 | 200 | ✅ |
| 15 | add SAU_REM1 row (RD1) | 201 | 201 | ✅ |
| 16 | remove EAN-13 row (RD1) | 204 | 204 | ✅ |
| 17 | SAU_REM_UNISETOR uniqueness (R30) | 409 | 409 | ✅ |
| 18 | DELETE unused | 204 | 204 | ✅ |
| 19 | DELETE w/ SAU_RECESP1 ref (R21) | 409 | 409 | ✅ |
| 20 | DELETE w/ SAU_REMLOT ref (R22) | 409 | 409 | ✅ |
| 21 | DELETE w/ InteracaoMedicamentosa ref (R19/R20) | 409 | 409 | ✅ |
| 22 | lookup by nome | 200 | 200 | ✅ |
| 23 | lookup receita context | 200 | 404 | ❌ DIVERGE (RD6) |
| 24 | MPP set (201) + unset w/ motivo (200), R44 | 201/200 | 201/200 | ✅ |
| 25 | controleEspecial visible in receita lookup | 200 | 404 | ❌ DIVERGE (RD6) |

## Divergences
Both #23 and #25 hit `GET /api/medicamentos/lookup/receita`, which is **not implemented (RD6 / OQ-8)** —
the prescription-context lookup needs the legacy filter-predicate comparison before it can be ported.
**Disposition:** known deferral; cutover caveat for the prescription-lookup feature only. The main
SAU_REM catalog transaction (CRUD + sub-levels + all guards + regulatory rules) is at full parity.

## Schema correction found during this run (BLOCK, fixed)
Parity surfaced a spec-vs-reality type mismatch (OQ-1): `RemMPP`, `RemOmitirSaldo`, `RemSemRename`,
`RemUsarPosologia` are **BOOLEAN** in the live DB, not smallint as the spec/V3 declared. The entity
mapped them as `Short`, causing `SQLState 22003 "Bad value for type short : f"` on every read.
Fixed: entity fields + DTOs → `Boolean`; V5 idempotently converts the columns to BOOLEAN for the
Flyway-built (Testcontainers) schema; frontend uses boolean. Re-verified — full `mvn verify` green
and all 25 scenarios run.
