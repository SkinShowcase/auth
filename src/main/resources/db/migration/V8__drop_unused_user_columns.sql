ALTER TABLE users
    DROP COLUMN IF EXISTS steam_id_hash,
    DROP COLUMN IF EXISTS persona_name,
    DROP COLUMN IF EXISTS persona_name_hash,
    DROP COLUMN IF EXISTS steam_trade_link_hash,
    DROP COLUMN IF EXISTS steam_avatar_url;
