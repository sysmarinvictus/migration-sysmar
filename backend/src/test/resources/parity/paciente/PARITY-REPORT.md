# SAU_PAC (Paciente) — Golden-master parity report

- **Date:** 2026-07-02
- **Method:** DB-state vs shared snapshot · **Auth:** JWT SAUDE_CADASTRO · **READ-ONLY**
- **Golden master:** snapshot DB `saude-mandaguari` (`sau_pac` 80325 rows + `sys_pes` 83105 rows; person supertype).
- **New app:** http://localhost:8090 (NON-PROD snapshot; flyway off, ddl-auto=none).
- **LGPD (Rule 4):** snapshot holds REAL patient PHI. Patients referenced by numeric code only; matched
  values verified by comparison and **not** transcribed here (names/CPF/CNS/addresses withheld / md5-only).
- **Write-parity DEFERRED:** create (composite SYS_PES+SAU_PAC), update (write-back), and DELETE incl.
  the R14 Portaria-344/98 delete-guard (SAU_RECESP) are **NOT** exercised in this pass — a separate
  write-parity pass against a throwaway copy is required before cutover.

## Result: 12/12 PARITY (D1 found by this parity run, then FIXED + re-verified 2026-07-02)

| # | Scenario | Inputs | Legacy (snapshot SQL) | New API | Equivalent? | Notes |
|---|----------|--------|-----------------------|---------|-------------|-------|
| 1a | Search total by nome | `nome=SILVA` | count=13294 | totalElements=13294 | ✅ | ILIKE `%..%` on PesNom |
| 1b | Search total by nome | `nome=MARIA` | 6110 | 6110 | ✅ | |
| 1c | Search total by nome | `nome=JOSE` | 3254 | 3254 | ✅ | |
| 2a | Search by CPF (raw digits of a formatted-CPF patient) | count=1 | totalElements=1 (id matched) | ✅ | **D1 FIXED** — was ❌ 0; see D1 |
| 2b | Search by CNS | `cns=000000000000000` (pac 523) | count=1, id=523 | 1, id=523 | ✅ | CNS stored unformatted |
| 2c | Search by prontuário | `prontuario=335` (pac 26644) | count=1, id=26644 | 1, id=26644 | ✅ | prefix LIKE on PacProNum |
| 2d | Search by nomeMae | `nomeMae=CONCEICAO` | count=1465 | 1465 | ✅ | ILIKE `%..%` on PesNomMae |
| 3 | Detail field-by-field | pacs 523,409,124267,243,26644,470,19463 | 30 fields/pac from SAU_PAC⋈SYS_PES | identical | ✅ | see field audit below |
| 4a | Lookup | `q=IRENE` | 149 matches, first-10 by PesNom | same 10 ids, same order | ✅ | ORDER BY PesNom, cap 10 |
| 4b | Lookup | `q=ZULMIRA` | 18 matches, first-10 by PesNom | same 10 ids, same order | ✅ | name/CNS OR-match |
| 5 | Not found | `GET /api/pacientes/999999999` | (no row) | HTTP 404 | ✅ | |
| 6 | Type fidelity | entity ↔ live | — | — | ✅ | see type audit |

## Scenario 3 — detail field audit (per-patient, 30 fields each)

Patients chosen for varied data (codes only):
- **523** — allergy + histórico de doenças + rendaFamiliar=3
- **409** — obito=1; clinical booleans NULL in DB (returned as `null` by API — matches)
- **124267** — situacaoRua=true
- **243** — rendaFamiliar>0, beneficioSocial=false
- **26644** — prontuário present (`335`)
- **470** — meioTransporte present
- **19463** — beneficioSocial=true

Compared fields (all matched, null-preserving): person (SYS_PES) `nome, nomeMae, nomePai, cpfCnpj, cns,
rg, dataNascimento, sexo, cep, endereco, numero, bairroCod, municipioCod, celular, email, usaNomeSocial`
and patient (SAU_PAC) `unidadeCod, prontuario, numeroIdentificacao, alergia, historicoDoencas, obito,
inconsciente, situacaoRua, surtoPsiquiatrico, rendaFamiliar, meioTransporte, beneficioSocial, cnh, situacao`.

**Result: 7/7 patients — ALL fields byte-identical to golden master.** Notes:
- NULL fidelity: where SAU_PAC booleans / rendaFamiliar are DB-NULL (e.g. pac 409), the API returns
  JSON `null`, not a defaulted `false`/`0` — exact match.
- Whitespace fidelity: `historicoDoencas` for pac 523 is stored `"hipertensa "` (trailing space, varchar
  len 11); the API returns it verbatim — exact match (not a divergence; real stored data, not padding).
- char(10) `PacProNum` and char CNS are trimmed by the API (service `trim()`), matching the intended
  presentation of the golden-master value.

## Type-fidelity audit (entity ↔ live) — MATCH

| Field | Live type | Entity mapping | Verdict |
|-------|-----------|----------------|---------|
| PacPesCod (id) | bigint | Long PK | ✅ |
| PacProNum (prontuario) | character(10) | String len=10, service `trim()` | ✅ trimmed |
| PacSit (situacao) | smallint | Integer | ✅ |
| PacObi (obito) | smallint | Integer | ✅ |
| PacRendaFamiliar | smallint | Integer | ✅ |
| PacInconsciente / PacSituacaoRua / PacSurtoPsiquiatrico / PacBeneficioSocial | boolean | Boolean (nullable) | ✅ null-preserving |
| PacAler(50)/PacCHistDoe(200)/PacCNH(11)/PacMeioTransporte(50) | varchar | String | ✅ |
| PacIdNum (numeroIdentificacao) | bigint | Long | ✅ |
| PesNumCns (cns) | character(20) | String, service `trim()` | ✅ trimmed |
| PesCPFCNPJ (cpfCnpj) | character(18) | String, service `trim()` | ✅ trimmed (but see D1 for the *search* path) |
| PesNasDat | date | LocalDate | ✅ |
| PesSex/PesCep/etc. | char | String trimmed | ✅ |

No entity↔live type mismatch found on the read path.

## Divergences

### D1 — CPF search filter never matches (BLOCKER)
- **Where:** `PacienteService.search(...)` calls `digits(cpf)` (strips to raw digits, e.g.
  `00000000000`), but `PacienteRepository.search` runs `pes.PesCPFCNPJ LIKE concat(:cpf, '%')`.
- **Root cause:** in the live/snapshot DB, `sys_pes.pescpfcnpj` is stored **formatted** (48073 rows
  contain `.`/`-`, 0 rows are 11 raw digits). A `LIKE '00000000000%'` against a value stored as
  `000.000.000-00` matches nothing. Confirmed at DB level: `LIKE '00000000000%'` → 0 rows;
  `LIKE '000.000.000-00%'` → 1 row.
- **Impact:** the entire CPF search filter (`?cpf=`) returns an empty result set for every patient,
  regardless of formatting the caller supplies (tested raw, formatted, and partial — all 0). This is a
  functional regression vs the legacy patient search, which locates patients by CPF.
- **Note:** CNS search works because CNS is stored unformatted; the digit-stripping mismatch is specific
  to the formatted-CPF column. Data is heterogeneous — a minority of `pescpfcnpj` rows ARE unformatted
  (seen in lookup output), so no single normalization on input alone is sufficient; the fix must
  normalize on **both** sides (e.g. compare `regexp_replace(PesCPFCNPJ,'[^0-9]','','g')` to `:cpf`), or
  stop stripping the input and match the stored format.
- **FIX (2026-07-02):** `PacienteRepository.search` now matches
  `regexp_replace(pes.PesCPFCNPJ,'[^0-9]','','g') LIKE concat(:cpf,'%')` in both the main and count query
  (normalizes the stored column to digits; the service already passes raw digits — both sides normalized,
  handling the heterogeneous formatted/raw data). Locked by IT `PacienteControllerIT#searchByCpfNormalizes
  StoredFormatting` (seeds a formatted CPF, searches by raw digits, expects the patient). **Re-verified
  against the live snapshot:** `?cpf=<11 raw digits>` of a real formatted-CPF patient → `totalElements=1`,
  correct id. **RESOLVED.**
- **Verdict:** RESOLVED — CPF search now at parity.

## Verdict: 12/12 PARITY (read surface; D1 caught + fixed by this run)

Read-path list/detail/lookup/404, CPF search (after the D1 fix), and type fidelity are at exact byte-parity
with the golden master. Write-parity (create/update/delete + R14 Portaria-344 guard) is DEFERRED to a
separate pass. This slice remains `tested` (write-parity pending) — read surface verified.

## Safety attestation
- **ZERO write requests issued.** Only `GET` (API) and `SELECT` (DB) were run.
- **ZERO rows created/modified/deleted.** Post-run counts: `sau_pac`=80325, `sys_pes`=83105 (unchanged
  from pre-run baseline); `sau_pac` rows with prontuário `ZZPARITY%` = 0.
- No real PHI persisted into this report or fixtures (numeric codes + counts + md5 only).
