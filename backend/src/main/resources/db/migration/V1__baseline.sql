-- ============================================================================
-- V1 BASELINE — existing saude-mandaguari schema (GeneXus-created).
--
-- On the REAL database this migration is the Flyway *baseline marker* and is NOT executed
-- (spring.flyway.baseline-on-migrate=true adopts the already-populated DB at version 1).
--
-- On an EMPTY database (Testcontainers integration tests, baseline-on-migrate=false) this DDL
-- is executed to recreate the tables under test.
--
-- ⚠ PARTIAL BASELINE. Only the tables needed by migrated slices so far are defined here.
--   gx-schema-mapper must introspect the live DB and append every remaining table (full DDL) so
--   `hibernate ddl-auto=validate` passes against production. Column names/types/nullability below
--   are best-effort from gx attribute mining and MUST be confirmed against the live schema.
--
-- Identifiers are unquoted → PostgreSQL folds them to lowercase, matching Hibernate's
-- PhysicalNamingStrategyStandardImpl (also unquoted). Keep it that way for consistency.
-- ============================================================================

-- CBO occupation lookup (SAU_CBOR) — minimal columns used by especialidade FK display.
CREATE TABLE IF NOT EXISTS SAU_CBOR (
    CborCod  INTEGER     NOT NULL,
    CborDes  VARCHAR(100),
    CONSTRAINT pk_sau_cbor PRIMARY KEY (CborCod)
);

-- Especialidade (SAU_ESP) — reference slice.
CREATE TABLE IF NOT EXISTS SAU_ESP (
    EspCod                          INTEGER     NOT NULL,
    EspNom                          VARCHAR(50) NOT NULL,
    EspSit                          VARCHAR(1),
    EspAux                          BOOLEAN,
    EspCborCod                      INTEGER,
    -- scheduling-queue parameters per urgency tier (estagnado / tempo-máximo / vagas min-max)
    EspLstAgendEstagnadoMuitoUrg    INTEGER,
    EspLstAgendEstagnadoNormal      INTEGER,
    EspLstAgendEstagnadoPri         INTEGER,
    EspLstAgendEstagnadoUrg         INTEGER,
    EspLstAgendTempoMaxMuitoUrg     INTEGER,
    EspLstAgendTempoMaxNormal       INTEGER,
    EspLstAgendTempoMaxPri          INTEGER,
    EspLstAgendTempoMaxUrg          INTEGER,
    EspLstAgendVagaMuitoUrgMax      INTEGER,
    EspLstAgendVagaMuitoUrgMin      INTEGER,
    EspLstAgendVagaNorMax           INTEGER,
    EspLstAgendVagaNorMin           INTEGER,
    EspLstAgendVagaPriMax           INTEGER,
    EspLstAgendVagaPriMin           INTEGER,
    EspLstAgendVagaUrgMax           INTEGER,
    EspLstAgendVagaUrgMin           INTEGER,
    CONSTRAINT pk_sau_esp PRIMARY KEY (EspCod),
    CONSTRAINT fk_sau_esp_cbor FOREIGN KEY (EspCborCod) REFERENCES SAU_CBOR (CborCod)
);

-- Profissional × Especialidade link (SAU_PROESP) — minimal, used by the R4 delete-guard query.
-- Full definition belongs to the profissional-especialidade slice (Wave 4).
CREATE TABLE IF NOT EXISTS SAU_PROESP (
    ProCod   INTEGER NOT NULL,
    EspCod   INTEGER NOT NULL,
    CONSTRAINT pk_sau_proesp PRIMARY KEY (ProCod, EspCod)
);

-- Conselho de Classe (SAU_CONCLA) — professional licensing board (CRM/COREN/CRF).
-- Types from the GeneXus reorg DDL (SAU_CONCLAConversion.xml): smallint PK, nullable varchars.
CREATE TABLE IF NOT EXISTS SAU_CONCLA (
    ConClaCod    SMALLINT     NOT NULL,
    ConClaSigra  VARCHAR(10),
    ConClaNom    VARCHAR(100),
    CONSTRAINT pk_sau_concla PRIMARY KEY (ConClaCod)
);

-- Profissionais (SAU_PRO) — MINIMAL stub: only the column used by the SAU_CONCLA delete-guard (R3).
-- The full table is an XL Wave-4 slice; this definition will be expanded when SAU_PRO is migrated.
CREATE TABLE IF NOT EXISTS SAU_PRO (
    ProCod     INTEGER  NOT NULL,
    ConClaCod  SMALLINT,
    CONSTRAINT pk_sau_pro PRIMARY KEY (ProCod)
);

-- Município (SYS_MUN) — MINIMAL stub: columns used by the SAU_LOC FK existence + derived display
-- (municipioNome/Uf/Ibge). SYS_MUN is a SYS_* system table migrated separately; expand then.
CREATE TABLE IF NOT EXISTS SYS_MUN (
    MunCod   INTEGER NOT NULL,
    MunNom   VARCHAR(60),
    MunUF    VARCHAR(2),
    MunIBGE  VARCHAR(7),
    CONSTRAINT pk_sys_mun PRIMARY KEY (MunCod)
);

-- Local (SAU_LOC) — locality within a município. From the reorg DDL (new table).
CREATE TABLE IF NOT EXISTS SAU_LOC (
    LocCod     INTEGER     NOT NULL,
    LocNom     VARCHAR(50),
    LocMunCod  INTEGER,
    CONSTRAINT pk_sau_loc PRIMARY KEY (LocCod),
    CONSTRAINT fk_sau_loc_mun FOREIGN KEY (LocMunCod) REFERENCES SYS_MUN (MunCod)
);
CREATE INDEX IF NOT EXISTS isau_loc1 ON SAU_LOC (LocMunCod);

-- Tipo de Medicamento (SAU_TIPREM) — medication-type catalog. From the reorg DDL.
CREATE TABLE IF NOT EXISTS SAU_TIPREM (
    TipRemCod  INTEGER     NOT NULL,
    TipRemDes  VARCHAR(50),
    CONSTRAINT pk_sau_tiprem PRIMARY KEY (TipRemCod)
);

-- Medicamento (SAU_REM) — MINIMAL stub: only the column used by the SAU_TIPREM delete-guard (R3).
-- The full table is an L Wave-3 slice; this definition will be expanded when SAU_REM is migrated.
CREATE TABLE IF NOT EXISTS SAU_REM (
    RemCod     INTEGER NOT NULL,
    TipRemCod  INTEGER,
    CONSTRAINT pk_sau_rem PRIMARY KEY (RemCod)
);
