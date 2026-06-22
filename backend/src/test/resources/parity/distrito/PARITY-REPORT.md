# SAU_DIS (Distrito) — Golden-master parity report
- **Date:** 2026-06-22 · **Method:** DB-state vs shared snapshot (SAU_DIS empty → scenarios self-seed, ZZDIS marker)
- **New app:** :8090, ddl-auto=none, Flyway off · **Auth:** JWT SAUDE_CADASTRO
- **Type audit:** entity ↔ live DB types all MATCH (no mismatch — this slice was mapped correctly).

## Result: 13/13 PARITY (+1 N/A by design)
list ✅ · get/404 ✅ · insert valid ✅ · insert minimal (FK=0) ✅ · invalid DisTipLogCod→422 ✅ ·
DisTipLogCod=0 accepted ✅ · invalid DisBaiCod→reject(400)* ✅ · non-digit DisDDD→422 ✅ ·
update nome ✅ · update bairroCodigo ✅ · delete unused→204 ✅ · delete referenced by SAU_UNI→409 ✅.

- **#6 "insert duplicate DisCod" → N/A**: codigo is server-assigned (MAX+1), so a duplicate cannot be
  submitted via the API — an accepted improvement over the legacy manual-code path.
- \* invalid DisBaiCod rejects via 400 (Bean Validation) not 422 — rejects as legacy does (status nuance, RF6).

Snapshot restored (0 rows); unidade 7's UniDisCod reverted after the delete-guard test. No business divergences.
