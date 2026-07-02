# SAU_RBAC (Autorização — per-program permission engine) — Golden-master parity report

- **Date:** 2026-07-02 · **Method:** DB-state vs shared snapshot (READ-ONLY; zero writes) · **Auth:** JWT SAUDE_ADMIN
- **Endpoints covered:** `GET /api/autorizacao/perfis/{perfilId}/permissoes`, `GET /api/autorizacao/usuarios/{usuCod}/permissoes`, `GET /api/autorizacao/check?usuCod=&programaId=&mode=`
- **Type audit:** entity ↔ live types MATCH. `sau_prfcon`(prfcod int, prfprgcod varchar, prfprg{con,inc,alt,exc} smallint), `sau_usucon`(usucod int, prgcod varchar, usu{con,inc,alt,exc} smallint), `sau_usu`(usubloq smallint, usuprfcod int, ususysmar boolean). Flags smallint→boolean(==1) as coded.

## SCOPE (read this first)
- **Legacy oracle = the `pisauthorized` precedence rules mined from SAU_USU R8/R2/R5**, resolved directly against the **shared snapshot DB rows** (the exact same tables both apps read). GeneXus emits encrypted-param HTML for these grids, so DB-row equivalence is the reliable oracle here (per ARCHITECTURE §6 method 1/2).
- **Engine-not-wired:** the RBAC engine is BUILT + TESTED but per-program enforcement is **NOT** wired into other migrated endpoints yet (they still use coarse SAU_USU roles — OQ1). This parity therefore covers **only the AutorizacaoController read/resolve surface** (grids + `/check` oracle). It does **not** assert that any business endpoint enforces these permissions.
- **Write-parity DEFERRED:** `PUT /perfis|usuarios/{}/permissoes/{programaId}` (permission-grant upsert) NOT exercised — READ-ONLY run, no DB mutation. Deferred to a snapshot-copy write-parity pass.
- **Grid endpoints return the RAW matrix row, not effective/resolved permission.** `/usuarios/{usuCod}/permissoes` returns the user's SAU_USUCON grid verbatim (e.g. user 669 returns its 1233 usucon rows even though the resolver would use its profile). Effective resolution (precedence) is asserted via `/check`. This is the documented maintenance-grid behavior, not a divergence.

## Result: 13/13 PARITY · verdict PARITY

| # | Scenario | Inputs | Legacy/DB result | New API result | Equivalent? | Notes |
|---|----------|--------|------------------|----------------|-------------|-------|
| 1a | Profile grid | perfil 2 | 1232 rows (100 any-granted) | 1232 rows, 0 value-diff | ✅ | full row-by-row match (C-locale sort) |
| 1b | Profile grid | perfil 3 | 1232 rows (90 granted) | 1232 rows, 0 value-diff | ✅ | full match |
| 1c | Profile grid | perfil 7 | 1232 rows (14 granted) | 1232 rows, 0 value-diff | ✅ | full match; 14 granted programs identical |
| 2a | User grid | usuCod 100216 (no valid profile) | 1240 usucon rows | 1240 rows, 0 value-diff | ✅ | fallback-tier user; raw grid exact |
| 2b | User grid | usuCod 669 (profile 1) | 1233 usucon rows | 1233 rows | ✅ | raw grid (not resolved) as designed |
| 3a | check — tier2 profile ALLOW | 669 / ARSAU_ATEMED / CON | prfcon(prf1) con=1 | granted=true | ✅ | profile grant |
| 3b | check — tier2 profile DENY | 669 / AGENDA / CON | prfcon(prf1) con=0 | granted=false | ✅ | profile deny |
| 3c | check — tier3 fallback ALLOW | 100216 / AGENDA / CON | usucon con=1, no valid profile | granted=true | ✅ | per-user fallback |
| 3d | **Precedence (NOT OR-ed)** | 669 / AGENDA / CON | usucon con=**1** but prfcon(prf1) con=**0** | granted=**false** | ✅ | **decisive:** profile takes precedence, per-user grant ignored |
| 4 (R5) | check — blocked user | 2 / AGENDA / CON | usubloq=1, usucon con=1 | granted=false | ✅ | **R5 before R2/tiers** — block wins over a granted row |
| 5 (R2) | check — sysmar bypass | 1 / SAU_PAC / EXC ; 1 / NONEXISTENT_PRG / EXC | ususysmar=true | granted=true (both) | ✅ | superuser flag grants all, even unknown program |
| 6 | check — unknown user | 99999999 / AGENDA / CON | no sau_usu row | granted=false | ✅ | fail-closed |
| 7 | Empty grids | perfil 88888 / usuCod 99999999 | no rows | `[]` HTTP 200 (both) | ✅ | empty list, not 404 |

### AuthZ (Scenario 4)
| Case | Input | Expected | Actual | ✅ |
|------|-------|----------|--------|----|
| No token | GET /perfis/1/permissoes | 401 | 401 | ✅ |
| SAUDE_CADASTRO-only JWT | GET /perfis/7/permissoes | 403 | 403 | ✅ |

## Business-rule confirmations (pisauthorized engine, R8/R2/R5)
- **R5 (blocked) evaluated before R2 (sysmar) and before both grant tiers** — user 2 has a granted usucon row yet resolves DENY. ✅
- **R2 sysmar flag** grants every (program, mode) including a non-existent program. ✅
- **Profile precedence, NOT OR** — the load-bearing rule: user 669 usucon AGENDA/CON=1 but profile-1 AGENDA/CON=0 → resolver returns false. A naive OR would have leaked access. ✅
- **Fail-closed** on unknown user and missing permission row. ✅

## Divergences
None. No business divergences (value, count, grant/deny outcome, or error condition). The only raw diffs observed were **row ordering** (GeneXus/DB collation vs API JSON order) — ignored per business-equivalence rules; re-diff under identical C-locale sort yielded 0 value differences for all grids.

## OQ4 / OQ5 status
- **OQ4 (parity vs pisauthorized incl. blocked-vs-sysmar + 3 tiers):** SATISFIED by scenarios 3a–3d, 4(R5), 5(R2), 6. This is the mandatory pre-wiring gate.
- **OQ5 (PrgAcessoPub tier-0):** NOT covered here — resolver is fail-closed (stricter than legacy) and does not consult the public-access flag. Semantic confirmation vs legacy `pisauthorized` still open before wiring; not a blocker for this read/resolve surface.

## Safety / LGPD
- READ-ONLY: only GET requests issued. **ZERO writes** to DB or via API. No POST/PUT/DELETE. Snapshot DB (`host.docker.internal`, non-prod) untouched.
- No PHI in this slice (RBAC catalogs/matrices). Users referenced by numeric UsuCod only; no usernames/CPF/PII persisted in this report or fixtures. PHI-scan: clean.

## Verdict: **13/13 PARITY** — resolve/read surface clears OQ4. Write-parity (PUT grant) and OQ5 (PrgAcessoPub) remain deferred before endpoint wiring (OQ1).
