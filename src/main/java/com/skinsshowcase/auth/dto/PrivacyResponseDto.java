package com.skinsshowcase.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ответ GET /auth/me/privacy: текущая настройка приватности профиля.
 */
public record PrivacyResponseDto(@JsonProperty("private") boolean privateProfile) {
}
