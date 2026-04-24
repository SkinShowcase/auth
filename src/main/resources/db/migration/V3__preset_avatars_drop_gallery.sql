-- Пресетные аватарки (1–8) вместо галереи по URL.

UPDATE users
SET avatar_source = 'STEAM',
    selected_gallery_image_id = NULL
WHERE UPPER(avatar_source) = 'GALLERY';

ALTER TABLE users ADD COLUMN selected_preset_avatar_id INT NULL;

ALTER TABLE users DROP COLUMN selected_gallery_image_id;

DROP TABLE IF EXISTS user_gallery_image;
