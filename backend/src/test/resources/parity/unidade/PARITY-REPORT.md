# SAU_UNI — Golden-master parity report

- **Date:** 2026-06-21
- **New app:** Spring Boot backend `:8090`, profile `local`, `ddl-auto=none`, Flyway disabled
- **Legacy:** GeneXus `ReceituarioJavaEnvironment` (Tomcat `host.docker.internal:8080`)
- **Equivalence method:** **DB-state comparison** against the shared non-prod snapshot
  (`saude-mandaguari`, 140 real `SAU_UNI` rows). Both apps read/write the same physical schema.
- **Auth:** HS256 JWT, role `SAUDE_ADMIN` (main writes) — minted from the local `JWT_SECRET`.
- **Data hygiene:** all created rows use the `ZZPARITY%` name marker and are deleted in teardown.
  Real professional/municipio identifiers used as FKs are **redacted** in committed fixtures.

## Result: 18 / 20 PARITY · 2 DIVERGE (both pre-documented, accepted)

| # | Scenario | Expected | Actual | Verdict |
|---|----------|----------|--------|---------|
| 1 | GET /api/unidades (list) | 200 | 200 | ✅ PARITY |
| 2 | GET /api/unidades/{id} (detail) | 200 | 200 | ⚠️ PARITY* |
| 3 | GET /api/unidades/lookup?q=CENTRO | 200 | 200 | ✅ PARITY |
| 4 | POST valid minimum → 201 (+ DB row, UniNom UPPERCASE) | 201 | 201 | ✅ PARITY |
| 5 | POST blank UniNom | 422 + "Informe o Nome da Unidade!" | 400 | ❌ DIVERGE (RF6) |
| 6 | POST invalid CNPJ | 422 "CNPJ inválido!" | 422 ✓msg | ✅ PARITY |
| 7 | POST invalid phone | 422 | 422 | ✅ PARITY |
| 8 | POST UniHiperdia=1 sem UniCnes | 422 "…CNES!" | 422 ✓msg | ✅ PARITY |
| 9 | POST UniOrgEmi sem DirCod | 422 "…Diretor Clínico!" | 422 ✓msg | ✅ PARITY |
| 10 | POST DirCod=AutCod, OrgEmi≠U/S (R26) | 422 "…Autorizador deve ser diferente do Diretor Clínico!" | 422 ✓msg | ✅ PARITY |
| 11 | PUT UniNom (lowercase) → stored UPPERCASE | 200, UPPERCASE | 200, "ZZPARITY RENOMEADA" | ✅ PARITY |
| 12 | PUT UniSit=2 (DESATIVADO) | 200, DB UniSit=2 | 200, UniSit=2 | ✅ PARITY |
| 13 | DELETE unidade sem filhos | 204 | 204 | ✅ PARITY |
| 14 | DELETE com filhos SAU_UNISETOR | 409 | 409 (row preserved) | ✅ PARITY |
| 15 | DELETE com referência SAU_USUUNI | 409 | 409 | ✅ PARITY |
| 16 | DELETE com referência SAU_REM1 | 409 | 409 | ✅ PARITY |
| 17 | POST /{id}/hiperdia-profissionais válido | 201 | 201 | ✅ PARITY |
| 18 | POST /{id}/hiperdia-profissionais sem DatInc | 422 "…Data de Inclusão!" | 422 ✓msg | ✅ PARITY |
| 19 | POST /{id}/sisprenatal-profissionais CBO inválido | 422 "CBO inválido…" | 201 | ❌ DIVERGE (RF2) |
| 20 | POST /{id}/salas SalaCod duplicado | 409 | 409 | ✅ PARITY |

\* **#2** returns 200 with the persisted columns, but the legacy-derived display fields
`municipioNome / UF / IBGE` and `uniIdENome` are **not** yet in the response (gap **RF4**, R30/R31).

## Divergences

### #5 — blank UniNom returns 400, not 422 (RF6)
The DTO's `@NotBlank` makes Bean Validation reject with **400** before the service-layer rule
(R5) runs, so the legacy message "Informe o Nome da Unidade!" is not surfaced. The field **is**
rejected — only the status code + error body differ.
**Disposition:** accepted divergence. Optional follow-up: route required-field failures through the
`BusinessRule → 422` handler for message/identical-contract parity (RF6).

### #19 — SISPRENATAL CBO not validated (RF2)
The CBO allow-list / existence check (R65/R66) is **deferred to Wave 4** (needs `SAU_ESP`/`SAU_PROESP`
lookups), so an invalid CBO is accepted (**201**) instead of rejected (**422**). Invalid CBO data can
be persisted on `SAU_UNI2` until then.
**Disposition:** known deferral (RF2). **Cutover caveat** for the SISPRENATAL sub-resource only — the
main `SAU_UNI` transaction is unaffected.

## Bug found & fixed during this run
- **Duplicate `SalaCod` silently upserted (was 201).** `UnidadeSubService.addSala` used
  `save()`, which JPA treats as merge for an assigned-ID entity, overwriting the existing room
  instead of conflicting. Added an `existsById` guard → now returns **409** (scenario #20 PARITY).
  Covered by `UnidadeSubServiceTest#addSalaRejectsDuplicateSalaCod`.
