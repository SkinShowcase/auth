package com.skinsshowcase.auth.dto;

/**
 * Элемент списка GET /auth/avatars — идентификатор пресета и абсолютный URL картинки.
 */
public record PresetAvatarOptionDto(int id, String url) {
}
