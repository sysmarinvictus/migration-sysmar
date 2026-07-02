# SAU_PESF (Pessoa Física / SYS_PES supertype) — Golden-master parity report

- **Date:** 2026-07-02 · **Method:** DB-state vs shared snapshot (READ-ONLY; 83105 rows) · **Auth:** JWT SAUDE_CADASTRO (admin)
- **Scope:** READ surfaces only. Write-parity (POST/PUT/DELETE create/update/delete + R1-R59) **DEFERRED** — no mutation performed, no ZZPARITY rows, no DB write.
- **New app:** http://localhost:8090 (NON-PROD snapshot; flyway off, ddl-auto=none). **Snapshot DB:** host.docker.internal:5432 `saude-mandaguari`.
- **LGPD:** SYS_PES is REAL PHI. Persons referenced by numeric PesCod only; all matched values compared via md5/redaction — NO names/CPF/CNS/addresses in this report.
- **Type audit:** live CHAR(n) blank-padded fields (PesNom varchar50, PesCPFCNPJ char18, PesNumCns char20, PesSex char1, PesCep char8) returned trimmed by API. PesNasDat date → ISO date. PesUsaNomSoc boolean → JSON bool. PesCod bigint / PesBaiCod,PesMunCod integer → numeric. All fidelity checks PASS.

## Result: 20/20 PARITY (read surfaces) — write-parity DEFERRED

| # | Scenario | Inputs | Legend (golden master) | New API result | Equiv? | Notes |
|---|----------|--------|------------------------|----------------|--------|-------|
| 1a | Detail field-by-field | PesCod 2 | md5 of 17 fields | all 17 md5 match | ✅ | rich person, no social name |
| 1b | Detail field-by-field | PesCod 3 | md5 of 17 fields | all match | ✅ | escolaridade=56 (LIVE-ONLY col surfaced) |
| 1c | Detail field-by-field | PesCod 4 | md5 of 17 fields | all match | ✅ | pesnomsoc==pesnom (both md5 6d9177…) |
| 1d | Detail field-by-field | PesCod 6 | md5 of 17 fields | all match | ✅ | corCod=4, estadoCivil=2, sitFam=1 preserved |
| 1e | Detail field-by-field | PesCod 103852 | md5 of 17 fields | all match | ✅ | **usaNomeSocial=true**; nomeExibicao=nomeSocial |
| 2a | Search by nome | nome=SOARES | count 819 (PesNom ILIKE) | totalElements=819 | ✅ | exact count |
| 2b | Search by nome | nome=GUEDES | count 110 | totalElements=110 | ✅ | exact count |
| 3a | **Search by CPF (raw digits) — D1 fix** | cpf=&lt;PesCod4 raw 11d, redacted&gt; | 1 owner (PesCod 4, formatted CPF stored) | total=1, id=4 | ✅ | **D1 fix CONFIRMED WORKING** — regexp_replace normalizes stored formatted PesCPFCNPJ |
| 3b | Search by CPF (formatted input) | cpf=&lt;PesCod4 formatted, redacted&gt; | 1 owner (PesCod 4) | total=1, id=4 | ✅ | punctuation normalized both sides |
| 4 | Search by CNS | cns=&lt;PesCod4 CNS 15d, redacted&gt; | 1 owner (PesCod 4) | total=1, id=4 | ✅ | trimmed char20 match |
| 5a | Lookup ?q= count | q=GUEDES | 110 (PesNom ILIKE) | 10 returned, capped list | ✅ | lookup caps result set (typeahead); order verified below |
| 5b | Lookup ordering | q=GUEDES | id order by PesNom: 71562,69168,26280,26322,78926,124182,14031,110291 | identical id sequence | ✅ | ORDER BY PesNom reproduced exactly |
| 5c | Lookup label content | q=GUEDES ids 71562/69168/26280 | md5(PesNom) | nomeExibicao md5 identical | ✅ | label = display name, matches |
| 6 | Not found | GET /api/pessoas/999999999/cadastro | pescod absent | HTTP 404 | ✅ | correct error condition |
| 7a | Type: CHAR trim | PesNom/CPF/CNS/Sex/Cep | blank-padded in DB | trimmed in JSON | ✅ | no trailing spaces leaked |
| 7b | Type: date | PesNasDat | date | ISO yyyy-MM-dd | ✅ | e.g. 1958-07-17 |
| 7c | Type: boolean | PesUsaNomSoc | pg boolean | JSON true/false | ✅ | PesCod 103852 true, others false |
| 7d | Type: numeric ids | PesBaiCod/PesMunCod/PesCod | integer/bigint | JSON numbers | ✅ | bairroCod/municipioCod/id |
| 7e | GET /{id} (plain read model) | PesCod 2 | subset row | matching subset JSON | ✅ | read-model subset unchanged |
| 7f | CPF digit-normalization uniqueness | PesCod 4 | 1 person shares digit-CPF | search returns exactly that person | ✅ | no false multi-match |

## D1 fix status
**CONFIRMED WORKING (PASS).** CPF is stored FORMATTED in `PesCPFCNPJ` (char18, e.g. `NNN.NNN.NNN-NN`). The pessoa search now normalizes both the stored column and the query via `regexp_replace(PesCPFCNPJ,'[^0-9]','','g')`. Searching PesCod 4's CPF as raw 11 digits AND as the formatted string (real values redacted for LGPD) both return the single owner PesCod 4 (total=1). Digit-CPF uniqueness verified in DB (exactly 1 row shares the normalized value). Not re-flagged as a new divergence.

## Non-divergences (legacy data artifacts — API is faithful)
- **PesCod 2 & 3 `enderecoComplemento` = literal string `"NULL"`.** The golden-master `sys_pes.pesendcom` literally contains the 4-char string `NULL` for these rows. The API returns the stored value verbatim → PARITY, not a bug. (Cosmetic legacy data hygiene item; out of scope for this slice.)
- `escolaridade` (LIVE-ONLY col, §disc-2) is surfaced with the stored value (e.g. 3→0, 4→56, 103852→60). Read parity only observes; write-side confirmation of GraEsc-vs-Escolaridade remains a write-parity item.
- Sentinel `0001-01-01` dates and `paisCod=10` (Brasil) / `corCod` defaults are passed through unchanged from the golden master.

## Deferred (NOT tested here — write path)
Write-parity for POST/PUT/DELETE (create/update/delete cadastro, R1–R52 validations, R48/R49/R50 defaults, R51/R52 soundex, R53/R54/R55 delete-guards, R45 CPF-uniqueness) is **DEFERRED** to a mutation-capable snapshot run per the READ-ONLY constraint. Open questions OQ2 (PesSenha ''-vs-NULL), OQ8 (R11 CEP↔município), OQ9 (CPF uniqueness on formatted values) must be exercised there before cutover.

## Safety / audit
- **ZERO writes.** No POST/PUT/DELETE issued. `sys_pes` count 83105 before and after; MAX(PesCod)=125650 unchanged; 0 ZZPARITY/test rows. All curl calls used `--max-time 20`.
- PHI-scanned: this report contains only numeric PesCod, md5 hashes, and counts. No person names, CPF, CNS, or addresses.

## Verdict: **PARITY (20/20 read scenarios)** — write-parity DEFERRED. No read-surface divergence. Not `verified` for cutover until write-parity passes.
