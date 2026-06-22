-- ============================================================================
-- V7: Align SAU_ESP column types to the live saude-mandaguari schema.
--
-- V1 declared EspAux BOOLEAN, EspSit VARCHAR(1), and the 16 EspLstAgend* columns
-- INTEGER — but the live DB has EspAux INTEGER, EspSit SMALLINT, and EspLstAgend*
-- SMALLINT (confirmed 2026-06-22 during /verify-parity SAU_ESP). The entity is
-- being corrected to match; this aligns the Flyway-built (Testcontainers) schema.
--
-- Idempotent: each ALTER runs only when the column is still the V1 type, so it is
-- a no-op against the already-correct live DB.
-- ============================================================================
DO $$
DECLARE
    col TEXT;
    agenda TEXT[] := ARRAY[
        'esplstagendestagnadomuitourg','esplstagendestagnadonormal','esplstagendestagnadopri',
        'esplstagendestagnadourg','esplstagendtempomaxmuitourg','esplstagendtempomaxnormal',
        'esplstagendtempomaxpri','esplstagendtempomaxurg','esplstagendvagamuitourgmax',
        'esplstagendvagamuitourgmin','esplstagendvaganormax','esplstagendvaganormin',
        'esplstagendvagaprimax','esplstagendvagaprimin','esplstagendvagaurgmax','esplstagendvagaurgmin'];
BEGIN
    IF (SELECT data_type FROM information_schema.columns
        WHERE table_name='sau_esp' AND column_name='espaux') = 'boolean' THEN
        ALTER TABLE SAU_ESP ALTER COLUMN EspAux TYPE INTEGER USING (CASE WHEN EspAux THEN 1 ELSE 0 END);
    END IF;

    IF (SELECT data_type FROM information_schema.columns
        WHERE table_name='sau_esp' AND column_name='espsit') = 'character varying' THEN
        ALTER TABLE SAU_ESP ALTER COLUMN EspSit TYPE SMALLINT USING (NULLIF(btrim(EspSit),'')::smallint);
    END IF;

    FOREACH col IN ARRAY agenda LOOP
        IF (SELECT data_type FROM information_schema.columns
            WHERE table_name='sau_esp' AND column_name=col) = 'integer' THEN
            EXECUTE format('ALTER TABLE SAU_ESP ALTER COLUMN %I TYPE SMALLINT USING (%I::smallint)', col, col);
        END IF;
    END LOOP;
END $$;
