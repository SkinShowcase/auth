package com.skinsshowcase.auth.dto;

import java.util.Map;

/**
 * Ответ POST /auth/internal/users/privacy-flags: true — профиль приватный.
 */
public record InternalPrivacyFlagsResponseDto(Map<String, Boolean> privateBySteamId) {
}
