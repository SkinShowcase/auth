package com.skinsshowcase.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Ответ GET /auth/me: профиль аутентифицированного пользователя.
 * Поле effectiveAvatarUrl — всегда URL пресета (1–8); avatarSource в БД — PRESET.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MeResponseDto(
        String steamId,
        String displayName,
        boolean privateProfile,
        int successfulTradesCount,
        Instant lastOnlineAt,
        String steamTradeLink,
        String effectiveAvatarUrl,
        Integer selectedPresetAvatarId,
        String avatarSource,
        boolean blocked
) {
}
