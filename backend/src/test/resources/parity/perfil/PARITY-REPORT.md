# SAU_PRF (Perfil / access profile) — Golden-master parity report
- **Date:** 2026-07-02 · **Method:** DB-state vs shared snapshot (10 rows) · **READ-ONLY** (no POST/PUT/DELETE, no DB mutation)
- **Auth:** JWT SAUDE_ADMIN (list/get admin-gated); SAUDE_CADASTRO-only + no-token used for AuthZ negatives.
- **Type audit:** entity ↔ live types MATCH (PrfCod integer↔Integer PK; PrfNom varchar(50)↔String). ZERO FKs.
- **Scope note:** write-parity (create R1/R3, blank-nome reject R2, delete-guards R4/R5, cascade R6) **DEFERRED** — mutating scenarios excluded per READ-ONLY mandate; covered by 12 unit + 13 IT (spec `status: tested`). This run verifies read/list/lookup/authZ + type & count fidelity only.
- **PHI:** perfil names are RBAC catalog/config (not patient PHI). No redaction needed.

## Result: 11/11 PARITY (read-only subset)

| # | Scenario | Inputs | Legacy / golden-master (DB) | New result | Equivalent? | Notes |
|---|----------|--------|-----------------------------|------------|-------------|-------|
| 1 | List total | `GET /api/perfis?size=100` | `SELECT count(*)` = 10 | totalElements=10, 10 rows | ✅ | all 10 real profiles present |
| 1b| List field fidelity | same | rows 1..10 {prfcod,prfnom} | {id,nome} exact per row | ✅ | values byte-match incl. accents (CIRURGIÃO, SAÚDE) |
| 1c| List ordering | same | — | nome ASC (ACS…RESTRITO) | ✅ | synthesized list sorts by nome; deterministic |
| 2 | Get by id (×3) | `GET /api/perfis/{1,6,10}` | rows 1/6/10 | {1,HSAU_PRONTUARIO},{6,ACS},{10,AUXILIARES DA AUDITORIA} | ✅ | field-by-field match |
| 3 | Lookup match | `GET /api/perfis/lookup?q=enfermeiro` | ENFERMEIRO (id 2) | [{2,ENFERMEIRO}] | ✅ | R for hpromptsau_prf |
| 3b| Lookup ordering | `?q=aud` | AUXILIARES/ESTAGIARIO auditoria | [{10,…AUDITORIA},{9,ESTAGIARIO AUDITORIA}] | ✅ | nome ASC |
| 4 | Not found | `GET /api/perfis/9999` | no row | 404 | ✅ | |
| 5 | Type fidelity | list+get | PrfCod integer, PrfNom varchar(50) | id:int, nome:string | ✅ | matches entity & live DB; no @Convert needed |
| 6a| AuthZ no-token (list) | `GET /api/perfis` no Authorization | admin-gated | 401 | ✅ | R9 |
| 6b| AuthZ CADASTRO-only (list) | CADASTRO JWT | requires SAUDE_ADMIN | 403 | ✅ | R9 — confirms admin gating (get by id also 403) |
| 6c| AuthZ lookup access | CADASTRO JWT→200; no-token→401 | lookup isAuthenticated | 200 / 401 | ✅ | lookup is authenticated-only, not admin-gated |

## Divergences
None (business-equivalence). One documented behavioral note, not a divergence:
- **Lookup is case-insensitive substring:** `?q=enf` returns ENFERMEIRO **and** "AUXILIAR OU TÉCNICO DE ENFERMAGEM"
  (both contain "enf"). This is a superset autocomplete, expected of a synthesized `/lookup` (spec OQ1: no hprompt
  1:1 mapping). `?q=enfermeiro` correctly narrows to ENFERMEIRO alone. Accepted as intended autocomplete behavior.

## Verdict: **PARITY** (read-only subset). Write-parity DEFERRED — see scope note.
DB snapshot unchanged after run (count 10, max(prfcod) 10) — **ZERO writes confirmed**.
