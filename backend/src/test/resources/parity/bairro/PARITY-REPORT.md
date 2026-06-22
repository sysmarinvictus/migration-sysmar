# SAU_BAI (Bairro) — Golden-master parity report
- **Date:** 2026-06-22 · **Method:** DB-state vs shared snapshot (944 rows) · **Auth:** JWT SAUDE_CADASTRO
- **Type audit:** entity ↔ live types MATCH (BaiCod integer↔Integer; BaiNom varchar↔String).

## Result: 12/12 PARITY
list ✅ · get/404 ✅ · insert valid (codigo auto MAX+1) ✅ · missing nome→reject(400)* ✅ ·
duplicate nome→409 (R3) ✅ · update nome ✅ · update-to-existing-nome→409 (R3) ✅ ·
delete unused→204 ✅ · delete ref by SAU_DIS→409 (R5, seeded) ✅ · delete ref by SYS_PES→409 (R4, seeded) ✅ ·
lookup by nome ✅.
\* missing nome rejects via 400 (Bean Validation) not 422 — rejects as legacy (status nuance, RF6).
Auto PK; nome uniqueness (R3); both delete-guards live-tested. No business divergences. Snapshot restored (944 rows).
