# SAU_LOC (Local) — Golden-master parity report
- **Date:** 2026-06-22 · **Method:** DB-state vs shared snapshot (SAU_LOC empty → self-seed, codes 990000+)
- **Auth:** JWT SAUDE_CADASTRO · **Type audit:** entity ↔ live types all MATCH (LocCod/LocMunCod integer↔Integer; LocNom varchar↔String).

## Result: 11/11 PARITY
list ✅ · get/404 ✅ · insert valid → 201 + derives municipioNome (MANDAGUARI) ✅ ·
insert missing nome → reject(400)* ✅ · zero/missing municipio → reject(400)* ✅ · unknown municipio → 422 ✅ ·
duplicate codigo → 409 ✅ · codigo>999999 → reject(400)* ✅ · update nome+municipio ✅ · delete (no guard, R5) → 204 ✅.

\* required-field / range failures reject via 400 (Bean Validation) not 422 — reject as legacy (status nuance, RF6).
Client-supplied PK; municipio FK validated (R3/R4 → 422); no delete-guard (R5). No business divergences. Snapshot restored (0 rows).
