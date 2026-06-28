-- V9: SAU_LOG (audit trail) baseline for Testcontainers / empty DBs.
--
-- PRODUCTION: SAU_LOG already exists (~7.7M rows) → this is never executed there (ddl-auto=validate,
-- no ALTER/DDL touches the live table). On an EMPTY database (integration tests) this CREATE makes
-- the table available so the modern AuditService write path and the admin viewer can be exercised.
--
-- Added as a FORWARD migration (not appended to V1) because V1 may already be checksum-locked in
-- deployed environments. All statements are idempotent (IF NOT EXISTS) and additive.
--
-- Composite PK = (logempcod, logdat, logusucod, logkey); logkey is CHAR(50) — keep it CHAR so the
-- entity's @JdbcTypeCode(Types.CHAR) PK match works (bpchar padding). Identifiers unquoted →
-- PostgreSQL folds to lowercase, matching the standard physical naming strategy.

CREATE TABLE IF NOT EXISTS SAU_LOG (
    logempcod           integer                     NOT NULL,
    logdat              timestamp without time zone NOT NULL,
    logusucod           integer                     NOT NULL,
    logope              varchar(3),
    logtab              varchar(31)                 NOT NULL,
    logkey              char(50)                    NOT NULL,
    usucod              integer,
    logsituacao         varchar(50),
    loghistorico        text,
    lognomeprofissional varchar(50),
    logpropescod        bigint,
    lognomepaciente     varchar(50),
    logpacpescod        bigint,
    logunicod           integer,
    CONSTRAINT sau_log_pkey PRIMARY KEY (logempcod, logdat, logusucod, logkey)
);

CREATE INDEX IF NOT EXISTS isau_log5 ON SAU_LOG (usucod);
