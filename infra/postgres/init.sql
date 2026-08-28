-- Changelog materializado pelo consumer (CONTRATOS.md §4, frente C).

CREATE SCHEMA IF NOT EXISTS changelog;

CREATE TABLE IF NOT EXISTS changelog.events (
    event_id            UUID PRIMARY KEY,
    schema_version      TEXT        NOT NULL DEFAULT '1.0',
    event_type          TEXT        NOT NULL,
    entity_key          JSONB       NOT NULL,
    competencia_de      TEXT        NOT NULL,
    competencia_para    TEXT        NOT NULL,
    campos_alterados    JSONB       NOT NULL DEFAULT '[]'::jsonb,
    before_state        JSONB,
    after_state         JSONB,
    detected_at         TIMESTAMPTZ NOT NULL,
    pipeline_estrategia TEXT,
    pipeline_run_id     TEXT,
    consumed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_changelog_events_type
    ON changelog.events (event_type);

CREATE INDEX IF NOT EXISTS idx_changelog_events_competencias
    ON changelog.events (competencia_de, competencia_para);
