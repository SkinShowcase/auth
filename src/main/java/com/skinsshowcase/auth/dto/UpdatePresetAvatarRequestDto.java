package com.skinsshowcase.auth.dto;

/**
 * PATCH /auth/me/avatar: {@code {"presetAvatarId":1}} (1–8). Источник отображаемого аватара — только пресет с сервера.
 */
public record UpdatePresetAvatarRequestDto(Integer presetAvatarId) {
}
