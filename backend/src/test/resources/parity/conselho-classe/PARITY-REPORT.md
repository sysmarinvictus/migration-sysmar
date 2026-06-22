# SAU_CONCLA (Conselho de Classe) — Golden-master parity report
- **Date:** 2026-06-22 · **Method:** DB-state vs shared snapshot (40 rows) · **Auth:** JWT SAUDE_CADASTRO
- **Type audit:** entity ↔ live types all MATCH (ConClaCod smallint↔Short; sigla/nome varchar↔String).

## Result: 11/11 PARITY
list ✅ · get/404 ✅ · insert valid (cod+sig+nome) ✅ · insert codigo-only ✅ · duplicate→409 ✅ ·
codigo>999→reject(400)* ✅ · update sigla+nome ✅ · update-changing-codigo (path id wins, no new row) ✅ ·
delete unused→204 ✅ · delete referenced by SAU_PRO→409 (R3) ✅.

\* codigo>999 rejects via 400 (Bean Validation @Max) not 422 — rejects as legacy (status nuance, RF6).
Client-supplied PK (R1 range 0..999 + R2 uniqueness). No business divergences. Snapshot restored (40 rows).
