-- Только пресетные аватарки: убираем хранение выбора STEAM, дефолт — пресет 1.

UPDATE users
SET selected_preset_avatar_id = 1
WHERE selected_preset_avatar_id IS NULL;

UPDATE users
SET avatar_source = 'PRESET'
WHERE UPPER(avatar_source) = 'STEAM';

ALTER TABLE users
    ALTER COLUMN avatar_source SET DEFAULT 'PRESET';
