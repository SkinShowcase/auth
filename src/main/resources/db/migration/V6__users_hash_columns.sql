ALTER TABLE users
    ADD COLUMN IF NOT EXISTS steam_id_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS persona_name_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS steam_trade_link_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS display_name_hash VARCHAR(64);
