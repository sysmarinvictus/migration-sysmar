-- V2: Extend SAU_UNISETOR stub with the 6 missing columns; extend SAU_REM_UNISETOR;
-- add minimal stubs for SAU_USUUNI1 and SAU_REMLOT (needed for SAU_UNISETOR delete guards).

-- SAU_USUUNI1 — Usuário × Unidade × Setor  (minimal stub — delete guard R13 for SAU_UNISETOR)
CREATE TABLE IF NOT EXISTS SAU_USUUNI1 (
    UsuCod      INTEGER NOT NULL,
    UniUsuCod   INTEGER,
    UsuSetorCod INTEGER,
    CONSTRAINT pk_sau_usuuni1 PRIMARY KEY (UsuCod)
);

-- SAU_REMLOT — Lote de Medicamento  (minimal stub — delete guard R14 for SAU_UNISETOR)
CREATE TABLE IF NOT EXISTS SAU_REMLOT (
    RemCod      INTEGER NOT NULL,
    RemLotNum   VARCHAR(20),
    RemUniCod   INTEGER,
    RemSetorCod INTEGER,
    CONSTRAINT pk_sau_remlot PRIMARY KEY (RemCod)
);

-- SAU_PAR5 — add ParSalSetorCod used by SAU_UNISETOR delete guard R12.
ALTER TABLE SAU_PAR5 ADD COLUMN IF NOT EXISTS ParSalSetorCod INTEGER;

-- SAU_UNISETOR — add the 6 columns missing from the Wave-1 stub.
-- 'IF NOT EXISTS' keeps this idempotent against a live DB that already has them.
ALTER TABLE SAU_UNISETOR
    ADD COLUMN IF NOT EXISTS SetorNom         VARCHAR(50)  NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS SetorEstocador   SMALLINT     NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS SetorSituacao    VARCHAR(40)  NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS SetorDataInativo TIMESTAMP             DEFAULT '1900-01-01 00:00:00',
    ADD COLUMN IF NOT EXISTS SetorHorIni      TIMESTAMP             DEFAULT '1900-01-01 00:00:00',
    ADD COLUMN IF NOT EXISTS SetorHorFim      TIMESTAMP             DEFAULT '1900-01-01 00:00:00';

-- SAU_REM_UNISETOR — add columns missing from the Wave-1 stub.
ALTER TABLE SAU_REM_UNISETOR
    ADD COLUMN IF NOT EXISTS RemUniSetorSetorCod INTEGER,
    ADD COLUMN IF NOT EXISTS RemUniSetorEstMin   INTEGER,
    ADD COLUMN IF NOT EXISTS RemUniSetorSit      SMALLINT;
