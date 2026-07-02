# SAU_USUUNI (Usuário × Unidade — capability matrix) — Golden-master parity report
- **Date:** 2026-07-02 · **Method:** DB-state vs shared snapshot (READ-ONLY: GET + SELECT only) · **Auth:** minted HS256 JWT, role SAUDE_ADMIN
- **Snapshot:** `host.docker.internal:5432` db `saude-mandaguari`, table `sau_usuuni` (composite PK UsuCod,UniUsuCod) — **1427 rows** at start and finish (unchanged).
- **Type audit:** entity ↔ live types MATCH. UsuCod/UniCod/EspecialidadeCod integer↔Integer; all 54 flags native PG boolean↔Boolean (no smallint, no @Convert). DB has **0 NULLs** in any flag or espcod, so every comparison was an exact value match (no null→false coercion exercised).
- **Endpoints tested (read):** `GET /api/usuarios/{usuCod}/unidades`, `GET /api/usuarios/{usuCod}/unidades/{uniCod}`.
- **Write parity (POST/PUT/DELETE):** **DEFERRED** — READ-ONLY run; no mutation permitted. Insert/update/delete equivalence (R2/R3/R4/R6) to be covered in a mutating snapshot run before cutover (OQ4).

## Result: 14/14 PARITY

| # | Scenario | Inputs | Legacy/DB result | New API result | Equivalent? | Notes |
|---|----------|--------|------------------|----------------|-------------|-------|
| 1a | List fidelity — count | UsuCod 100046 | 30 rows | 200, 30 elems | ✅ | array size == COUNT(*) |
| 1b | List fidelity — count | UsuCod 1 | 29 rows | 200, 29 elems | ✅ | |
| 1c | List fidelity — count | UsuCod 100043 | 28 rows | 200, 28 elems | ✅ | |
| 1d | List fidelity — count | UsuCod 100145 | 28 rows | 200, 28 elems | ✅ | |
| 2 | Flag/field fidelity | 4 users × all rows (115 rows) | each row's uniCod + espcod + 54 booleans | exact match, every field | ✅ | **0 divergences** across ~6210 field comparisons (115 rows × 54 flags + espcod) |
| 3 | Zero unit-rows | UsuCod 2 (exists in SAU_USU, 0 usuuni rows) | 0 rows | 200, `[]` | ✅ | empty list, not 404 |
| 4a | Unknown user — list | UsuCod 999999 (absent from SAU_USU) | n/a | 404 "Usuário 999999 não encontrado" | ✅ | R1 (usuarioExists) gate |
| 4b | Unknown user — single GET | UsuCod 999999 / uni 1 | n/a | 404 | ✅ | |
| 4c | Known user, unknown unit | UsuCod 1 / uni 999999 | no row | 404 "Acesso ... não encontrado" | ✅ | |
| 5a | Single GET, espcod≠0 | UsuCod 100229 / uni 11 | usuuniespcod=1130 | especialidadeCod=1130 | ✅ | non-zero FK to SAU_ESP preserved |
| 5b | Type fidelity | UsuCod 1 / uni 1 | int PK/FK, boolean flags | usuCod/uniCod/especialidadeCod = JSON int; all 54 flags = JSON true/false | ✅ | no bool-as-int, no int-as-bool |
| 6a | AuthZ — no token | (none) | — | 401 | ✅ | |
| 6b | AuthZ — SAUDE_CADASTRO only | CADASTRO JWT, list | — | 403 | ✅ | SAUDE_ADMIN required (R11) |
| 6c | AuthZ — SAUDE_CADASTRO only | CADASTRO JWT, single GET | — | 403 | ✅ | |
| — | AuthZ — garbage token | invalid JWT | — | 401 | ✅ | (extra hardening check) |

## Verdict: **PARITY (14/14)** — READ side.

### Notes / nuances (non-divergences)
- **SAUDE_ADMIN gating confirmed:** the whole controller is `@PreAuthorize("hasRole('SAUDE_ADMIN')")`. admin/admin123 (SAUDE_CADASTRO) → 403 as designed; only the minted SAUDE_ADMIN token reads the matrix. This is an intentional migration decision (spec R11 / OQ3): legacy gated via the SAU_USU program permission; the modern slice treats this authorization-source table as ADMIN-only.
- **Zero-row vs unknown-user distinction:** a user present in SAU_USU with no usuuni rows → `[]` 200 (scenario 3); a user absent from SAU_USU → 404 (scenario 4). Both are correct per R1.
- **No NULLs in snapshot:** all 1427 rows have concrete booleans and espcod, so the mapping was verified against real true/false values, not defaulted nulls. The null→(not blocked/not granted) semantics (R5) remain untested by this data but are not reachable via read of this snapshot.
- **especialidadeCod:** DB stores non-null 0 or a real SAU_ESP code; API echoes it as an int (0 or code) — exact. No null-espcod rows exist to test the null→0 branch.

### Write-parity DEFERRED (blockers to clear before cutover)
- R2 (UniUsuCod exists → 422), R3 (espcod≠0 exists → 422), R4 (composite-PK dup → 409), R6 (unconditional delete → 204), R10 (audit) require a mutating snapshot run. Not a divergence — simply out of scope for this READ-ONLY pass.

## Safety confirmation
- **ZERO writes.** Only HTTP GET and SQL SELECT were issued. `sau_usuuni` row count = **1427 before and after**; user-1 rows = **29 before and after**.
- **LGPD/PHI:** this table is an authorization matrix (numeric codes + boolean flags) with no patient data (`phi_fields: []`). Fixtures reference users by numeric UsuCod only. PHI scan: none present, nothing redacted.

## Fixtures
- `PARITY-REPORT.md` (this file)
- `scenarios.json` — replayable scenario definitions (users, expected counts, espcod checks, auth expectations)
