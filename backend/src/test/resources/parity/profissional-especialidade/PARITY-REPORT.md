# SAU_PROESP (Especialidade do Profissional) — Golden-master parity report

- **Date:** 2026-07-02
- **Method:** DB-state vs shared snapshot (1904 rows) · **Auth:** JWT SAUDE_CADASTRO · **READ-ONLY**
- **Endpoint under test:** `GET /api/profissionais/{proPesCod}/especialidades`
- **Scope:** master row only (Pri/Sit + Man/Tar/Noi aggregate quotas). Write-parity
  (add/update/remove, incl. the SAU_IMP delete-guard R5) is **DEFERRED** (see below).

## Type audit — entity ↔ live types (SMALLINT → Integer via @JdbcTypeCode)
| GX column | live type | DTO field | JSON type | Verdict |
|---|---|---|---|---|
| ProPesCod       | bigint   | profissionalId  | number  | MATCH (Long) |
| EspCod          | integer  | especialidadeId | number  | MATCH (Integer) |
| ProEspPri       | smallint | prioritario     | boolean | MATCH (SMALLINT→short→bool, R4) |
| ProEspSit       | smallint | situacao        | number  | MATCH (SMALLINT→Integer) |
| ProEspAgeManQtd | smallint | agendaManhaQtd  | number  | MATCH (SMALLINT→Integer) |
| ProEspAgeTarQtd | smallint | agendaTardeQtd  | number  | MATCH (SMALLINT→Integer) |
| ProEspAgeNoiQtd | smallint | agendaNoiteQtd  | number  | MATCH (SMALLINT→Integer) |

No type divergence: all 5 numeric cols round-trip smallint→Integer/short; no truncation/overflow
in the sampled range (values 0..2).

## Scenarios
| # | Scenario | Inputs (ProPesCod) | Legacy DB result | New API result | Equivalent? | Notes |
|---|---|---|---|---|---|---|
| 1 | List fidelity | 105227 | 19 rows | 200, 19 elems, row-for-row | ✅ | ordered by EspCod asc |
| 1 | List fidelity | 105242 | 7 rows | 200, 7 elems, row-for-row | ✅ | contains prioritario=1 case |
| 1 | List fidelity | 13916 | 6 rows | 200, 6 elems, row-for-row | ✅ | 4 prioritario=1, 2 sit=2 |
| 1 | List fidelity | 106765 | 1 row | 200, 1 elem | ✅ | single-specialty pro |
| 1 | List fidelity | 57 | 1 row | 200, 1 elem | ✅ | single-specialty pro |
| 2 | Flag semantics (R4) | 105242#2327, 13916#1049.. | ProEspPri=1 | prioritario=true | ✅ | pri==1 → true, else false; situacao==ProEspSit (1/2) |
| 3 | Zero specialties | 974 (in SAU_PRO, no SAU_PROESP) | 0 rows | 200, `[]` | ✅ | empty list, NOT 404 |
| 4 | Unknown professional (R1) | 99999999 (not in SAU_PRO) | n/a | 404 not-found | ✅ | requireProfissional; RFC-7807 body |
| 5 | Type fidelity | all sampled | smallint 0..2 | Integer/boolean, no divergence | ✅ | see type audit |
| — | Auth guard | 106765, no token | n/a | 401 | ✅ | SAUDE_CADASTRO required |

Row-by-row diff (EspCod|prioritario|situacao|manha|tarde|noite) was compared programmatically for
all 5 professionals (34 rows total) — **0 mismatches**.

## Result: 9/9 PARITY

List fidelity ✅ (5 pros, ordered by EspCod) · flag semantics R4 (pri→bool) ✅ · situacao integer ✅ ·
zero-specialty→`[]` 200 ✅ · unknown pro→404 (R1) ✅ · type fidelity (SMALLINT→Integer, 5 cols) ✅ ·
auth guard 401 ✅. No business divergences.

## Deferred (NOT covered by this READ-ONLY run)
- **Write parity** (POST add / PUT update / DELETE remove) — requires mutation; deferred per
  READ-ONLY mandate. Covered by Testcontainers IT (`ProfEspControllerIT`), not golden-master.
- **R5 SAU_IMP delete-guard** — delete blocked when SAU_IMP(ProPesCod,EspCod) exists → 409. Write
  path; deferred to write-parity session.
- **R3/R4 insert defaults** (sit=1, pri=0) — write path; deferred.
- **R6 / SAU_PROESP1** period schedule sub-slice — out of scope (OQ1, separate deferred slice).

## Safety
- **ZERO writes / ZERO rows created.** No POST/PUT/DELETE issued; no ZZPARITY rows.
- Post-run `SELECT count(*) FROM sau_proesp` = **1904** (unchanged from pre-run).
- **LGPD:** professionals referenced by numeric ProPesCod only; no professional names committed.
  EspCod values are catalog references (not PHI).
