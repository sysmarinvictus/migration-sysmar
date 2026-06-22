-- ============================================================================
-- V5: SAU_REM (Medicamento) support objects.
--
-- The full SAU_REM constellation (SAU_REM + SAU_REM1/REM2/REM_UNISETOR/REMPOSO,
-- PKs + FKs to SAU_TIPREM/SAU_UNI) was already built in V3. This migration adds:
--   (0) seq_sau_rem_cod — RemCod generation (R15; OQ-6 resolved: live DB has no sequence).
--   (1) Minimal stubs for the lookup/guard tables SAU_REM references that are not yet
--       migrated as their own slices — needed so Testcontainers builds a schema the
--       FK-existence checks (R2-R8) and delete-guards (R19-R22) can run against. All real
--       tables already exist in the live DB, so every statement is IF NOT EXISTS / idempotent.
--   (2) SAU_RECESP1.RemCod — present in the live DB but absent from the V1 stub; required by
--       the R21 delete-guard and R53 controlled-substance check.
--
-- Idempotent: CREATE ... IF NOT EXISTS / ADD COLUMN IF NOT EXISTS throughout. Column casing
-- follows the existing GeneXus physical names (PostgreSQL folds unquoted identifiers to lower).
-- ============================================================================

-- 0. RemCod sequence (seeded from MAX so it never collides with the 799 live rows).
CREATE SEQUENCE IF NOT EXISTS seq_sau_rem_cod;
DO $$
BEGIN
    PERFORM setval('seq_sau_rem_cod', COALESCE((SELECT MAX(RemCod) FROM SAU_REM), 0) + 1, false);
END $$;

-- 1. Lookup / FK-target stubs (real tables already exist in the live DB).
CREATE TABLE IF NOT EXISTS SAU_DCB (
    DcbCod  CHAR(10)     NOT NULL,
    DcbDes  VARCHAR(250),
    CONSTRAINT pk_sau_dcb PRIMARY KEY (DcbCod)
);

CREATE TABLE IF NOT EXISTS SAU_RENAME (
    RENAMECod      VARCHAR(20) NOT NULL,
    RENAMEPrincAt  VARCHAR(300),
    RENAMEConc     VARCHAR(50),
    RENAMEFormFarm VARCHAR(50),
    RENAMEVol      VARCHAR(50),
    RENAMEUnd      VARCHAR(20),
    CONSTRAINT pk_sau_rename PRIMARY KEY (RENAMECod)
);

CREATE TABLE IF NOT EXISTS SAU_RENAMEATUALIZADO (
    RenameAtualCod          VARCHAR(20) NOT NULL,
    RenameAtualDesc         VARCHAR(300),
    RenameAtualBasico       BOOLEAN,
    RenameAtualEstrategico  BOOLEAN,
    RenameAtualProprio      BOOLEAN,
    RenameAtualEspecializado BOOLEAN,
    CONSTRAINT pk_sau_renameatualizado PRIMARY KEY (RenameAtualCod)
);

CREATE TABLE IF NOT EXISTS SAU_RENAME_DEPARA (
    RENAMECod      VARCHAR(20) NOT NULL,
    RenameAtualCod VARCHAR(20) NOT NULL,
    CONSTRAINT pk_sau_rename_depara PRIMARY KEY (RENAMECod, RenameAtualCod)
);

CREATE TABLE IF NOT EXISTS OBM (
    ObmCod  VARCHAR(30) NOT NULL,
    ObmNome VARCHAR(2000),
    CONSTRAINT pk_obm PRIMARY KEY (ObmCod)
);

CREATE TABLE IF NOT EXISTS SAU_APRREM (
    AprRemCod INTEGER NOT NULL,
    AprRemDes VARCHAR(30),
    AprRemAbr CHAR(5),
    CONSTRAINT pk_sau_aprrem PRIMARY KEY (AprRemCod)
);

CREATE TABLE IF NOT EXISTS InteracaoMedicamentosa (
    InteraMedCod  INTEGER NOT NULL,
    InteraRemCod1 INTEGER,
    InteraRemCod2 INTEGER,
    CONSTRAINT pk_interacaomedicamentosa PRIMARY KEY (InteraMedCod)
);

-- 2. SAU_RECESP1.RemCod — present live, absent from the V1 stub (R21 / R53 guard).
ALTER TABLE SAU_RECESP1 ADD COLUMN IF NOT EXISTS RemCod INTEGER;

-- 3. Boolean flags. The live saude-mandaguari schema stores RemMPP/RemOmitirSaldo/RemSemRename/
--    RemUsarPosologia as BOOLEAN (confirmed 2026-06-21), but V3 declared them SMALLINT. Align the
--    Flyway-built (Testcontainers) schema to the live type. Idempotent: only converts when the
--    column is still smallint, so it is a no-op against the already-boolean live DB.
DO $$
DECLARE
    col TEXT;
BEGIN
    FOREACH col IN ARRAY ARRAY['remmpp','remomitirsaldo','remsemrename','remusarposologia'] LOOP
        IF (SELECT data_type FROM information_schema.columns
            WHERE table_name = 'sau_rem' AND column_name = col) = 'smallint' THEN
            EXECUTE format(
                'ALTER TABLE SAU_REM ALTER COLUMN %I TYPE BOOLEAN USING (%I <> 0)', col, col);
        END IF;
    END LOOP;
END $$;
