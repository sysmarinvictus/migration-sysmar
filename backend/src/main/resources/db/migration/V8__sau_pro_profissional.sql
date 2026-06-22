-- V8: Profissional (SAU_PRO) slice support.
--
-- The full 16-column SAU_PRO is created in V1 (the stub was replaced in-place; safe because no
-- environment had applied the partial-4-column stub to a real DB yet — confirm before broad rollout).
-- This forward migration adds the columns the SAU_PRO native queries reference on the *minimal stub*
-- tables (SYS_PES / SAU_USU / SAU_RECESP) so Testcontainers can run the joins/guards. All ALTERs are
-- idempotent (ADD COLUMN IF NOT EXISTS) and additive — they do NOT touch production (ddl-auto=validate,
-- live DB already has these columns).

-- ── SYS_PES: person fields read by the SAU_PRO detail/list projection (R2 write-back targets) ──
-- Physical names/types match live saude-mandaguari (introspected 2026-06-22).
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesCPFCNPJ  CHAR(18);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesRGIE     VARCHAR(15);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesSex      CHAR(1);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesNasDat   DATE;
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesEnd      VARCHAR(70);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesEndNum   VARCHAR(10);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesEndCom   VARCHAR(40);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesCEP      CHAR(8);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesMunCod   INTEGER;
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesFon      VARCHAR(20);
ALTER TABLE SYS_PES ADD COLUMN IF NOT EXISTS PesCel      VARCHAR(20);

-- ── SAU_USU: link to professional (R21 delete-guard) ──
ALTER TABLE SAU_USU ADD COLUMN IF NOT EXISTS UsuProPesCod BIGINT;

-- ── SAU_RECESP: prescriber link (R26 delete-guard, Portaria 344/98) ──
ALTER TABLE SAU_RECESP ADD COLUMN IF NOT EXISTS RecEspProPesCod BIGINT;

-- ── SAU_PRO.ProCertificadoSenha: widen for encryption-at-rest ──
-- Legacy stored this as VARCHAR(50) PLAINTEXT (R31). The modern app encrypts it at rest with
-- AES-256-GCM (CertificadoSenhaCryptoConverter); the "v1:<base64 iv>:<base64 ct>" envelope is ~72
-- chars for a short password and up to ~108 for a 50-char one — it does NOT fit in VARCHAR(50).
-- Widen so the ciphertext fits; the entity @Column(length=255) keeps ddl-auto=validate happy.
-- Idempotent + additive; production must apply the same widening before storing encrypted values.
ALTER TABLE SAU_PRO ALTER COLUMN ProCertificadoSenha TYPE VARCHAR(255);

-- NOTE: R22/R23/R24 guard tables ("uni-nut-pro-pes", SISPRENATAL, HIPERDIA) are absent from the live
-- saude-mandaguari schema (TODO confirm physical names — OQ-DG). The repository guards those queries
-- with to_regclass(), so no stub is created here; they evaluate to "false" (not referenced) until the
-- real tables/names are confirmed.
