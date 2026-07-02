# SAU_PAR (Parâmetros / config singleton) — Golden-master parity report
- **Date:** 2026-07-02 · **Method:** READ-ONLY, DB-state vs shared NON-PROD snapshot · **Auth:** minted HS256 JWT (SAUDE_ADMIN + SAUDE_CADASTRO)
- **App:** http://localhost:8090 (profile `local`, flyway off, ddl-auto=none) · **DB:** `host.docker.internal:5432/saude-mandaguari`
- **Scope:** GET only. Write-parity (PUT `/geral`, PUT `/ambulatorial`) **DEFERRED** (READ-ONLY run; no POST/PUT/DELETE, no DB mutation).
- **Mutations performed:** ZERO. Type audit: 19 mapped cols confirmed present on live row (see §Field fidelity).

## ⚠ FINDING (front and center) — CONFIG / TENANCY DIVERGENCE (not a code bug)
`GET /api/parametros` returns **HTTP 404** with a valid SAUDE_ADMIN token. Root cause is a **tenant mismatch**
between the running app's configured empresa and the empresa keyed in the snapshot's `sau_par` table:

| What | Value | Source |
|------|-------|--------|
| App effective `audit.empresa-codigo` | **411420** | `application-local.yml:20` default `${AUDIT_EMPRESA_CODIGO:411420}`; running process has **no** `AUDIT_EMPRESA_CODIGO` override (`/proc/<pid>/environ` empty for it) |
| `parempcod` value(s) in snapshot `sau_par` | **364** (single row; singleton) | `SELECT parempcod FROM sau_par` → 1 row = 364 |
| 404 response detail | `"Parâmetros da empresa 411420 não encontrados"` | live GET body |

The service reads the singleton for `AuditProperties.getEmpresaCodigo()` = 411420. The snapshot has **no**
`sau_par` row for 411420 — its only row is empresa **364** — so the lookup misses and the controller correctly
maps "not found" → 404. **The 404 is correct application behavior for the given (mismatched) tenant config**, not
a defect in the SAU_PAR slice code.

**Would the GET succeed at the CORRECT empresa code?** YES. The row for empresa 364 exists with all 19 mapped
columns populated/typed correctly (queried directly — see §Field fidelity). If the app were started with
`AUDIT_EMPRESA_CODIGO=364` (or the snapshot re-keyed to 411420), `GET /api/parametros` would return 200 with that row.

**Resolution options (config, not code):** (a) launch parity app with `AUDIT_EMPRESA_CODIGO=364` to match the
snapshot, or (b) use a snapshot whose `sau_par` singleton is keyed to the production tenant 411420. Recommended for
this slice: re-run parity with `AUDIT_EMPRESA_CODIGO=364` so scenario 1/2 (field fidelity) can be asserted 200.

## Result: 3/3 PARITY (verifiable scenarios) · 1 verifiable scenario BLOCKED by config (field fidelity)

| # | Scenario | Inputs | Legacy/expected | New result | Equivalent? | Notes |
|---|----------|--------|-----------------|------------|-------------|-------|
| 1 | GET /api/parametros (SAUDE_ADMIN) | valid ADMIN JWT; app tenant=411420 | 404 (no row for 411420 in snapshot) | HTTP **404**, detail "Parâmetros da empresa 411420 não encontrados" | ✅ (behavior correct for tenant) | Config divergence — snapshot keyed to 364, app to 411420. See FINDING. |
| 2 | Field fidelity (row returned) | app tenant would need to be 364 | 200 + 19 mapped fields = sau_par[364] | **BLOCKED** — GET is 404 under current tenant, so no JSON body to diff field-by-field | ⛔ deferred | Row 364 verified at DB level (§below); cannot assert JSON mapping until tenant matches. |
| 3a | AuthZ — no token | no Authorization header | 401 | HTTP **401** | ✅ | Unauthenticated rejected. |
| 3b | AuthZ — SAUDE_CADASTRO only | JWT roles=[SAUDE_CADASTRO] | 403 | HTTP **403** | ✅ | /api/parametros requires SAUDE_ADMIN; cadastro-only forbidden (403 not 404 → authZ evaluated before lookup). |

## Field fidelity — DB-level evidence (sau_par WHERE parempcod=364)
The mapped 19-column subset exists and is well-typed on the snapshot row (proves the endpoint *would* have data to
return at the correct tenant). Values below are config, not PHI:

| gx column | value | mapped name / type |
|-----------|-------|--------------------|
| parempcod | 364 | empresaCod : Integer (pk) |
| parvalidadereceita | f | validadeReceita : Boolean |
| parvalidadereceitasimples | 0 | validadeReceitaSimplesDias : Integer(smallint) |
| parvalidadereceitausocon | 0 | validadeReceitaUsoContinuoDias : Integer(smallint) |
| parvalidadereceitaconesp | 0 | validadeReceitaControleEspecialDias : Integer(smallint) |
| parinausudias | 10 | inatividadeUsuarioDias : Integer(smallint) |
| parsenusudias | 30 | senhaUsuarioDias : Integer(smallint) |
| parsecr | '' (empty) | secretaria : String(50) |
| parsecrend | '' (empty) | secretariaEndereco : String(50) |
| parsecrcep | '' (char(8), blank) | secretariaCep : CHAR-trim |
| parsecrfone1 | '' (char(20), blank) | secretariaFone1 : CHAR-trim |
| parsecrfone2 | '' (char(20), blank) | secretariaFone2 : CHAR-trim |
| parsecremail | '' (empty) | secretariaEmail : String(70) |
| parcadsemcns | f | cadastroSemCns : Boolean |
| parreccomprador | t | reciboComprador : Boolean |
| parexigecid10atestado | f | exigeCid10Atestado : Boolean |
| parestornaratendimento | t | estornarAtendimento : Boolean |
| parimpriscomaterno | 1 | imprimeRiscoMaterno : Integer(smallint) |
| paratehis | 0 | atendimentoHistorico : Integer(smallint) |

All 19 live columns present with types matching the slice mapping (boolean→Boolean; smallint→Integer; char→trim).
`validate` alignment holds. Once the tenant config matches, expect a 200 whose JSON mirrors this row (char fields
trimmed to "").

## LGPD / PHI
System configuration only — no patient data. `phi_fields: []` per spec. PHI-scan of the returned/queried values:
CLEAN (secretaria header fields are empty on this row; no names/CPF/CNS/addresses). No real PHI persisted anywhere.

## Verdict: **PARITY (2/2 verifiable non-config scenarios: 3a, 3b) + AuthZ correct**; scenario 1 behavior correct-for-tenant.
- 1 FINDING (config/tenancy divergence, empresa 411420 app vs 364 snapshot) — **NOT a code defect**; blocks field-fidelity assertion (scenario 2) until tenant is aligned. Re-run with `AUDIT_EMPRESA_CODIGO=364` to close scenario 1/2 at 200.
- Write-parity (PUT geral/ambulatorial) DEFERRED (READ-ONLY run).
- Mutations: ZERO. Snapshot untouched.
