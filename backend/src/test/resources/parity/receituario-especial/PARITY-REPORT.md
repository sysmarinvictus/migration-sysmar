# SAU_RECESP (Receituário Controle Especial) — Golden-master parity report

- **Date:** 2026-07-01 · **Method:** DB-state vs shared snapshot (1510 rows) · **Auth:** JWT SAUDE_CADASTRO · **READ-ONLY**
- **New app:** http://localhost:8090 (live NON-PROD snapshot, flyway off, ddl-auto=none) · **Golden master:** snapshot DB `saude-mandaguari` (`sau_recesp` 1510 · `sau_recesp1` 2298).
- **Type audit:** entity ↔ live types MATCH — `recespunicod` integer↔Integer; `recespcod`/`funpescod`/`pacpescod`/`recesppropescod` bigint↔Long; `recespseqult`/`recespcon`/`rectip`/`recespseq`/`recespqtdtip`/`recesptip`/`recesptipuso`/`recespusocon` smallint↔@JdbcTypeCode(SMALLINT) Integer; `recespusulogin` character↔@JdbcTypeCode(CHAR)+trim; `recespqtd` numeric(5,1)↔BigDecimal; `recind` boolean↔Boolean. No divergence.

## Result: 9/9 PARITY

| # | Scenario | Inputs | Legacy (snapshot SQL) | New app | Equivalent? | Notes |
|---|----------|--------|-----------------------|---------|-------------|-------|
| 1 | List total | `?size=1` | `count(*) sau_recesp` = 1510 | totalElements = 1510 | ✅ | |
| 2 | List row fidelity | first page `?size=5` | rows unit 2 / numero 41,40,39,38,37; PesNom present | identical unidade/numero/data/paciente/prescritor; pacienteNome populated | ✅ | `pacienteNome` for 2/41 == DB `PesNom` (md5 5156fe3f…, exact) |
| 3 | Filter by unidade | `?unidade=30` | `count WHERE recespunicod=30` = 1423 | totalElements = 1423 | ✅ | |
| 4 | Filter by paciente | `?paciente=1649` | `count WHERE pacpescod=1649` = 17 | totalElements = 17 | ✅ | |
| 5 | Filter by nome | `?nome=silva` / `?nome=santos` | ILIKE %silva% = 208 · %santos% = 163 | 208 · 163 | ✅ | join SAU_PAC→SYS_PES, PesNom ILIKE |
| 6 | Detail field-by-field | keys 37/15 (13 itens), 2/4 (1), 30/77 (6) | master + `sau_recesp1` rows | every master field + all `itens[]` fields match in seq order | ✅ | see field-by-field below |
| 7 | Derivations | same 3 keys | age(PesNasDat,RecEspDat)=57/46/55; CNS present→no aviso; no social name | pacienteIdade 57/46/55; avisos []; exibicao==PesNom | ✅ | R15/R16(neg)/R13 verified; R13/R16 positive branches not present in snapshot (see coverage note) |
| 8 | Not found | `GET /999999/999999` | n/a | HTTP 404 | ✅ | |
| 9 | Ordering | first page | `ORDER BY recespunicod ASC, recespcod DESC` → 2/41,40,39,38,37 | identical order | ✅ | |

### Scenario 6 — detail field-by-field (master)
| Key | data | pacienteCodigo | prescritorCodigo | funcionarioCodigo | situacao | tipoReceituario | observacao | itens |
|-----|------|----------------|------------------|-------------------|----------|-----------------|-----------|-------|
| 37/15 | 2021-05-19 ✅ | 24989 ✅ | 86677 ✅ | 106610 ✅ | 0 ✅ | 0 ✅ | (empty) ✅ | 13 ✅ |
| 2/4   | 2021-04-06 ✅ | 84697 ✅ | 106039 ✅ | 106039 ✅ | 0 ✅ | 0 ✅ | (empty) ✅ | 1 ✅ |
| 30/77 | 2024-08-13 ✅ | 102883 ✅ | 117425 ✅ | 117425 ✅ | 0 ✅ | 0 ✅ | (empty) ✅ | 6 ✅ |

### Scenario 6 — itens[] (SAU_RECESP1) field-by-field
For each key the app `itens[]` array was compared against `sau_recesp1` rows in `recespseq` order on all
11 fields (sequencia, medicamentoCodigo, prescricao, quantidade, quantidadeTipo, posologiaCodigo,
observacao, tipoReceita, tipoUso, usoContinuo, indeferido). PHI text fields (`prescricao`, `observacao`)
were compared by md5 to avoid exposing content. **All 20 line rows across the 3 keys were identical.**
The only textual delta was numeric rendering (`quantidade` `360.0` vs psql `360`) — same numeric value,
formatting only, not a divergence.

## Derivations verified (Scenario 7)
- **R15 patient age** — `age(RecEspDat, PesNasDat)` computed on the DB = 57 / 46 / 55, equals app
  `pacienteIdade` exactly for all 3 keys.
- **R16 display name (negative branch)** — no snapshot patient uses a social name, so app
  `pacienteNomeExibicao == pacienteNome == SYS_PES.PesNom` (md5 6a0722e3… for 37/15, exact).
- **R13 CNS warning** — all 3 sampled patients have a CNS on file → app `avisos == []` (warning correctly
  absent). Snapshot-wide: **0 of 1510** prescriptions reference a blank-CNS patient.

## Coverage limits (not divergences)
- **R13 CNS-missing warning (positive)** and **R16 social-name display (positive)** cannot be exercised by
  golden-master data: the snapshot has **0** prescriptions whose patient has a blank/null CNS and **0**
  whose patient uses a social name. Those branches remain covered by the slice's unit + Testcontainers IT
  suite (29 unit + 13 IT), not by parity.
- **Write-parity (create server-allocated numero R1, copy R31, delete) is DEFERRED** pending CRF/regulatory
  sign-off (CLAUDE.md Rule 5; SLICE-SPEC regulatory OQ1/OQ2). No POST/PUT/DELETE was issued and no row was
  created/updated/deleted. Legacy R29 hard-delete is intentionally NOT ported (modern blocks with 409); that
  behavioural change is a recorded intended improvement, not a parity divergence, and is out of scope for
  this READ-ONLY gate.

## Safety / LGPD (Rule 4)
- READ-ONLY: only `GET` endpoints + the `/auth/login` POST (authentication, not a data write) were called.
- Post-run row counts unchanged: `sau_recesp` 1510, `sau_recesp1` 2298; `ZZPARITY` marker rows: 0.
- No real patient name, CNS, CPF or other identifying value is recorded here. Patients/prescribers are
  referenced by numeric code only; matched PHI values were compared via md5 digests, never printed.

## Verdict: **PARITY (9/9)** for the read surface. No business divergences.
Write-scenario parity (create/copy/delete) DEFERRED pending CRF regulatory sign-off — this report does NOT
clear the slice for cutover on its own (Rule 5).
