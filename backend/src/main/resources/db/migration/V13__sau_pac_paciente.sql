-- SAU_PAC (Paciente) — promote the baseline stub (PK + a few cols) to the full 26-column subtype.
-- The live saude-mandaguari DB already has these columns → every ADD is a no-op there; this builds the
-- local/Testcontainers schema so ddl-auto=validate passes. Types live-verified 2026-07-01. Idempotent.
-- Align PacProNum to the live type CHAR(10) (the baseline stub created it VARCHAR; live is char(10)).
-- PG skips the table rewrite when the type is already char(10) → no-op on the live DB.
ALTER TABLE SAU_PAC ALTER COLUMN PacProNum TYPE CHAR(10);

ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacIdNum              BIGINT;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacAler               VARCHAR(50);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacCHistDoe           VARCHAR(200);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacObi                SMALLINT;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacInconsciente       BOOLEAN;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacSituacaoRua        BOOLEAN;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacSurtoPsiquiatrico  BOOLEAN;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacRendaFamiliar      SMALLINT;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacMeioTransporte     VARCHAR(50);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacBeneficioSocial    BOOLEAN;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacCNH                VARCHAR(11);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacSit                SMALLINT;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacExpEsusErro        SMALLINT;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacUsuLogin           VARCHAR(20);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacUsuLoginIns        VARCHAR(20);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacUsuDatAlt          TIMESTAMP;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacPesUsuCod          INTEGER;
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacPesUsuLogin        VARCHAR(40);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacPesNomSoundex      VARCHAR(50);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacPesNomMaeSoundex   VARCHAR(50);
ALTER TABLE SAU_PAC ADD COLUMN IF NOT EXISTS PacPesNomSocSoundex   VARCHAR(50);

-- R14 delete-guard needs SAU_RECESP.PacPesCod (the baseline SAU_RECESP stub lacks it; live has it).
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS PacPesCod BIGINT;

-- R2 exemption needs SAU_UNI.UniCadCPF (flag: unidade allows CPF-less patient registration).
ALTER TABLE SAU_UNI ADD COLUMN IF NOT EXISTS UniCadCPF BOOLEAN;
