-- Публичный SteamID64 для API (поиск по display name и ответы DTO). PK users.steam_id остаётся хешем.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS steam_id64 VARCHAR(17) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_steam_id64
    ON users (steam_id64)
    WHERE steam_id64 IS NOT NULL;
