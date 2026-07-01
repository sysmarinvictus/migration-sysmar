-- SAU_RECESP (Receituário Controle Especial) master + SAU_RECESP1 line items — promote the baseline stubs
-- to the full live shape so local/Testcontainers ddl-auto=validate passes. The live saude-mandaguari DB
-- already has every column (master 11 cols / child 13 cols, live-verified 2026-07-01,
-- host.docker.internal:5432) → every statement here is a NO-OP on the live DB. Idempotent; no data touched;
-- no DROP. Portaria 344/98: this migration does NOT add any status/cancel column (the live table has none;
-- cancellation/retention modeling is a deliberate regulatory decision — SLICE-SPEC OQ2).
--
-- Columns already added by prior migrations are intentionally NOT repeated:
--   master: FunPesCod (V1:508), RecEspProPesCod (V8), PacPesCod (V13)
--   child:  RemCod (V5), RemObsCod (V1 baseline)

-- ── master SAU_RECESP: the 6 remaining columns ──
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS RecEspDat      DATE;
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS RecEspSeqUlt   SMALLINT;
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS RecEspUsuLogin CHAR(20);
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS RecEspCon      SMALLINT;
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS RecTip         SMALLINT;
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS RecObs         VARCHAR(300);

-- Align RecEspUsuLogin to the live CHAR(20) if an earlier stub created it VARCHAR (matches the entity's
-- @JdbcTypeCode(CHAR) mapping; PG skips the rewrite when already char(20) → no-op on live). Precedent V13.
ALTER TABLE SAU_RECESP ALTER COLUMN RecEspUsuLogin TYPE CHAR(20);

-- ── child SAU_RECESP1: the 8 remaining columns ──
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecEspPre    VARCHAR(50);
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecEspQtd    NUMERIC(5,1);
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecEspQtdTip SMALLINT;
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecEspObs    VARCHAR(60);
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecEspTip    SMALLINT;
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecEspTipUso SMALLINT;
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecEspUsoCon SMALLINT;
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RecInd       BOOLEAN;
