# SAU_REMOBS (Posologia) — Golden-master parity report

- **Date:** 2026-06-21 · **Method:** DB-state vs shared snapshot (2620 SAU_REMOBS rows)
- **New app:** :8090, ddl-auto=none, Flyway off · **Auth:** JWT SAUDE_CADASTRO
- **Hygiene:** synthetic rows marked `ZZPOSO%` (codigo auto MAX+1), deleted in teardown → restored to 2620.

## Result: 11 / 11 PARITY

| # | Scenario | Expected | Got | Verdict |
|---|----------|----------|-----|---------|
| 1 | list default | 200 | 200 | ✅ |
| 2 | get by id (existing, 1) | 200 | 200 | ✅ |
| 3 | get by id unknown | 404 | 404 | ✅ |
| 4 | insert valid (descricao only, codigo auto) | 201 | 201 (codigo 2621) | ✅ |
| 5 | insert missing descricao (R2) | reject | 400* | ✅ rejects |
| 6 | update descricao | 200 | 200 | ✅ |
| 7 | update optional fields (internamento/quantidadeDose/…) | 200 | 200 | ✅ |
| 8 | delete unused | 204 | 204 | ✅ |
| 9 | delete referenced by SAU_REMPOSO (R3) | 409 | 409 | ✅ |
| 10 | delete referenced by SAU_RECESP1 (R4) | 409 | 409 | ✅ |
| 11 | lookup by descricao (partial) | 200 | 200 | ✅ |

\* #5 rejects via 400 (Bean Validation `@NotBlank`) not 422 — behavior matches legacy; same status nuance (RF6). No business divergences. PK generated MAX+1 (R1); both delete-guards (R3 REMPOSO, R4 RECESP1) at parity.
