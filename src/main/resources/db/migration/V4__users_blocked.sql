-- Блокировка аккаунтов (модерация / админ-панель).

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS blocked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS ix_users_blocked ON users (blocked) WHERE blocked = TRUE;
