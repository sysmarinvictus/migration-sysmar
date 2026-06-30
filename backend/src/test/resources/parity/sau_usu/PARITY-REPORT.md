# Parity Report — SAU_USU (Usuário do Sistema · authentication keystone)

- **Date:** 2026-06-30
- **Verdict:** **BEHAVIORAL PARITY** (6/8 scenarios, IT/unit-backed) **+ live credential golden-master DEFERRED-TO-CUTOVER** (2/8, bridge-gated). 2 scenarios deferred by design (fine RBAC). **Status remains `tested`** — NOT `verified` — because the credential-equivalence scenarios cannot honestly be confirmed against this snapshot pre-bridge.
- **Target:** new app on `:8090` (real SAU_USU auth profile, flyway off, ddl-auto none) against the shared non-prod `saude-mandaguari` snapshot on the Windows host (`host.docker.internal`). Legacy GeneXus app live on `:8080`.

## Why this slice cannot be live golden-master'd pre-cutover

Unlike every read/CRUD slice, SAU_USU has **two hard blockers** to a live golden-master run, both confirmed against the live snapshot on 2026-06-30:

1. **The legacy login is a stateful GeneXus AJAX form.** `hindex` posts `vUSULOGIN`/`vUSUSENHA`/`vUNIDADE` plus a server-integrity-hashed `GXState` + `_EventName`. A headless POST trips GX's state check rather than exercising the credential path, so a clean "bad-credentials" golden is not capturable without a JS-driving browser. (This matches the precedent for the auth-gated WWs in SAU_IMP/SAU_PRO reports — HTML golden not automated.)

2. **Credential equivalence requires the password-bridge, which is a cutover-only operation.** The legacy verifies `decrypt64(ususen, usukey) == senha` (reversible); the modern app verifies bcrypt and only for **migrated** users (`usukey IS NULL`). Live snapshot reality:

   | metric | value |
   |---|---|
   | SAU_USU total | 1230 |
   | migrated (`usukey` NULL → loginnable in new app) | **1** |
   | not migrated (`usukey` present → "redefina sua senha") | **1229** |
   | SYSMAR superusers | **12** |

   So the new `/auth/login` correctly rejects 1229/1230 users by design, and **no JWT can be obtained** against this snapshot via the real SAU_USU path — there is nothing to diff. Running the bridge `--commit` to enable it is forbidden pre-cutover (it breaks legacy login against the shared snapshot and needs a backup + coordination, OQ3). We did **not** mutate the shared security table or run the bridge.

Equivalence is therefore established as **behavioral parity** against the high-confidence, line-cited mined rules and the 37-test unit/IT suite (all green; full suite 268/0F/0E).

## Scenario results

| # | Scenario | Rule | Legacy cite | Verdict | Evidence |
|---|----------|------|-------------|---------|----------|
| 2 | invalid login / wrong password → same generic message (no enumeration) | R3 | hindex_impl.java:866-918 | **BEHAVIORAL-PARITY** | AuthControllerTest (unknown-user & wrong-password both → "Usuário ou senha inválidos") |
| 3 | blocked user (UsuBloq=1) rejected AFTER correct password | R5 | hindex_impl.java:891-894 | **BEHAVIORAL-PARITY** | AuthControllerTest.login_rejectsBlockedUserEvenWithCorrectPassword (was the review BLOCK — fixed) |
| 4 | change password: current+confirmation; rotate usukey; stamp dataRedefinicao | R10/R11 | hmudasenhalogin/psau_usu_mudasenha | **BEHAVIORAL-PARITY** | UsuarioServiceTest.changePassword_* (3) |
| 6 | SYSMAR → bypass | R2 | pisauthorized.java:65-68 | **BEHAVIORAL-PARITY** | SauUsuUserDetailsServiceTest.superuserFlag_* (12 SYSMAR live → OQ2 audit) |
| 7 | create: login unique, nome+login required, usupesquisa* default-denied | R12/R13/R15 | sau_usu_impl.java:3247-3274,10878 | **BEHAVIORAL-PARITY** | UsuarioServiceTest.create_* (4) |
| 8 | delete blocked when referenced by SAU_USUCON/USUUNI | R17 | sau_usu_impl.java:5353-5369 | **BEHAVIORAL-PARITY** | UsuarioServiceTest.delete_* (3) |
| 1 | valid login (correct password) → issues token | R1 | hindex_impl.java:888-889 | **DEFERRED-TO-CUTOVER** | bridge-gated (1/1230 migrated); behavior covered by SauUsuUserDetailsServiceTest.migratedUser_* + AuthControllerTest.login_issuesTokenForUsableAccount |
| 5 | authorization: profile precedence; Inc/Alt/Exc/Con per mode | R8/R9 | pisauthorized/acessamodulo | **DEFERRED** | fine per-program RBAC deferred to a later authorization slice (OQ7); v1 is coarse + documented |

## Cutover blockers cleared this run (live introspection)

- **OQ12 ✅** `sau_prf` has exactly `(prfcod integer, prfnom varchar)` → `findPerfilNome` query is correct.
- **OQ13 ✅** 0 of 10 profiles contain "ADMIN" (ENFERMEIRO, ACS, ATENDENTE, …) → the `adminProfileMatch="ADMIN"` heuristic is inert on real data; `/api/usuarios` CRUD is **SYSMAR-only** in v1 (safe; no accidental elevation). Fine per-program admin is the deferred authorization slice.

## Still required before the auth cutover (not parity blockers, but cutover gates)

1. Run the **behavioral parity live** post-bridge: at cutover, after `password-bridge --commit` on a migrated snapshot (with SAU_USU backup + legacy coordination), re-run scenarios 1 & 3 against `/auth/login` to capture the credential golden. **Then** the slice can move to `verified`.
2. **OQ2** — audit the **12 SYSMAR** accounts; decide keep/gate/remove.
3. **OQ11** (login-audit timing) and **OQ14** (`nextUsuCod` scan/race) — correctness FLAGs from the review.
