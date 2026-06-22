# Parity Report — SAU_PRO (Profissionais)

- **Slice:** SAU_PRO / domain `profissional` (Wave-4 keystone; prescriber registry)
- **Run date:** 2026-06-22
- **New app:** `receituario-backend.jar`, profile `local`, port 8090, against the live shared
  non-prod snapshot (`host.docker.internal:5432`, db `saude-mandaguari`), `flyway.enabled=false`,
  `ddl-auto=none`, `security.cert.enc-key` set to a throwaway 32-byte test key (v1 never writes
  `certificadoSenha`; the key only satisfies the fail-closed converter bean).
- **Auth:** HS256 JWT (raw-bytes key), role `SAUDE_CADASTRO`.
- **Spec:** `/mnt/s/projetos/.planning/migration/slices/SAU_PRO.slice.md`
- **Verdict source for "expected":** high-confidence mined rules (R1–R32) in the SLICE-SPEC.

## Method / caveat — legacy HTML golden-master NOT automated

The legacy GeneXus WW screens are auth-gated and cannot be driven headlessly:
`hwwsau_pro` → 301 redirect to login; `sau_pro` → 403. A headless HTML golden-master capture of the
legacy responses is therefore **not feasible** and was **not automated** for this run. Equivalence was
verified at the **DB row + API behavior** level against the new app on a copy of the shared seed state
(self-seeded synthetic rows, restored exactly at the end), using the high-confidence mined business
rules as the oracle. This is the reliable equivalence check for a shared-schema strangler slice, but it
is **not** a byte-for-byte legacy capture — see "Cutover blockers" below; parity here is
necessary-not-sufficient.

**PHI:** all seeded person data is synthetic (`ZZ`-prefixed names, fake ids 9999001x). No real
professional/person PHI was copied into this report or any fixture. The live list/detail endpoints do
return real PHI for production rows; none of it is recorded here.

## Verdict table

| # | Scenario | Rule | Expected | Got | Verdict |
|---|----------|------|----------|-----|---------|
| 1 | GET list default (paginated) | — | 200 paginated | 200, `content[]` + page metadata | PARITY |
| 2 | Create A (id 99990010, person pre-seeded) | R1/R12/R13/R15 | 201; SAU_PRO row exists; soundex non-empty; situacao=1; proext=0; expEsus=false | 201; row present; `propesnomsoundex="S PARITI PO A"`; prosit=1; proext=0; proexpesus=f | PARITY |
| 3 | GET /99990010 detail | view + R31 | 200; nome from SYS_PES; NO certificadoSenha/certificado/assinaturaImagem | 200; `nome="ZZ PARITY PRO A"`; all three sensitive fields **absent** | PARITY |
| 4a | Search by nome substring | R16 | row present (literal `PesNom LIKE %?%`) | `nome=PARITY` and `nome=ZZ PARITY PRO A` → A returned | PARITY |
| 4b | Search by numeroCns exact | R16 | row present | `numeroCns=799999999990008` → A returned | PARITY |
| 4c | situacao + dataInicioFrom + dataFimAte (both bounds) | R16 / 42P18 regression | 200, no SQL error | 200, both date bounds applied, **no 42P18** | PARITY |
| 5 | GET /99990099 (unseeded) | — | 404 | 404 `not-found` | PARITY |
| 6 | Create id=99990099, person NOT seeded | R1 | 422 reject | 422 `pro.pessoa.notfound` ("Não existe Profissional") | PARITY |
| 7a | Create missing numeroCns | R3 (bean `@NotNull`/`@CNS`) | 400 | 400 `validation`, `errors.numeroCns="CNS inválido"` | PARITY* |
| 7b | Create invalid CNS `700000000000001` | R4 (`@CNS`) | 400 | 400 `validation`, `errors.numeroCns="CNS inválido"` | PARITY* |
| 8 | Create B same CNS as A | R5 | 422 reject | 422 `pro.cns.duplicate` ("...utilizado pelo cadastro 99990010!") | PARITY |
| 9 | Create B conclacod=9999 (unknown, ≠0) | R10 | 422 reject | 422 `pro.conselho.notfound` ("Não existe Conselho de Classe.") | PARITY |
| 10 | PUT /99990010 pessoa.nome → A-EDIT | R2 | 200; SYS_PES.pesnom changed | 200; `sys_pes.pesnom = "ZZ PARITY PRO A-EDIT"` | PARITY |
| 11 | DELETE /99990010 with SAU_PROESP | R20 | blocked (409/422) | 422 `pro.delete.referenced` ("possui especialidade(s)...") | PARITY |
| 12 | DELETE /99990010 with SAU_RECESP | R26 | blocked (409/422) | 422 `pro.delete.referenced` ("...controle especial (Portaria 344/98)") | PARITY |
| 13 | DELETE /99990010 (now unreferenced) | R19 | 204; row gone | 204; SAU_PRO row count = 0 | PARITY |

`*` = status-nuance, see RF6 below.

**Run verdict: PARITY** across all 13 scenarios.

## Security assertion (R31 — certificadoSenha / certificate / signature)

PASS. The detail payload `GET /api/profissionais/99990010` contains **none** of
`certificadoSenha`, `certificado`, or `assinaturaImagem` (explicit absence asserted). The list
payload likewise omits them. v1 API neither accepts nor returns these fields; the legacy
cleartext-echo/log defect (R31) is not ported. Note: this run did not exercise a populated
certificate row (the synthetic seed has null cert/senha), so the assertion proves the fields are
**not serialized**, not that an existing encrypted value round-trips — the at-rest scheme remains an
OQ1/DPO sign-off item.

## Status nuance (RF6) — 400 vs 422 vs legacy message

Scenarios 7a/7b are accepted/rejected identically to legacy (rejected), but the **HTTP status and
shape differ from the legacy presentation**:
- New app returns **400 `validation`** (bean-level `@NotNull`/`@CNS`) with a field-error map
  (`errors.numeroCns`).
- Legacy GeneXus surfaces these as in-page **messages** ("Informe o Número do CNS!" / CNS check-digit
  failure via `psau_val_cns`), not an HTTP-status contract.
- Business-equivalence (accept vs reject) holds; only the transport/status representation differs.
  This is the same RF6 status-nuance class accepted in prior slices — flagged, not a divergence.
- Note the two-tier split in the new app: structural/format failures (missing or malformed CNS) →
  **400**; business-rule failures (duplicate CNS R5, unknown conselho R10, person-not-found R1,
  delete-guards R20/R26) → **422**. Legacy expresses all of these as messages.

## NOT covered by this run

- **R22 / R23 / R24 delete-guards** ("Uni Nut Pro Pes", SISPRENATAL, HIPERDIA): these are
  **non-enforcing stubs** in the new app (`to_regclass`/`query_to_xml`; the physical tables are absent
  in the live snapshot). They were **not seeded/exercised** here. A prescriber referenced only by one
  of these would currently be deletable. Resolve OQ-DG (confirm real physical table names) and replace
  with EXISTS guards + seeded ITs before cutover.
- **R21 (SAU_USU), R25 (SAU_UNI 4 roles)** delete-guards: present in code but not exercised in this run
  (only R20 and R26 were seeded). Recommend adding seeded fixtures for these to the parity IT.
- **R28 per-mode authorization + superuser (Ususysmar) bypass:** the new app uses coarse
  `hasRole('SAUDE_CADASTRO')` like the reference slices; per-mode bits (INS/UPD/DLT/DSP) and superuser
  bypass were not differentiated here (OQ6).
- **Certificate/signature populated round-trip & blob route ACL** (R29/R30, OQ2): not exercised (cert
  out of v1 API).
- **CNS mod-11 algorithm regulatory sign-off** (R4 / OQ-CNS): the validator matches the spec's mod-11
  description and rejected the invalid sample, but the exact algorithm vs `psau_val_cns.java` /
  regulatory confirmation is still open.

## Fixtures

No JSON capture fixtures were committed for this run: legacy HTML golden-master was not automated
(auth-gated), and all assertions ran live against the snapshot with synthetic, self-restored seed
state. Recommended follow-up: have `test-author` add a Testcontainers parity IT that seeds the
synthetic rows (SYS_PES 9999001x, SAU_PRO 99990010, SAU_PROESP, SAU_RECESP, SAU_USU, SAU_UNI roles)
and replays scenarios 2–13 plus the R21/R22/R23/R24/R25 guards, so CI does not need the legacy app
live.

## Cutover blockers (parity is necessary, NOT sufficient)

This run does **not** mark the slice verified. Remaining sign-off blockers (from the SLICE-SPEC cutover
checklist):

1. **DPO/security sign-off (OQ1/OQ2):** certificadoSenha at-rest scheme + bulk-PHI-read audit;
   certificate/signature blob ACL + dedicated audited route.
2. **Regulatory sign-off (OQ-CNS, R26):** exact mod-11 CNS algorithm; SAU_RECESP retention semantics
   (soft-deactivate, never hard-delete a prescriber with controlled-substance history).
3. **OQ-DG / delete-guards R22/R23/R24:** confirm physical table names and replace non-enforcing stubs
   with real EXISTS guards + seeded ITs (otherwise a referenced prescriber is deletable).
4. **R28 per-mode auth + superuser bypass (OQ6):** replace coarse role with per-mode permission mapping.
5. **Flyway baseline:** confirm no env applied the partial 4-column V1 SAU_PRO (checksum); repair if so.
6. **Legacy golden-master:** automate (or manually capture under an authenticated session) a true
   legacy HTML/result comparison for at least the read paths, to complement this rule-based check.

## Seed / restore ledger

- Baseline before run: `sys_pes = 83105`, `sau_pro = 3014`.
- Seeds created and removed: SYS_PES 99990010/99990011; SAU_PRO 99990010 (created S2, deleted S13);
  SAU_PROESP(99990010,1) (S11, removed); SAU_RECESP(1,99999001,99990010) (S12, removed).
- Baseline after cleanup: `sys_pes = 83105`, `sau_pro = 3014`; leftovers in
  SAU_PRO/SYS_PES/SAU_PROESP/SAU_RECESP for the seed ids = 0. **Restored exactly.**
- App stopped; temp scripts/logs/token removed.
