# SAU_ESP (Especialidade) — Golden-master parity report

- **Date:** 2026-06-22 · **Method:** DB-state vs shared snapshot (2692 SAU_ESP rows)
- **New app:** :8090, ddl-auto=none, Flyway off · **Auth:** JWT SAUDE_CADASTRO

## Result: 9/9 PARITY (after fixing a real schema-mismatch blocker)

| # | Scenario | Verdict |
|---|----------|---------|
| 1 | list default | ✅ |
| 2 | get by id (existing) | ✅ |
| 3 | get by id unknown → 404 | ✅ |
| 4 | insert valid (code+name+cbor) → 201 | ✅ (after fix) |
| 5 | insert duplicate code → 409 | ✅ |
| 6 | insert missing name → reject (400) | ✅ |
| 7 | update name + agenda params → 200 | ✅ (agenda→SMALLINT, situacao→smallint persisted) |
| 8 | delete unused → 204 | ✅ |
| 9 | delete referenced by profissional (R4) → 409 | ✅ |

## Blocker found & fixed
The first run failed (insert/update → 500, `PSQLException 42804`): the entity's types did NOT match
the live production schema (Testcontainers passed only because V1 declared the same wrong types):
- `auxiliar` Boolean → live **EspAux is INTEGER**  → `@Convert(BooleanToIntegerConverter)` (0/1)
- `situacao` String  → live **EspSit is SMALLINT** (values 1/2) → `@Convert(SituacaoToShortConverter)`
- 16 × `agenda*` Integer → live **SMALLINT** → `@JdbcTypeCode(Types.SMALLINT)`
- **V7__sau_esp_type_alignment.sql** aligns the Flyway/Testcontainers schema to live (idempotent;
  no-op against the already-correct live DB).
Re-verified: full `mvn verify` green (173 unit / 246 IT) and all 9 scenarios PARITY. Snapshot restored (2692 rows).
