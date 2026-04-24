-- Базовая таблица пользователей (Steam), до расширений из V2

CREATE TABLE users (
    steam_id                  VARCHAR(32) PRIMARY KEY,
    persona_name              VARCHAR(256),
    private_profile           BOOLEAN NOT NULL DEFAULT FALSE,
    successful_trades_count   INT NOT NULL DEFAULT 0,
    last_online_at            TIMESTAMPTZ,
    steam_trade_link          VARCHAR(512),
    display_name              VARCHAR(128),
    steam_avatar_url          VARCHAR(512),
    avatar_source             VARCHAR(16) NOT NULL DEFAULT 'STEAM',
    selected_gallery_image_id UUID
);

CREATE UNIQUE INDEX uq_users_display_name ON users (display_name) WHERE display_name IS NOT NULL;
