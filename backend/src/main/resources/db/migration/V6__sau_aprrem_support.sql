-- ============================================================================
-- V6: SAU_APRREM (Forma de Apresentação) support objects.
--
-- The table itself already exists: in the live saude-mandaguari DB and as the
-- idempotent stub in V5__sau_rem_support.sql
--   (SAU_APRREM(AprRemCod INTEGER PK, AprRemDes VARCHAR(30), AprRemAbr CHAR(5))).
-- The live shape matches the stub exactly, so NO CREATE is needed here. This
-- migration adds only the missing code sequence (live DB has none; OQ-4) and the
-- GeneXus secondary index, for parity. Idempotent throughout.
-- ============================================================================

-- AprRemCod sequence (R1/R15-style generation). Seeded from MAX so it never
-- collides with existing rows (table may be empty in some snapshots — OQ-5).
CREATE SEQUENCE IF NOT EXISTS seq_sau_aprrem_cod;
DO $$
BEGIN
    PERFORM setval('seq_sau_aprrem_cod',
                   COALESCE((SELECT MAX(AprRemCod) FROM SAU_APRREM), 0) + 1, false);
END $$;

-- GeneXus secondary index (present in the live DB) — parity for the Testcontainers schema.
CREATE INDEX IF NOT EXISTS usau_aprrem_desc ON SAU_APRREM (AprRemCod DESC);
