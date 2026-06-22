-- ============================================================================
-- V4: SAU_UNI corrections + 4 sub-table creations.
--
-- V1 SAU_UNI had three issues confirmed by SAU_UNIConversion.xml comparison:
--   (a) UniCnpj/UniCep/UniLicFun declared VARCHAR but authoritative DDL is CHAR.
--   (b) 7 GeneXus indexes missing (only isau_uni_nom was present, which is NOT in the spec).
--   (c) UniMunCod FK → SYS_MUN missing (SYS_MUN stub exists in V1).
--   (d) UniProPes*Cod FK → SAU_PRO: intentionally deferred (Wave 4).
--
-- Sub-tables SAU_UNI1, SAU_UNI2, SAU_UNI3, SAU_UNISALA are absent from V1/V2/V3.
-- DDL taken directly from SAU_UNI1/2/3/SALAConversion.xml <Statements> blocks.
--
-- Idempotent: safe against Testcontainers (V1→…→V4 on empty DB) and live DB.
-- ============================================================================

-- ============================================================================
-- 0. UniCod sequence — UniCod is generated (Autonumber=No but form hides it;
--    confirmed OQ11 2026-06-21: server generates value).
--    Seed from current MAX so the sequence never collides with live data.
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS seq_sau_uni_cod;
DO $$
BEGIN
    PERFORM setval(
        'seq_sau_uni_cod',
        COALESCE((SELECT MAX(UniCod) FROM SAU_UNI), 0) + 1,
        false   -- false = next call returns this value (start-from semantics)
    );
END $$;

-- ============================================================================
-- 1. SAU_UNI — fix type mismatches and add missing indexes / FK
-- ============================================================================

-- (a) Fix CHAR columns that V1 incorrectly declared as VARCHAR.
--     ALTER COLUMN TYPE is a no-op when the column is already the target type.
ALTER TABLE SAU_UNI ALTER COLUMN UniCnpj    TYPE CHAR(18)  USING UniCnpj::bpchar;
ALTER TABLE SAU_UNI ALTER COLUMN UniCep     TYPE CHAR(8)   USING UniCep::bpchar;
ALTER TABLE SAU_UNI ALTER COLUMN UniLicFun  TYPE CHAR(10)  USING UniLicFun::bpchar;

-- (b) Missing GeneXus indexes (authoritative names from SAU_UNIConversion.xml)
CREATE INDEX IF NOT EXISTS isau_uni4      ON SAU_UNI (UniMunCod);
CREATE INDEX IF NOT EXISTS usau_uni_desc  ON SAU_UNI (UniCod DESC);
CREATE INDEX IF NOT EXISTS isau_uni1      ON SAU_UNI (UniDisCod);
CREATE INDEX IF NOT EXISTS isau_uni2      ON SAU_UNI (UniProPesRespCod);
CREATE INDEX IF NOT EXISTS isau_uni6      ON SAU_UNI (UniProPesDirCod);
CREATE INDEX IF NOT EXISTS isau_uni7      ON SAU_UNI (UniProPesAudCod);
CREATE INDEX IF NOT EXISTS isau_uni8      ON SAU_UNI (UniProPesAutCod);

-- (c) FK: UniMunCod → SYS_MUN (SYS_MUN stub is in V1, safe to add now)
DO $$ BEGIN
    ALTER TABLE SAU_UNI ADD CONSTRAINT fk_sau_uni_mun
        FOREIGN KEY (UniMunCod) REFERENCES SYS_MUN (MunCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Deferred FKs (SAU_PRO = Wave 4; add in the Wave-4 migration that creates SAU_PRO properly):
--   UniProPesRespCod → SAU_PRO(ProPesCod)   constraint ISAU_UNI2
--   UniProPesDirCod  → SAU_PRO(ProPesCod)   constraint ISAU_UNI6
--   UniProPesAudCod  → SAU_PRO(ProPesCod)   constraint ISAU_UNI7
--   UniProPesAutCod  → SAU_PRO(ProPesCod)   constraint ISAU_UNI8

-- ============================================================================
-- 2. SAU_UNI1 — Hiperdia: professionals linked to a health unit
--    DDL from SAU_UNI1Conversion.xml <Statements>
-- ============================================================================

CREATE TABLE IF NOT EXISTS SAU_UNI1 (
    UniCod        INTEGER  NOT NULL,
    UniProPesCod  BIGINT   NOT NULL,
    UniProDatInc  DATE,
    UniProMat     CHAR(20),
    UniProCBO     VARCHAR(8),
    UniProSta     SMALLINT,
    UniProDatDes  DATE,
    CONSTRAINT pk_sau_uni1 PRIMARY KEY (UniCod, UniProPesCod)
);

-- Secondary index on the professional FK column.
-- Named idx_sau_uni1_propes (not isau_uni13) to avoid collision with the eventual
-- FK constraint isau_uni13 that references SAU_PRO once it is migrated in Wave 4.
CREATE INDEX IF NOT EXISTS idx_sau_uni1_propes ON SAU_UNI1 (UniProPesCod);

DO $$ BEGIN
    ALTER TABLE SAU_UNI1 ADD CONSTRAINT fk_sau_uni1_uni
        FOREIGN KEY (UniCod) REFERENCES SAU_UNI (UniCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Deferred: UniProPesCod → SAU_PRO(ProPesCod) [constraint isau_uni13] — add in Wave 4.

-- ============================================================================
-- 3. SAU_UNI2 — Sisprenatal: professional+specialty gestores per health unit
--    DDL from SAU_UNI2Conversion.xml <Statements>
-- ============================================================================

CREATE TABLE IF NOT EXISTS SAU_UNI2 (
    UniCod           INTEGER NOT NULL,
    UniGesProPesCod  BIGINT  NOT NULL,
    UniGesEspCod     INTEGER NOT NULL,
    UniGesDatInc     DATE,
    UniGesSta        SMALLINT,
    UniGesDatDes     DATE,
    CONSTRAINT pk_sau_uni2 PRIMARY KEY (UniCod, UniGesProPesCod, UniGesEspCod)
);

-- Named idx_sau_uni2_proesp (not isau_uni3) to avoid collision with the eventual
-- FK constraint isau_uni3 that references SAU_PROESP once it is migrated in Wave 4.
CREATE INDEX IF NOT EXISTS idx_sau_uni2_proesp ON SAU_UNI2 (UniGesProPesCod, UniGesEspCod);

DO $$ BEGIN
    ALTER TABLE SAU_UNI2 ADD CONSTRAINT fk_sau_uni2_uni
        FOREIGN KEY (UniCod) REFERENCES SAU_UNI (UniCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Deferred: (UniGesProPesCod, UniGesEspCod) → SAU_PROESP(ProPesCod, EspCod) [constraint isau_uni3] — add in Wave 4.

-- ============================================================================
-- 4. SAU_UNI3 — Uni Nut Pro Pes: nutritionist professionals per health unit
--    DDL from SAU_UNI3Conversion.xml <Statements>
-- ============================================================================

CREATE TABLE IF NOT EXISTS SAU_UNI3 (
    UniCod           INTEGER NOT NULL,
    UniNutProPesCod  BIGINT  NOT NULL,
    UniNutEspCod     INTEGER NOT NULL,
    UniNutDatInc     DATE,
    UniNutSta        SMALLINT,
    UniNutDatDes     DATE,
    CONSTRAINT pk_sau_uni3 PRIMARY KEY (UniCod, UniNutProPesCod, UniNutEspCod)
);

-- Named idx_sau_uni3_proesp (not isau_uni5) to avoid collision with the eventual
-- FK constraint isau_uni5 that references SAU_PROESP once it is migrated in Wave 4.
CREATE INDEX IF NOT EXISTS idx_sau_uni3_proesp ON SAU_UNI3 (UniNutProPesCod, UniNutEspCod);

DO $$ BEGIN
    ALTER TABLE SAU_UNI3 ADD CONSTRAINT fk_sau_uni3_uni
        FOREIGN KEY (UniCod) REFERENCES SAU_UNI (UniCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Deferred: (UniNutProPesCod, UniNutEspCod) → SAU_PROESP(ProPesCod, EspCod) [constraint isau_uni5] — add in Wave 4.

-- ============================================================================
-- 5. SAU_UNISALA — Rooms per health unit
--    DDL from SAU_UNISALAConversion.xml <Statements>
-- ============================================================================

CREATE TABLE IF NOT EXISTS SAU_UNISALA (
    UniCod        INTEGER                     NOT NULL,
    SalaCod       SMALLINT                    NOT NULL,
    SalaNom       VARCHAR(100),
    SalaSta       CHAR(1),
    SalaDatAlt    TIMESTAMP WITHOUT TIME ZONE,
    SalaUsuLogin  VARCHAR(40),
    CONSTRAINT pk_sau_unisala PRIMARY KEY (UniCod, SalaCod)
);

-- No secondary indexes in SAU_UNISALAConversion.xml beyond the PK.

DO $$ BEGIN
    ALTER TABLE SAU_UNISALA ADD CONSTRAINT fk_sau_unisala_uni
        FOREIGN KEY (UniCod) REFERENCES SAU_UNI (UniCod);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
