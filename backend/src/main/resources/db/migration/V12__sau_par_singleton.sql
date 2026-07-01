-- SAU_PAR (Parâmetros do sistema) — per-empresa SINGLETON config. The live saude-mandaguari DB already
-- has this table with 232 columns, so CREATE TABLE IF NOT EXISTS is a NO-OP there. This creates only the
-- FOCUSED mapped subset (prescription validity, user/security day-counts, secretariat header, policy
-- flags) so the local/Testcontainers schema satisfies ddl-auto=validate for the Parametro entity.
-- Types are live-verified. The ~215 unmapped config columns are intentionally omitted (see SLICE-SPEC).
CREATE TABLE IF NOT EXISTS SAU_PAR (
    ParEmpCod                    INTEGER NOT NULL,
    ParValidadeReceita           BOOLEAN,
    ParValidadeReceitaSimples    SMALLINT,
    ParValidadeReceitaUsoCon     SMALLINT,
    ParValidadeReceitaConEsp     SMALLINT,
    ParInaUsuDias                SMALLINT,
    ParSenUsuDias                SMALLINT,
    ParSecr                      VARCHAR(50),
    ParSecrEnd                   VARCHAR(50),
    ParSecrCep                   CHAR(8),
    ParSecrFone1                 CHAR(20),
    ParSecrFone2                 CHAR(20),
    ParSecrEmail                 VARCHAR(70),
    ParCadSemCns                 BOOLEAN,
    ParRecComprador              BOOLEAN,
    ParExigeCid10Atestado        BOOLEAN,
    ParEstornarAtendimento       BOOLEAN,
    ParImpRiscoMaterno           SMALLINT,
    ParAteHis                    SMALLINT,
    CONSTRAINT pk_sau_par PRIMARY KEY (ParEmpCod)
);
