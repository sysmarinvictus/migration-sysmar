-- ============================================================================
-- V3: Upgrade SAU_REM constellation stubs to full production definitions.
--
-- Previous state after V1+V2:
--   SAU_REM        — 2-col stub (RemCod PK + TipRemCod only)
--   SAU_REM1       — wrong single-col PK (RemCod), nullable RemUniCod, missing RemEstMin/RemUniSit
--   SAU_REM2       — absent entirely
--   SAU_REM_UNISETOR — wrong 2-col PK, RemUniSetorSeq is SMALLINT not INTEGER,
--                      RemUniSetorUniCod nullable; V2 added data columns
--   SAU_REMPOSO    — correct in V1; add FK constraints only
--
-- Idempotent: safe against Testcontainers (V1→V2→V3 on empty DB) and live DB
-- (spring.flyway.baseline-on-migrate=true, only V3 runs).
-- ADD COLUMN IF NOT EXISTS, IF NOT EXISTS, DO $$ EXCEPTION WHEN ... $$ patterns throughout.
-- ============================================================================

-- ============================================================================
-- 1. SAU_REM — expand 2-col stub to all 28 columns
-- ============================================================================

ALTER TABLE SAU_REM
    ADD COLUMN IF NOT EXISTS RemTipoProduto          SMALLINT,
    ADD COLUMN IF NOT EXISTS RemUsuLogin             CHAR(20),
    ADD COLUMN IF NOT EXISTS RemNom                  VARCHAR(250),
    ADD COLUMN IF NOT EXISTS RemCon                  VARCHAR(150),
    ADD COLUMN IF NOT EXISTS RemFarBas               SMALLINT,
    ADD COLUMN IF NOT EXISTS RemPsico                SMALLINT,
    ADD COLUMN IF NOT EXISTS RemConEsp               SMALLINT,
    ADD COLUMN IF NOT EXISTS RemEti                  SMALLINT,
    ADD COLUMN IF NOT EXISTS RemVlrHos               NUMERIC(11,4),
    ADD COLUMN IF NOT EXISTS RemVlrUni               NUMERIC(11,4),
    ADD COLUMN IF NOT EXISTS RemSemRename            SMALLINT,
    ADD COLUMN IF NOT EXISTS RemPortariaPsicotropico VARCHAR(20),
    ADD COLUMN IF NOT EXISTS RemSit                  SMALLINT,
    ADD COLUMN IF NOT EXISTS RemOmitirSaldo          SMALLINT,
    ADD COLUMN IF NOT EXISTS RemUsarPosologia        SMALLINT,
    ADD COLUMN IF NOT EXISTS RemMPP                  SMALLINT,
    ADD COLUMN IF NOT EXISTS RemMPPDes               VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS RemMPPCanMotivo         VARCHAR(300),
    ADD COLUMN IF NOT EXISTS RemMPPCanData           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS RemMppCanUsuLogin       CHAR(20),
    ADD COLUMN IF NOT EXISTS RemUniSetorSeqUlt       INTEGER,
    ADD COLUMN IF NOT EXISTS DcbCod                  CHAR(10),
    ADD COLUMN IF NOT EXISTS RENAMECod               VARCHAR(20),
    ADD COLUMN IF NOT EXISTS RenameAtualCod          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS AprRemCod               INTEGER,
    ADD COLUMN IF NOT EXISTS ObmCod                  VARCHAR(30);
-- RemCod (PK) and TipRemCod already exist in the V1 stub — not repeated.

-- Indexes on SAU_REM (GeneXus physical names, lowercase per PG folding)
CREATE INDEX IF NOT EXISTS usau_rem          ON SAU_REM (RemNom);
CREATE INDEX IF NOT EXISTS usau_rem_desc     ON SAU_REM (RemCod DESC);
CREATE INDEX IF NOT EXISTS isau_rem1         ON SAU_REM (RENAMECod, RenameAtualCod);
CREATE INDEX IF NOT EXISTS isau_rem2         ON SAU_REM (AprRemCod);
CREATE INDEX IF NOT EXISTS isau_rem3         ON SAU_REM (TipRemCod);
CREATE INDEX IF NOT EXISTS isau_rem4         ON SAU_REM (ObmCod);
CREATE INDEX IF NOT EXISTS isau_rem6         ON SAU_REM (DcbCod);

-- FK to SAU_TIPREM (Wave 1, already migrated)
DO $$ BEGIN
    ALTER TABLE SAU_REM ADD CONSTRAINT fk_sau_rem_tiprem
        FOREIGN KEY (TipRemCod) REFERENCES SAU_TIPREM (TipRemCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Deferred FKs (tables not yet migrated):
--   DcbCod → SAU_DCB, RENAMECod → SAU_RENAME, RenameAtualCod → SAU_RENAMEATUALIZADO,
--   (RENAMECod, RenameAtualCod) → SAU_RENAME_DEPARA, AprRemCod → SAU_APRREM, ObmCod → OBM
-- Add these constraints in the migration that migrates each respective table.

-- ============================================================================
-- 2. SAU_REM1 — fix stub: single-col PK → (RemCod, RemUniCod); set RemUniCod NOT NULL;
--               add missing data columns
-- ============================================================================

ALTER TABLE SAU_REM1
    ADD COLUMN IF NOT EXISTS RemEstMin INTEGER,
    ADD COLUMN IF NOT EXISTS RemUniSit SMALLINT;

-- RemUniCod was nullable in the V1 stub; GeneXus never inserts NULL here.
ALTER TABLE SAU_REM1 ALTER COLUMN RemUniCod SET NOT NULL;

-- Replace the wrong single-column PK with the correct composite PK.
-- Detects by counting key columns on any existing PK; drops only if fewer than 2.
DO $$
DECLARE
    v_con   text;
    v_ncols int;
BEGIN
    SELECT tc.constraint_name
      INTO v_con
      FROM information_schema.table_constraints tc
     WHERE tc.table_schema    = current_schema()
       AND tc.table_name      = 'sau_rem1'
       AND tc.constraint_type = 'PRIMARY KEY';

    IF v_con IS NOT NULL THEN
        SELECT COUNT(*)
          INTO v_ncols
          FROM information_schema.key_column_usage
         WHERE table_schema    = current_schema()
           AND table_name      = 'sau_rem1'
           AND constraint_name = v_con;

        IF v_ncols < 2 THEN
            EXECUTE 'ALTER TABLE SAU_REM1 DROP CONSTRAINT ' || quote_ident(v_con);
            ALTER TABLE SAU_REM1 ADD CONSTRAINT pk_sau_rem1 PRIMARY KEY (RemCod, RemUniCod);
        END IF;
        -- v_ncols >= 2: correct PK already exists; nothing to do.
    ELSE
        -- No PK at all (should not happen, but defensive)
        ALTER TABLE SAU_REM1 ADD CONSTRAINT pk_sau_rem1 PRIMARY KEY (RemCod, RemUniCod);
    END IF;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- FK: SAU_REM1 → SAU_REM (parent aggregate)
DO $$ BEGIN
    ALTER TABLE SAU_REM1 ADD CONSTRAINT fk_sau_rem1_rem
        FOREIGN KEY (RemCod) REFERENCES SAU_REM (RemCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- FK: SAU_REM1 → SAU_UNI (already defined in V1)
DO $$ BEGIN
    ALTER TABLE SAU_REM1 ADD CONSTRAINT fk_sau_rem1_uni
        FOREIGN KEY (RemUniCod) REFERENCES SAU_UNI (UniCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS isau_rem5 ON SAU_REM1 (RemUniCod);

-- ============================================================================
-- 3. SAU_REM2 — new table; absent from V1/V2
-- ============================================================================

CREATE TABLE IF NOT EXISTS SAU_REM2 (
    RemCod   INTEGER NOT NULL,
    RemEan13 BIGINT  NOT NULL,
    CONSTRAINT pk_sau_rem2     PRIMARY KEY (RemCod, RemEan13),
    CONSTRAINT fk_sau_rem2_rem FOREIGN KEY (RemCod) REFERENCES SAU_REM (RemCod)
);

-- ============================================================================
-- 4. SAU_REM_UNISETOR — fix PK (2-col → 3-col), fix RemUniSetorSeq type
--    (SMALLINT → INTEGER), set RemUniSetorUniCod NOT NULL.
--    V2 already added the missing data columns (SetorCod, EstMin, Sit).
-- ============================================================================

-- Widen sequence column: SMALLINT → INTEGER (safe widening, no data loss)
ALTER TABLE SAU_REM_UNISETOR ALTER COLUMN RemUniSetorSeq    TYPE INTEGER;

-- RemUniSetorUniCod was nullable in the V1 stub; GeneXus never inserts NULL here.
ALTER TABLE SAU_REM_UNISETOR ALTER COLUMN RemUniSetorUniCod SET NOT NULL;

-- Replace the wrong 2-column PK with the correct 3-column PK.
DO $$
DECLARE
    v_con   text;
    v_ncols int;
BEGIN
    SELECT tc.constraint_name
      INTO v_con
      FROM information_schema.table_constraints tc
     WHERE tc.table_schema    = current_schema()
       AND tc.table_name      = 'sau_rem_unisetor'
       AND tc.constraint_type = 'PRIMARY KEY';

    IF v_con IS NOT NULL THEN
        SELECT COUNT(*)
          INTO v_ncols
          FROM information_schema.key_column_usage
         WHERE table_schema    = current_schema()
           AND table_name      = 'sau_rem_unisetor'
           AND constraint_name = v_con;

        IF v_ncols < 3 THEN
            EXECUTE 'ALTER TABLE SAU_REM_UNISETOR DROP CONSTRAINT ' || quote_ident(v_con);
            ALTER TABLE SAU_REM_UNISETOR ADD CONSTRAINT pk_sau_rem_unisetor
                PRIMARY KEY (RemCod, RemUniSetorSeq, RemUniSetorUniCod);
        END IF;
    ELSE
        ALTER TABLE SAU_REM_UNISETOR ADD CONSTRAINT pk_sau_rem_unisetor
            PRIMARY KEY (RemCod, RemUniSetorSeq, RemUniSetorUniCod);
    END IF;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- FK: SAU_REM_UNISETOR → SAU_REM
DO $$ BEGIN
    ALTER TABLE SAU_REM_UNISETOR ADD CONSTRAINT fk_sau_rem_unisetor_rem
        FOREIGN KEY (RemCod) REFERENCES SAU_REM (RemCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- FK: SAU_REM_UNISETOR → SAU_UNI
DO $$ BEGIN
    ALTER TABLE SAU_REM_UNISETOR ADD CONSTRAINT fk_sau_rem_unisetor_uni
        FOREIGN KEY (RemUniSetorUniCod) REFERENCES SAU_UNI (UniCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Composite FK: SAU_REM_UNISETOR → SAU_UNISETOR (UniCod, SetorCod).
-- RemUniSetorSetorCod is nullable; PostgreSQL skips the FK check when any FK column is NULL.
DO $$ BEGIN
    ALTER TABLE SAU_REM_UNISETOR ADD CONSTRAINT fk_sau_rem_unisetor_setor
        FOREIGN KEY (RemUniSetorUniCod, RemUniSetorSetorCod)
        REFERENCES SAU_UNISETOR (UniCod, SetorCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Alternate-key index used by the uniqueness check (rule R30: (UniCod, SetorCod, RemCod) unique)
CREATE INDEX IF NOT EXISTS usau_rem_unisetor
    ON SAU_REM_UNISETOR (RemUniSetorUniCod, RemUniSetorSetorCod, RemCod);

-- ============================================================================
-- 5. SAU_REMPOSO — structure correct in V1; add FK constraints and missing index
-- ============================================================================

DO $$ BEGIN
    ALTER TABLE SAU_REMPOSO ADD CONSTRAINT fk_sau_remposo_rem
        FOREIGN KEY (RemCod) REFERENCES SAU_REM (RemCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE SAU_REMPOSO ADD CONSTRAINT fk_sau_remposo_remobs
        FOREIGN KEY (PosoRemObsCod) REFERENCES SAU_REMOBS (RemObsCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS isau_remposo1 ON SAU_REMPOSO (PosoRemObsCod);
