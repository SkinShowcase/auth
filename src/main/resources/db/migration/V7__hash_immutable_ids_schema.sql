ALTER TABLE users
    ALTER COLUMN steam_id TYPE VARCHAR(64);

ALTER TABLE user_report
    ALTER COLUMN reporter_steam_id TYPE VARCHAR(64),
    ALTER COLUMN reported_steam_id TYPE VARCHAR(64);
