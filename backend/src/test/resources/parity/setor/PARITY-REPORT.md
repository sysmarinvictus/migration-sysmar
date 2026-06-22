# SAU_UNISETOR (Setor da Unidade) — Golden-master parity report
- **Date:** 2026-06-22 · **Method:** DB-state vs shared snapshot (80 rows; tested on unidade 74)
- **New app:** :8090, ddl-auto=none, Flyway off · **Auth:** JWT SAUDE_CADASTRO
- **Type audit:** entity ↔ live types all MATCH (estocador smallint↔Short, timestamps↔LocalDateTime).

## Result: 12/12 live PARITY · 1 deferred · 3 guards code-verified
POST valid/minimal ✅ · POST duplicate composite→409 ✅ · POST bad UniCod→422 ✅ ·
PUT nome→UPPERCASE ✅ · PUT situacao=inativo→SetorDataInativo set ✅ · PUT unknown→404 ✅ ·
DELETE unreferenced→204 ✅ · DELETE blocked by SAU_REM_UNISETOR (R15)→409 ✅ (seeded) ·
GET list filter (LIKE)→200 ✅ · GET lookup (per-unit)→200 ✅ · GET unknown composite→404 ✅.

- **Optimistic concurrency (scenario 8): DEFERRED** — no @Version column (RD1, consistent across slices).
- **Delete-guards R12/R13/R14 (SAU_PAR5 / SAU_USUUNI1 / SAU_REMLOT): code-verified** — the repo queries use
  the correct referencing columns (ParSalUniCod/SetorCod, UniUsuCod/UsuSetorCod, RemUniCod/RemSetorCod) and the
  guard mechanism is proven live via R15 (REM_UNISETOR). Same sequential-check pattern.
No business divergences. Snapshot restored (unidade 74 back to its 1 original setor).
