# SAU_PESF_PROFEXT (Profissional Externo) — Golden-master parity report
- **Date:** 2026-07-02 · **Method:** DB-state (read-only) vs NON-PROD snapshot · **Auth:** JWT SAUDE_CADASTRO (admin)
- **Read surface:** GET-by-id ONLY (`GET /api/profissionais-externos/{id}`). No list endpoint exists.
  The composite **create (POST)** is a two-table write (SYS_PES + SAU_PRO) — **write-parity DEFERRED** to
  `/verify-parity` against a copy DB (per SLICE-SPEC OQ3). This run is strictly READ-ONLY: **ZERO writes**
  (no POST/PUT/DELETE issued, no DB mutation, no snapshot restore needed).
- **External flag:** confirmed column is `sau_pro.proext` (smallint). Population: 3014 SAU_PRO rows —
  1479 `proext=1`, 773 `proext=0`, 762 `proext=NULL`.
- **Type audit (entity ↔ live):** `ProExt` smallint↔Short ✅ · `ProSit` smallint↔Short ✅ · `ConClaCod`
  smallint↔Short ✅ · `ProNumCr`/`ProPesNumCns` char↔String (@JdbcTypeCode CHAR) ✅ · `ProDatIni`/`ProDatFim`
  date↔LocalDate ✅ · `PesMunCod` integer↔Integer ✅ · `PesCod`/`ProPesCod` bigint↔Long ✅. No mismatch.
- **PHI:** names/CNS/nº-conselho are PHI — records referenced by numeric ProPesCod; matched name/CNS values
  shown md5-redacted. Report PHI-scanned before finishing (no cleartext name/CPF/CNS present).

## Result: 7/7 PARITY (with 1 recorded behavioral note, not a divergence)

| # | Scenario | Input (ProPesCod) | Legacy/DB truth | New API result | Equiv? | Notes |
|---|----------|-------------------|-----------------|----------------|--------|-------|
| 1a | Get external | 106189 | proext=1, conclacod=71, cns md5=ab367…, nome md5=1bb186…, muncod=411520, numCR=25594, datIni=2020-08-03, datFim=0001-01-01, sit=1 | HTTP 200; externo=1, conselhoClasseCod=71, cns/nome match (redacted), municipioCod=411520, numeroConselho=25594, dataInicio=2020-08-03, dataFim=0001-01-01, situacao=1 | ✅ | field-by-field match |
| 1b | Get external | 106190 | proext=1, conclacod=71, nome md5=6a12e7…, numCR=43162, muncod=411520 | HTTP 200; all fields match | ✅ | |
| 1c | Get external | 106191 | proext=1, conclacod=71, nome md5=afcb50…, numCR=34080, muncod=411520 | HTTP 200; all fields match | ✅ | |
| 1d | Get external | 106192 | proext=1, conclacod=71, nome md5=32ec0f…, numCR=29434, muncod=411520 | HTTP 200; all fields match | ✅ | |
| 2 | Not found | 99999999 | no SYS_PES/SAU_PRO row | HTTP 404 RFC-7807 (`not-found`, detail "Profissional externo 99999999 não encontrado") | ✅ | clean 404 |
| 3 | Non-external via ext endpoint | 57, 247 (proext=0); 597 (proext=NULL) | SAU_PRO rows exist but are NOT external | HTTP **200** — returns the row with `externo`=0 / null | ✅† | **BEHAVIORAL NOTE** — see below |
| 5 | AuthZ no token | 106189 | — | HTTP 401 | ✅ | secured |

† Scenario 3 is a **PARITY match** in the sense that the row values returned equal the DB, but it exposes a
**design behavior worth flagging (not a legacy divergence — legacy has no equivalent GET-by-id read surface
that filters on ProExt either):** the endpoint does **NOT filter on `proext=1`**. `ProfissionalExternoService.get(id)`
only requires that BOTH a `Pessoa` (SYS_PES) and a `Profissional` (SAU_PRO) row exist for `id`; it never checks
the external flag. Consequence: **any SAU_PRO professional (internal, proext=0/NULL) is served through the
"profissionais-externos" endpoint**, returning `externo:0` (or `null`). ProPesCod 597 (proext=NULL) additionally
returns `dataInicio:null`, `dataFim:null`, `externo:null` (GeneXus never populated ProDatIni/Fim for that row —
faithful, not a bug).

## Divergences / decisions
- **D1 (behavioral, accepted-pending-decision): GET-by-id does not enforce `proext=1`.** A non-external
  professional resolves 200 on this endpoint. It is arguably an intended shared read (edit/delete of an
  external professional delegate to the `profissional/` SAU_PRO flow, per spec §Decisões), so the two
  domains overlap by design. **However**, for a "Profissional Externo" read-back it is safer to 404
  non-external ids. **Recommendation:** add `&& proext==1` guard in `get()` returning NotFound otherwise,
  OR explicitly accept the shared behavior in the SLICE-SPEC. Not a golden-master value divergence — no
  data is wrong — so it does **not block** cutover of the read path, but it should be a conscious sign-off.
- **`dataFim` sentinel `0001-01-01`:** all 1479 external rows carry the GeneXus empty-date sentinel; the API
  passes it through verbatim (`"dataFim":"0001-01-01"`). Faithful to legacy. (Future improvement could map
  the sentinel to `null` in the DTO — currently only genuinely-null dates return `null`, as row 597 shows.)
- **CNS / numeroConselho padding:** API returns the raw CHAR(20) space-padded string (e.g.
  `"<redacted-15d>     "`). Trimmed value equals the DB. Presentation-only (CHAR fixed width) — ignored
  per business-equivalence rules; a `.trim()` in the DTO mapper would be a safe cosmetic improvement.
- **No CPF/RG/endereço in the response** — correct: the lean form never collects them (spec §Sem CPF).

## Write-parity (DEFERRED — not run here)
Create is POST (SYS_PES PesTip=1 + SAU_PRO ProExt=1, atomic). Must be verified DB-row-vs-DB-row against a
**copy** DB under `/verify-parity SAU_PESF_PROFEXT` (R25 PesCod=MAX+1 — current MAX(pescod)=125650; R12
uppercase; R17 CNS-unique 409; R19 município-exists; R21 conselho-exists; R29 ProExt=1/ProSit=1; R30 no
cert). Not exercisable read-only.

## Fixtures
- `scenarios.json` (this dir) — replayable golden inputs/expectations, PHI md5-redacted.

## Verdict
**PARITY — 7/7 read scenarios equivalent.** Read path (GET-by-id, 404, authZ) is faithful to DB state and
type-correct. **One non-blocking behavioral note (D1)** for sign-off: the endpoint serves non-external
professionals; decide whether to add a `proext=1` guard or formally accept the shared read. **Write-parity
(POST composite create) DEFERRED** to a copy-DB run before cutover.
