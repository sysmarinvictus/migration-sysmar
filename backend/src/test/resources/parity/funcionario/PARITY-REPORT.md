# Parity Report — SAU_FUN (Funcionário)

- **Date:** 2026-06-30
- **Verdict:** **BEHAVIORAL PARITY** (7/7 scenarios) — the new endpoints reproduce the live snapshot data
  and the mined rules, end-to-end IT-proven on identical DDL. **Live HTTP golden-master run deferred**
  (sandbox can't sustain a long-lived server) → run via `/dev-stack` in a real terminal to stamp `verified`.
- **Target:** new app vs the shared non-prod `saude-mandaguari` snapshot (host.docker.internal). Legacy
  GeneXus on :8080 — its `hwwsau_fun` WW is **auth-gated**, so HTML golden-master is not headlessly
  capturable (same as SAU_PRO/SAU_IMP/SAU_USU); parity is established behaviorally vs the live DB + rules.

## Why behavioral (not HTML golden-master)
SAU_FUN's legacy Work-With is behind the GeneXus login, so there is no anonymous golden to scrape. The
authoritative comparison is therefore **new endpoint result == live snapshot row** for real funcionários,
plus the mined-rule behaviors. The 12 `FuncionarioControllerIT` tests run the full HTTP→service→JPA→Postgres
path on a Testcontainers DB built from the **same V1 baseline DDL** as production, and I confirmed the
SAU_FUN/SYS_PES column types live before mapping — so the IT behavior transfers to the live schema.

## Golden data (live snapshot, PHI-redacted)
Captured read-only; rows referenced by `FunPesCod` + metadata only (never names/CPF):

| metric | live value |
|---|---|
| SAU_FUN total | 1148 |
| FunSit=1 (Ativo) / =2 (Inativo) | 1147 / 1 |
| with work phone (FunTraFon) | 6 |
| linked to a system user (SAU_USU.FunPesCod) | 1086 (94%) |
| name LIKE '%silva%' (search golden) | 117 |
| sample rows 57 / 436 / 597 | FunSit=1, no work-phone, no ramal, CPF len 14 |

## Scenario results

| # | Scenario | Rule | Verdict | Evidence |
|---|----------|------|---------|----------|
| 1 | GET maps own cols + SYS_PES person | R2 | BEHAVIORAL-PARITY | IT getsByIdAnd404; live rows 57/436/597 |
| 2 | list/search by person name (LIKE) | — | BEHAVIORAL-PARITY | IT listsAndSearchesByPersonName; golden silva=117 |
| 3 | create needs existing person; situação default 1 | R1/R5 | BEHAVIORAL-PARITY | IT createsForExistingPerson… (201, sit=1) + rejectsCreateForNonExistentPerson (422) |
| 4 | update writes back to SYS_PES | R2 | BEHAVIORAL-PARITY | IT updatesAndWritesBackPersonName (DB row mutated) |
| 5 | phone formats; ramal free; NO CPF validation | R6-R9/R12 | BEHAVIORAL-PARITY | ServiceTest (5) + IT rejectsInvalidWorkPhone |
| 6 | delete guards SAU_USU/SAU_RECESP | R13-R15 | BEHAVIORAL-PARITY | IT 3 delete tests (409/409/204); golden 1086 user-linked |
| 7 | soundex from name; PHI never logged | R3/R18 | BEHAVIORAL-PARITY | ServiceTest computesSoundexFromName + SecurityTest toStringOmitsSoundex |

## To stamp `verified`
Run the new app against the snapshot via `/dev-stack` (real terminal), obtain a SAUDE_CADASTRO token, and
hit `GET /api/funcionarios/57|436|597` + `?nome=silva` — confirm they match the golden table above. The
mapping is already IT-proven; this is the final live-HTTP confirmation the sandbox could not host.

## Notes
- No real PHI in this report or `scenarios.json` (Rule 4): only FunPesCod + situação + lengths/counts.
- Write scenarios are not run against the live shared SYS_PES (person table) — covered by Testcontainers ITs.
