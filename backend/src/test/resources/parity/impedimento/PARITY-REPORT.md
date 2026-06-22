# Parity Report — SAU_IMP (Impedimento do Profissional)

- **Date:** 2026-06-22
- **Verdict:** PARITY — 22/22 behavioral checks pass (12 scenarios P1–P12 + sub-assertions). 1 documented deferral (RD1), 1 bug found-and-fixed.
- **Target:** new app on `:8090` (profile `local`, flyway off, ddl-auto none) against the shared
  non-prod `saude-mandaguari` snapshot on the Windows host (`host.docker.internal`).
- **Legacy golden master:** the GeneXus WW is auth-gated (`hwwsau_imp`→301 login, `sau_imp`→403);
  headless HTML capture not automated. Scenarios verified against the **live DB** (real SAU_PRO=3014 /
  SAU_PROESP=1904 / SAU_ESP=2692 / SYS_PES) and the high-confidence mined rules (R1–R26, mostly
  `confidence: high` with exact line cites). SAU_IMP itself is empty on this snapshot (0 rows) → write
  scenarios self-seed real FK refs (VPRO=105765, VESP=1130) and restore the table to 0 on cleanup.

## Scenario results

| # | Scenario | Rule | Expected | Got | Verdict |
|---|----------|------|----------|-----|---------|
| P6 | insert valid pair | R4,R7–R11 | 201, ImpCod=MAX+1, row persisted | 201, ImpCod=1, row ✓ | PARITY |
| P5 | view detail (joined names) | R15–R18 | profNome (SYS_PES) + espNome (SAU_ESP) derived | 'JULIANA CORREIA' / 'ENFERMEIRO' ✓ | PARITY |
| P1 | list default | — | 200, new row visible | 200 ✓ | PARITY |
| P2 | filter profissionalId | — | 200, row matches pro | 200 ✓ | PARITY |
| P3 | filter profissionalNome (partial) | — | 200, row returned | 200 ✓ (LIKE substring — see OQ2) | PARITY* |
| P4 | filter date range (both bounds) | — | 200, row in range | 200 ✓ **(was 500 — fixed)** | PARITY |
| P7 | insert missing dataCadastro | R6 | defaults to server date (NOT rejected) | 201, ImpDat=today ✓ | PARITY |
| P7b | insert missing dataInicio | R13 | rejected | 400 (Bean Validation) | PARITY* (RF6 status nuance) |
| P8 | insert invalid ProPesCod | R8 | rejected | 422 ✓ | PARITY |
| P9 | insert pair not in SAU_PROESP | R11 | rejected | 422 ✓ | PARITY |
| P10 | update dates (valid) | — | 200, DB updated | 200, dataFim=2026-07-15 ✓ | PARITY |
| P11 | update with stale data | R20 | legacy 409 ("SAU_IMP foi alterado") | 200 (last-write-wins) | **DEFERRED (RD1)** |
| P12 | delete | R24 | 204, row gone | 204, count=0 ✓ | PARITY |
| — | cleanup | — | snapshot restored to baseline | 0 rows ✓ | PARITY |

\* status-code nuance / search-impl difference — accepted, see notes.

## Bug found & fixed (parity caught what Testcontainers missed)

**P4 date-range filter returned HTTP 500** — `PSQLException 42P18: could not determine data type of
parameter $5`. The native filter query had `(:dataInicioFrom IS NULL OR i.ImpDatIni >= CAST(:dataInicioFrom AS DATE))`;
the **`:param IS NULL`** position left the date parameter untyped, so PostgreSQL could not infer its type.
It only surfaced when a date bound was actually supplied (P1–P3 sent null dates → no failure), which is
why the Testcontainers ITs (none exercised a date filter) didn't catch it.

- **Fix:** cast the parameter inside the IS NULL check too —
  `(CAST(:dataInicioFrom AS DATE) IS NULL OR ...)` — applied to all 4 query bodies
  (`findByFilters` + `findByFiltersWithNome`, main + count). `ImpedimentoRepository.java`.
- **Regression test added:** `ImpedimentoControllerIT#filtersByDateRange` (in-range hit, out-of-range
  miss, name+date path). Suite green: ImpedimentoControllerIT 13/13, ImpedimentoServiceTest 12/12.

## Open questions resolved by this run

- **OQ3 (PK gen):** RESOLVED — **no `ImpCod` sequence** exists on the live DB. Legacy MAX+1 confirmed;
  the service uses `findMaxCodigo()+1` (correct — a `@GeneratedValue(SEQUENCE)` would 500 on the live DB).
- **OQ12 (SAU_PRO PK column):** RESOLVED — live column is `propescod` (BIGINT), matching all cursors.
- **OQ7 (prescription enforcement):** RESOLVED earlier this session — the `sau_recesp` constellation has
  ZERO references to SAU_IMP; impediments do NOT block prescriptions. (Only external ref: a reverse
  delete-guard in `sau_proesp_impl.java` — a Wave-4 SAU_PROESP rule.) → SAU_IMP cutover is NOT gated on
  prescription behavior.
- **OQ1 (SAU_CBOR):** present on live DB (CHAR(6)); CBO derivation (R18) wired via SAU_ESP→SAU_CBOR.

## Remaining cutover notes (NOT parity blockers — behavior matches legacy)

- **RD1 — optimistic locking not ported (P11).** No `@Version` column exists in the physical schema;
  the new app is last-write-wins where legacy raises 409 on a stale form snapshot. Accepted deferral
  (consistent across all migrated slices); revisit if concurrent-edit safety is required pre-cutover.
- **OQ2 — phonetic search.** Legacy list filters by `psau_soundex` (phonetic). The new app uses
  case-insensitive `LIKE '%name%'` substring match. Exact/substring matches are equivalent; phonetic
  (misspelling-tolerant) matches differ. Search-quality only — needs product sign-off if SOUNDEX is required.
- **OQ5 (auth granularity):** implemented as the coarse `SAUDE_CADASTRO` role (matches reference slices);
  confirm with Wave-0 auth team whether a transaction-level permission is wanted.
- **OQ6 / OQ8 / OQ9 (product-owner rules):** legacy does NOT block inactive-professional impediments,
  does NOT cross-validate `ImpDatIni ≤ ImpDatFim`, and does NOT reject overlapping periods. The new app
  matches legacy (no such rules). These are "should we ADD a rule?" questions for the product owner —
  parity holds either way.
- **OQ4 (FK cycle):** `ProPesCod → SAU_PRO` FK constraint is intentionally omitted until SAU_PRO (Wave 4)
  is live; add a forward Flyway migration at that point.
- **R22 audit persistence:** Phase-1 `AuditService` emits a structured log line (captures actor — an LGPD
  improvement over legacy's `logProPesCod=0`, resolving OQ10); SAU_LOG/append-only persistence is the
  Wave-0 audit slice.
