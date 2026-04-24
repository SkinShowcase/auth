-- Юридические документы, жалобы, галерея (PostgreSQL). Колонки профиля users — в V1.

CREATE TABLE IF NOT EXISTS legal_document (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(64) NOT NULL,
    version     INT NOT NULL,
    title       VARCHAR(256) NOT NULL,
    content     TEXT NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_legal_document_slug_version UNIQUE (slug, version)
);

CREATE INDEX IF NOT EXISTS ix_legal_document_slug_effective
    ON legal_document (slug, effective_from DESC);

CREATE TABLE IF NOT EXISTS user_report (
    id                  UUID PRIMARY KEY,
    reporter_steam_id   VARCHAR(32) NOT NULL,
    reported_steam_id   VARCHAR(32) NOT NULL,
    reason              VARCHAR(64) NOT NULL,
    details             VARCHAR(2000),
    created_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_user_report_reported_created
    ON user_report (reported_steam_id, created_at DESC);

CREATE TABLE IF NOT EXISTS user_gallery_image (
    id              UUID PRIMARY KEY,
    user_steam_id   VARCHAR(32) NOT NULL,
    image_url       VARCHAR(1024) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_user_gallery_user ON user_gallery_image (user_steam_id, created_at DESC);

INSERT INTO legal_document (slug, version, title, content, effective_from, created_at)
VALUES
    ('terms', 1, 'Пользовательское соглашение',
     'Текст пользовательского соглашения размещается здесь. Обновите содержимое при необходимости.',
     NOW(), NOW()),
    ('privacy', 1, 'Политика конфиденциальности',
     'Текст политики конфиденциальности размещается здесь. Обновите содержимое при необходимости.',
     NOW(), NOW())
ON CONFLICT (slug, version) DO NOTHING;
