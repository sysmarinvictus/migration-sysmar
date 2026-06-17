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

-- Posologia (SAU_REMOBS) — dosage-instructions catalog. From the reorg DDL.
CREATE TABLE IF NOT EXISTS SAU_REMOBS (
    RemObsCod             INTEGER      NOT NULL,
    RemObsDes             VARCHAR(60),
    RemObsInternamento    BOOLEAN,
    RemObsQuantidadeDose  NUMERIC(8,2),
    RemObsMedidaDose      INTEGER,
    RemObsIntervaloHoras  SMALLINT,
    RemObsDuracaoDias     SMALLINT,
    RemObsUsuCod          INTEGER,
    CONSTRAINT pk_sau_remobs PRIMARY KEY (RemObsCod)
);
CREATE INDEX IF NOT EXISTS usau_remobs_desc ON SAU_REMOBS (RemObsCod DESC);

-- Posologia de Medicamento (SAU_REMPOSO) — MINIMAL stub: only columns used by the
-- SAU_REMOBS delete-guard R3 (PosoRemObsCod). Full table is Wave-3.
CREATE TABLE IF NOT EXISTS SAU_REMPOSO (
    RemCod         INTEGER NOT NULL,
    PosoRemObsCod  INTEGER NOT NULL,
    CONSTRAINT pk_sau_remposo PRIMARY KEY (RemCod, PosoRemObsCod)
);

-- Receituário Controle Especial items (SAU_RECESP1) — MINIMAL stub: only the column used by the
-- SAU_REMOBS delete-guard R4 (RemObsCod). Full table is Wave-6 / Portaria 344/98.
CREATE TABLE IF NOT EXISTS SAU_RECESP1 (
    RecEspUniCod  INTEGER NOT NULL,
    RecEspCod     BIGINT  NOT NULL,
    RecEspSeq     SMALLINT NOT NULL,
    RemObsCod     INTEGER,
    CONSTRAINT pk_sau_recesp1 PRIMARY KEY (RecEspUniCod, RecEspCod, RecEspSeq)
);

-- Bairro (SAU_BAI) — neighborhood catalog. From the reorg DDL (SAU_BAIConversion.xml).
CREATE TABLE IF NOT EXISTS SAU_BAI (
    BaiCod  INTEGER     NOT NULL,
    BaiNom  VARCHAR(50),
    CONSTRAINT pk_sau_bai PRIMARY KEY (BaiCod)
);
CREATE INDEX IF NOT EXISTS usau_bai_desc ON SAU_BAI (BaiCod DESC);

-- Pessoa (SYS_PES) — MINIMAL stub: only PesBaiCod used by the SAU_BAI delete-guard R4.
-- Full table is Wave-0 (person/auth foundation); this definition will be expanded then.
CREATE TABLE IF NOT EXISTS SYS_PES (
    PesCod    BIGINT  NOT NULL,
    PesBaiCod INTEGER,
    CONSTRAINT pk_sys_pes PRIMARY KEY (PesCod)
);

-- Tipo de Logradouro (SAU_TIPLOG) — address/street-type catalog (Rua, Av., etc.).
-- No GeneXus transaction form — read-only reference data. From SAU_TIPLOGConversion.xml.
CREATE TABLE IF NOT EXISTS SAU_TIPLOG (
    TipLogCod  INTEGER      NOT NULL,
    TipLogNom  VARCHAR(100),
    TipLogSig  VARCHAR(15),
    CONSTRAINT pk_sau_tiplog PRIMARY KEY (TipLogCod)
);

-- Distrito Sanitário (SAU_DIS) — full Wave-2 definition (all 11 columns from GeneXus INSERT cursor).
CREATE TABLE IF NOT EXISTS SAU_DIS (
    DisCod       SMALLINT    NOT NULL,
    DisNom       VARCHAR(30),
    DisEnd       VARCHAR(50),
    DisNum       SMALLINT,
    DisCom       VARCHAR(15),
    DisCEP       INTEGER,
    DisDDD       VARCHAR(3),
    DisFon       INTEGER,
    DisFax       INTEGER,
    DisTipLogCod INTEGER,
    DisBaiCod    INTEGER,
    CONSTRAINT pk_sau_dis PRIMARY KEY (DisCod)
);

-- Tipo de Unidade (SAU_TIPUNI) — MINIMAL stub for the TipUniCod FK in SAU_UNI.
CREATE TABLE IF NOT EXISTS SAU_TIPUNI (
    TipUniCod  INTEGER NOT NULL,
    TipUniNom  VARCHAR(50),
    CONSTRAINT pk_sau_tipuni PRIMARY KEY (TipUniCod)
);

-- Unidade de Atendimento (SAU_UNI) — full Wave-2 definition (all 55 columns from GeneXus INSERT SQL).
CREATE TABLE IF NOT EXISTS SAU_UNI (
    UniCod                  INTEGER  NOT NULL,
    UniForPesCod            BIGINT,
    UniNom                  VARCHAR(50),
    UniRazSoc               VARCHAR(50),
    UniCnpj                 VARCHAR(18),
    UniCep                  VARCHAR(8),
    UniEnd                  VARCHAR(70),
    UniEndNum               VARCHAR(10),
    UnEndCom                VARCHAR(40),
    UniBai                  VARCHAR(70),
    UniFon                  VARCHAR(20),
    UniFax                  VARCHAR(20),
    UniLicFun               VARCHAR(10),
    UniRes                  VARCHAR(50),
    UniEMail                VARCHAR(70),
    UniCnes                 INTEGER,
    UniBPA                  SMALLINT,
    UniSIPNI                SMALLINT,
    UniOrgEmi               VARCHAR(10),
    UniEsfAdm               SMALLINT,
    UniPSF                  SMALLINT,
    UniSisPreNatal          SMALLINT,
    UniHiperdia             SMALLINT,
    UniGes                  SMALLINT,
    UniSia                  VARCHAR(7),
    UniSigla                VARCHAR(6),
    UniSit                  SMALLINT,
    UniSIASUS               VARCHAR(7),
    UniScnesID              VARCHAR(20),
    UniExpEsus              BOOLEAN,
    UniExpBNAFAR            BOOLEAN,
    UniCadCNS               BOOLEAN,
    UniCadEnd               BOOLEAN,
    UniAteSemCNS            BOOLEAN,
    UniAteSemEnd            BOOLEAN,
    UniEncFisio             BOOLEAN,
    UniExt                  BOOLEAN,
    TipUniCod               INTEGER,
    UniAtencaoSecundaria    BOOLEAN,
    UniBloqPacSemCadInd     BOOLEAN,
    UniAvisoVacinaAtrasada  BOOLEAN,
    UniCadCPF               BOOLEAN,
    UniPainel               BOOLEAN,
    UniRecInterMedMpp       BOOLEAN,
    UniRecInterMedMppImp    BOOLEAN,
    UniBaiRemSemCns         BOOLEAN,
    UniBloqLancPcdAut       BOOLEAN,
    UniBloqDispPacExt       BOOLEAN,
    UniBloqAgSolExaPacExt   BOOLEAN,
    UniMunCod               INTEGER,
    UniProPesRespCod        BIGINT,
    UniProPesDirCod         BIGINT,
    UniProPesAudCod         BIGINT,
    UniProPesAutCod         BIGINT,
    UniDisCod               SMALLINT,
    CONSTRAINT pk_sau_uni PRIMARY KEY (UniCod),
    CONSTRAINT fk_sau_uni_dis FOREIGN KEY (UniDisCod) REFERENCES SAU_DIS (DisCod),
    CONSTRAINT fk_sau_uni_tipuni FOREIGN KEY (TipUniCod) REFERENCES SAU_TIPUNI (TipUniCod)
);
CREATE INDEX IF NOT EXISTS isau_uni_nom ON SAU_UNI (UniNom);

-- Setor (SAU_SETOR) — MINIMAL stub: only SetorCod PK used by SAU_UNISETOR.
CREATE TABLE IF NOT EXISTS SAU_SETOR (
    SetorCod  INTEGER NOT NULL,
    SetorNom  VARCHAR(50),
    CONSTRAINT pk_sau_setor PRIMARY KEY (SetorCod)
);

-- Unidade × Setor (SAU_UNISETOR) — MINIMAL stub: delete-guard for SAU_UNI.
CREATE TABLE IF NOT EXISTS SAU_UNISETOR (
    UniCod    INTEGER  NOT NULL,
    SetorCod  INTEGER  NOT NULL,
    CONSTRAINT pk_sau_unisetor PRIMARY KEY (UniCod, SetorCod),
    CONSTRAINT fk_sau_unisetor_uni FOREIGN KEY (UniCod) REFERENCES SAU_UNI (UniCod)
);

-- Receituário Controle Especial (SAU_RECESP) — MINIMAL stub: delete-guard for SAU_UNI.
-- Full table is Wave-6 / Portaria 344/98 (regulatory sign-off required before cutover).
CREATE TABLE IF NOT EXISTS SAU_RECESP (
    RecEspUniCod  INTEGER NOT NULL,
    RecEspCod     BIGINT  NOT NULL,
    CONSTRAINT pk_sau_recesp PRIMARY KEY (RecEspUniCod, RecEspCod),
    CONSTRAINT fk_sau_recesp_uni FOREIGN KEY (RecEspUniCod) REFERENCES SAU_UNI (UniCod)
);

-- Profissionais × Unidade link sub-table (SAU_PROESP1) — MINIMAL stub: delete-guard for SAU_UNI.
CREATE TABLE IF NOT EXISTS SAU_PROESP1 (
    ProEspUniCod  INTEGER NOT NULL,
    EspCod        INTEGER,
    ProPesCod     BIGINT,
    CONSTRAINT pk_sau_proesp1 PRIMARY KEY (ProEspUniCod),
    CONSTRAINT fk_sau_proesp1_uni FOREIGN KEY (ProEspUniCod) REFERENCES SAU_UNI (UniCod)
);

-- Parâmetros 5 (SAU_PAR5) — MINIMAL stub: delete-guards for SAU_UNI (ParSalUniCod, ParSolUniCod).
CREATE TABLE IF NOT EXISTS SAU_PAR5 (
    ParEmpCod     INTEGER NOT NULL,
    ParSalUniCod  INTEGER,
    ParSolUniCod  INTEGER,
    CONSTRAINT pk_sau_par5 PRIMARY KEY (ParEmpCod)
);

-- Parâmetros de Agendamento (SAU_PAR2) — MINIMAL stub: delete-guards for SAU_UNI.
CREATE TABLE IF NOT EXISTS SAU_PAR2 (
    ParEmpCod         INTEGER  NOT NULL,
    ParAgendUniCod    INTEGER,
    ParAgendTipAge    SMALLINT NOT NULL DEFAULT 0,
    ParAgendDesUniCod INTEGER,
    CONSTRAINT pk_sau_par2 PRIMARY KEY (ParEmpCod, ParAgendTipAge)
);

-- Usuário × Unidade link (SAU_USUUNI) — MINIMAL stub: delete-guard for SAU_UNI.
CREATE TABLE IF NOT EXISTS SAU_USUUNI (
    UsuCod     INTEGER NOT NULL,
    UniUsuCod  INTEGER,
    CONSTRAINT pk_sau_usuuni PRIMARY KEY (UsuCod)
);

-- Usuário (SAU_USU) — MINIMAL stub: delete-guard for SAU_UNI (UsuUniCod).
CREATE TABLE IF NOT EXISTS SAU_USU (
    UsuCod     INTEGER NOT NULL,
    UsuUniCod  INTEGER,
    CONSTRAINT pk_sau_usu PRIMARY KEY (UsuCod)
);

-- Remessa 1 (SAU_REM1) — MINIMAL stub: delete-guard for SAU_UNI (RemUniCod).
CREATE TABLE IF NOT EXISTS SAU_REM1 (
    RemCod     INTEGER NOT NULL,
    RemUniCod  INTEGER,
    CONSTRAINT pk_sau_rem1 PRIMARY KEY (RemCod)
);

-- Remessa × Unidade × Setor (SAU_REM_UNISETOR) — MINIMAL stub: delete-guard for SAU_UNI.
CREATE TABLE IF NOT EXISTS SAU_REM_UNISETOR (
    RemCod              INTEGER  NOT NULL,
    RemUniSetorSeq      SMALLINT NOT NULL,
    RemUniSetorUniCod   INTEGER,
    CONSTRAINT pk_sau_rem_unisetor PRIMARY KEY (RemCod, RemUniSetorSeq)
);

-- Paciente (SAU_PAC) — MINIMAL stub: delete-guards for SAU_UNI (3 UniCod columns)
-- + columns required by ProntuarioPaciente entity (SAU_PACPRN slice).
CREATE TABLE IF NOT EXISTS SAU_PAC (
    PacPesCod           BIGINT       NOT NULL,
    PacUniCod           INTEGER,
    PacPesCadAltUniCod  INTEGER,
    PacPesCadInsUniCod  INTEGER,
    PacPesNom           VARCHAR(50),           -- display copy from SYS_PES; SAU_PACPRN slice
    PacProNum           VARCHAR(10),           -- prontuário number; SAU_PACPRN slice
    CONSTRAINT pk_sau_pac PRIMARY KEY (PacPesCod)
);
